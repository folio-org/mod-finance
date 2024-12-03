package org.folio.services.financedata;

import static java.util.Collections.singletonList;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FINANCE_DATA_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Future;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Budget;
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
    // 1. Validate
    validateFinanceData(financeData);

    // 2. Apply calculation with allocating new value(allocation transaction should be created):
    calculateAllocation();

    // 3. Send request to update finance data
    updateFinanceData(financeData, requestContext);

    // 4. Invoke Bulk Transactions API to create allocation transactions
    processTransaction(financeData, requestContext);

    // 5. Invoke storage actions logs endpoint to save request payload + status metadata + recordsCount
    processLogs(financeData, requestContext);
    return null;
  }

  private void validateFinanceData(FyFinanceDataCollection financeData) {

  }

  public Future<Void> createAllocationTransaction(FyFinanceDataCollection fyFinanceDataCollection, RequestContext requestContext) {
    var transactions = fyFinanceDataCollection.getFyFinanceData().stream()
      .map(financeData -> createAllocationTransaction(financeData, requestContext))
      .toList();

    return fiscalYearService.getFiscalYearById(financeData.getFiscalYearId(), requestContext)
      .compose(fiscalYear -> transactionService.createTransaction(transaction.withCurrency(fiscalYear.getCurrency()), requestContext));
  }

  public Future<Transaction> createAllocationTransaction(FyFinanceData financeData, RequestContext requestContext) {
    Transaction transaction = new Transaction()
      .withTransactionType(Transaction.TransactionType.ALLOCATION)
      .withId(UUID.randomUUID().toString())
      .withAmount(financeData.getAllocationChange())
      .withFiscalYearId(financeData.getFiscalYearId())
      .withToFundId(financeData.getFundId())
      .withSource(Transaction.Source.USER);
    return fiscalYearService.getFiscalYearById(financeData.getFiscalYearId(), requestContext)
      .compose(fiscalYear -> transactionService.createTransaction(transaction.withCurrency(fiscalYear.getCurrency()), requestContext));
  }

  private void calculateAllocation() {

  }

  private void updateFinanceData(FyFinanceDataCollection financeData, RequestContext requestContext) {
    // send request to update finance data
  }

  private void processTransaction(FyFinanceDataCollection financeData, RequestContext requestContext) {
    // invoke Bulk Transactions API to create allocation transactions
  }

  private void processLogs(FyFinanceDataCollection financeData, RequestContext requestContext) {
    // invoke storage actions logs endpoint to save request payload + status metadata + recordsCount
  }
}

