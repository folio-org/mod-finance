package org.folio.services.financedata;

import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FINANCE_DATA_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Future;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.protection.AcqUnitsService;
import org.folio.services.transactions.TransactionService;

public class FinanceDataService {
  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;
  private final TransactionService transactionService;
  private final FiscalYearService fiscalYearService;

  public FinanceDataService(RestClient restClient, AcqUnitsService acqUnitsService,
                            TransactionService transactionService, FiscalYearService fiscalYearService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
    this.transactionService = transactionService;
    this.fiscalYearService = fiscalYearService;
  }

  public Future<FyFinanceDataCollection> getFinanceDataWithAcqUnitsRestriction(String query, int offset, int limit,
                                                                               RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .compose(effectiveQuery -> getFinanceData(effectiveQuery, offset, limit, requestContext));
  }

  private Future<FyFinanceDataCollection> getFinanceData(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FINANCE_DATA_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FyFinanceDataCollection.class, requestContext);
  }

  public Future<Void> putFinanceData(FyFinanceDataCollection financeData, RequestContext requestContext) {
    validateFinanceData(financeData);

    return processAllocationTransaction(financeData, requestContext)
      .compose(v -> updateFinanceData(financeData, requestContext))
      .compose(v -> processLogs(financeData, requestContext));
  }

  private void validateFinanceData(FyFinanceDataCollection financeData) {
    if (financeData.getFyFinanceData() == null || financeData.getFyFinanceData().isEmpty()) {
      throw new IllegalArgumentException("Finance data collection is empty");
    }

    financeData.getFyFinanceData().forEach(this::validateFinanceDataItem);
  }

  private void validateFinanceDataItem(FyFinanceData item) {
    // Validate fiscal year code format (assuming it should be "FY" followed by 4 digits)
    if (item.getFiscalYearCode() != null && !item.getFiscalYearCode().matches("FY\\d{4}")) {
      throw new IllegalArgumentException("Invalid fiscal year code format. Expected FY followed by 4 digits.");
    }

    validateNumericFields(item);

    // Validate that allocation change matches the difference between current and initial allocation
    if (item.getAllocationChange() != null && item.getBudgetCurrentAllocation() != null
      && item.getBudgetInitialAllocation() != null) {
      double expectedChange = item.getBudgetCurrentAllocation() - item.getBudgetInitialAllocation();
      if (Math.abs(item.getAllocationChange() - expectedChange) > 0.01) { // allowing for small floating-point discrepancies
        throw new IllegalArgumentException("Allocation change does not match the difference between current and initial allocation");
      }
    }
  }

  private void validateNumericFields(FyFinanceData item) {
    if (item.getBudgetInitialAllocation() != null && item.getBudgetInitialAllocation() < 0) {
      throw new IllegalArgumentException("Budget initial allocation must be non-negative");
    }
    if (item.getBudgetCurrentAllocation() != null && item.getBudgetCurrentAllocation() < 0) {
      throw new IllegalArgumentException("Budget current allocation must be non-negative");
    }
    if (item.getBudgetAllowableExpenditure() != null
      && (item.getBudgetAllowableExpenditure() < 0 || item.getBudgetAllowableExpenditure() > 100)) {
      throw new IllegalArgumentException("Budget allowable expenditure must be between 0 and 100");
    }
    if (item.getBudgetAllowableEncumbrance() != null
      && (item.getBudgetAllowableEncumbrance() < 0 || item.getBudgetAllowableEncumbrance() > 100)) {
      throw new IllegalArgumentException("Budget allowable encumbrance must be between 0 and 100");
    }
  }

  private Future<Void> processAllocationTransaction(FyFinanceDataCollection financeData, RequestContext requestContext) {
    return createAllocationTransaction(financeData, requestContext);
  }

  public Future<Void> createAllocationTransaction(FyFinanceDataCollection fyFinanceDataCollection, RequestContext requestContext) {
    var transactionsFuture = fyFinanceDataCollection.getFyFinanceData().stream()
      .map(financeData -> createAllocationTransaction(financeData, requestContext))
      .toList();

    return GenericCompositeFuture.join(transactionsFuture)
      .map(compositeFuture -> {
        List<Transaction> transactions = compositeFuture.list();
        return createBatchTransaction(transactions, requestContext);
      })
      .mapEmpty();
  }

  public Future<Transaction> createAllocationTransaction(FyFinanceData financeData, RequestContext requestContext) {
    var transaction = new Transaction()
      .withTransactionType(Transaction.TransactionType.ALLOCATION)
      .withId(UUID.randomUUID().toString())
      .withAmount(calculateAllocation(financeData))
      .withFiscalYearId(financeData.getFiscalYearId())
      .withToFundId(financeData.getFundId())
      .withSource(Transaction.Source.USER);

    return fiscalYearService.getFiscalYearById(financeData.getFiscalYearId(), requestContext)
      .map(fiscalYear -> transaction.withCurrency(fiscalYear.getCurrency()));
  }

  private Double calculateAllocation(FyFinanceData financeData) {
    var initialAllocation = BigDecimal.valueOf(financeData.getBudgetInitialAllocation());
    var allocationChange = BigDecimal.valueOf(financeData.getAllocationChange());
    return initialAllocation.add(allocationChange).doubleValue();
  }

  public Future<Void> createBatchTransaction(List<Transaction> transactions, RequestContext requestContext) {
    Batch batch = new Batch().withTransactionsToUpdate(transactions);
    return transactionService.processBatch(batch, requestContext);
  }

  private Future<Void> updateFinanceData(FyFinanceDataCollection financeData, RequestContext requestContext) {
    return restClient.put(resourcesPath(FINANCE_DATA_STORAGE), financeData, requestContext)
      .mapEmpty();
  }

  private Future<Void> processLogs(FyFinanceDataCollection financeData, RequestContext requestContext) {
    return Future.succeededFuture();
  }
}

