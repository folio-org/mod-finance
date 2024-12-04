package org.folio.services.financedata;

import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.ERROR;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FINANCE_DATA_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.JobDetails;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.fund.FundUpdateLogService;
import org.folio.services.protection.AcqUnitsService;
import org.folio.services.transactions.TransactionService;

public class FinanceDataService {
  private static final Logger log = LogManager.getLogger();

  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;
  private final TransactionService transactionService;
  private final FiscalYearService fiscalYearService;
  private final FundUpdateLogService fundUpdateLogService;

  public FinanceDataService(RestClient restClient, AcqUnitsService acqUnitsService,
                            TransactionService transactionService, FiscalYearService fiscalYearService, FundUpdateLogService fundUpdateLogService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
    this.transactionService = transactionService;
    this.fiscalYearService = fiscalYearService;
    this.fundUpdateLogService = fundUpdateLogService;
  }

  public Future<FyFinanceDataCollection> getFinanceDataWithAcqUnitsRestriction(String query, int offset, int limit,
                                                                               RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .compose(effectiveQuery -> getFinanceData(effectiveQuery, offset, limit, requestContext));
  }

  private Future<FyFinanceDataCollection> getFinanceData(String query, int offset, int limit,
                                                         RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FINANCE_DATA_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FyFinanceDataCollection.class, requestContext);
  }

  public Future<Void> putFinanceData(FyFinanceDataCollection financeData, RequestContext requestContext) {
    financeData.getFyFinanceData().forEach(this::validateFinanceData);

    return processAllocationTransaction(financeData, requestContext)
      .compose(v -> updateFinanceData(financeData, requestContext))
      .onSuccess(asyncResult -> processLogs(financeData, requestContext, COMPLETED))
      .onFailure(asyncResult -> processLogs(financeData, requestContext, ERROR));
  }

  private void validateFinanceData(FyFinanceData financeData) {
    validateFinanceDataFields(financeData);

    var allocationChange = BigDecimal.valueOf(financeData.getBudgetAllocationChange());
    var initialAllocation = BigDecimal.valueOf(financeData.getBudgetInitialAllocation());
//    var currentAllocation = BigDecimal.valueOf(financeData.getBudgetCurrentAllocation());
//    var expectedChange = currentAllocation.subtract(initialAllocation);
//
//    if (allocationChange.abs().subtract(expectedChange).doubleValue() != 0) {
//      throw new IllegalArgumentException(
//        "Allocation change does not match the difference between current and initial allocation");
//    }
    if (allocationChange.doubleValue() < 0 && allocationChange.abs().doubleValue() > initialAllocation.doubleValue()) {
      throw new IllegalArgumentException("Allocation change cannot be greater than initial allocation");
    }
  }

  private void validateFinanceDataFields(FyFinanceData financeData) {
    if (financeData.getFundId() == null) {
      throw new IllegalArgumentException("Fund ID is required");
    }
    if (financeData.getFundCode() == null) {
      throw new IllegalArgumentException("Fund code is required");
    }
    if (financeData.getFundName() == null) {
      throw new IllegalArgumentException("Fund name is required");
    }
    if (financeData.getFundDescription() == null) {
      throw new IllegalArgumentException("Fund description is required");
    }
    if (financeData.getFundStatus() == null) {
      throw new IllegalArgumentException("Fund status is required");
    }
    if (financeData.getBudgetId() == null) {
      throw new IllegalArgumentException("Budget ID is required");
    }
    if (financeData.getBudgetName() == null) {
      throw new IllegalArgumentException("Budget name is required");
    }
    if (financeData.getBudgetStatus() == null) {
      throw new IllegalArgumentException("Budget status is required");
    }
    if (financeData.getBudgetInitialAllocation() == null) {
      throw new IllegalArgumentException("Budget initial allocation is required");
    }
    if (financeData.getBudgetAllocationChange() == null) {
      throw new IllegalArgumentException("Allocation change is required");
    }
    if (financeData.getBudgetAllowableExpenditure() == null) {
      throw new IllegalArgumentException("Budget allowable expenditure is required");
    }
    if (financeData.getBudgetAllowableEncumbrance() == null) {
      throw new IllegalArgumentException("Budget allowable encumbrance is required");
    }
    if (financeData.getTransactionDescription() == null) {
      throw new IllegalArgumentException("Transaction description is required");
    }
    if (financeData.getTransactionTag() == null) {
      throw new IllegalArgumentException("Transaction tag is required");
    }
  }

  public Future<Void> processAllocationTransaction(FyFinanceDataCollection fyFinanceDataCollection,
                                                   RequestContext requestContext) {
    var transactionsFuture = fyFinanceDataCollection.getFyFinanceData().stream()
      .map(financeData -> createAllocationTransaction(financeData, requestContext))
      .toList();

    return GenericCompositeFuture.join(transactionsFuture)
      .compose(compositeFuture -> {
        List<Transaction> transactions = compositeFuture.list();
        return createBatchTransaction(transactions, requestContext);
      });
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
    var allocationChange = BigDecimal.valueOf(financeData.getBudgetAllocationChange());
    return initialAllocation.add(allocationChange).doubleValue();
  }

  public Future<Void> createBatchTransaction(List<Transaction> transactions, RequestContext requestContext) {
    Batch batch = new Batch().withTransactionsToUpdate(transactions);
    return transactionService.processBatch(batch, requestContext);
  }

  private Future<Void> updateFinanceData(FyFinanceDataCollection financeDataCollection,
                                         RequestContext requestContext) {
    log.debug("updateFinanceData:: Trying to update finance data collection with size: {}", financeDataCollection.getTotalRecords());
    return restClient.put(resourcesPath(FINANCE_DATA_STORAGE), financeDataCollection, requestContext);
  }

  private Future<Void> updateFinanceData(FyFinanceDataCollection financeDataCollection,
                                         RequestContext requestContext, AsyncResult<Void> asyncResult) {
    log.debug("updateFinanceData:: Trying to update finance data collection with size: {}", financeDataCollection.getTotalRecords());
    if (asyncResult.succeeded()) {
      log.info("");
      return restClient.put(resourcesPath(FINANCE_DATA_STORAGE), financeDataCollection, requestContext);
    } else {
      return Future.failedFuture(asyncResult.cause());
    }
  }

  private void processLogs(FyFinanceDataCollection financeDataCollection,
                           RequestContext requestContext, FundUpdateLog.Status status) {
    var jobDetails = new JobDetails().withAdditionalProperty("fyFinanceData", financeDataCollection.getFyFinanceData());

    var fundUpdateLog = new FundUpdateLog().withId(UUID.randomUUID().toString())
      .withJobName("Update finance data")
      .withStatus(status)
      .withRecordsCount(financeDataCollection.getTotalRecords())
      .withJobDetails(jobDetails)
      .withJobNumber(1);
    fundUpdateLogService.createFundUpdateLog(fundUpdateLog, requestContext);
  }
}

