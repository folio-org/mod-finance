package org.folio.services.financedata;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.ERROR;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FINANCE_DATA_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.JobDetails;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.fund.FundUpdateLogService;
import org.folio.services.protection.AcqUnitsService;
import org.folio.services.transactions.TransactionApiService;

@Log4j2
public class FinanceDataService {

  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;
  private final TransactionApiService transactionApiService;
  private final FiscalYearService fiscalYearService;
  private final FundUpdateLogService fundUpdateLogService;

  public FinanceDataService(RestClient restClient, AcqUnitsService acqUnitsService, TransactionApiService transactionApiService,
                            FiscalYearService fiscalYearService, FundUpdateLogService fundUpdateLogService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
    this.transactionApiService = transactionApiService;
    this.fiscalYearService = fiscalYearService;
    this.fundUpdateLogService = fundUpdateLogService;
  }

  /**
   * The method will fetch finance data with acq units restriction
   * 1. First fetch acq units for the user
   * 2. Build cql clause for finance data with acq units
   *
   * @param query           query to filter finance data
   * @param offset          offset
   * @param limit           limit
   * @param requestContext  request context
   * @return future with finance data collection
   */
  public Future<FyFinanceDataCollection> getFinanceDataWithAcqUnitsRestriction(String query, int offset, int limit,
                                                                               RequestContext requestContext) {
    log.debug("Trying to get finance data with acq units restriction, query={}", query);
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

  /**
   * The method will update finance data collection in one operation.
   * 1. Validate finance data collection, if it fails validation exception will be thrown
   * 2. Create allocation transactions, if it fails process will stop
   * 3. Update finance data by invoking storage API
   * 4. Save logs of the operation with COMPLETE or ERROR status
   *
   * @param financeDataCollection finance data collection to update
   * @param requestContext        request context
   * @return future with void result
   */
  public Future<FyFinanceDataCollection> putFinanceData(FyFinanceDataCollection financeDataCollection, RequestContext requestContext) {
    log.debug("Trying to update finance data collection with size: {}", financeDataCollection.getTotalRecords());
    if (CollectionUtils.isEmpty(financeDataCollection.getFyFinanceData())) {
      log.info("putFinanceData:: Finance data collection is empty, nothing to update");
      return succeededFuture(financeDataCollection);
    }

    validateFinanceDataCollection(financeDataCollection, getFiscalYearId(financeDataCollection));
    calculateAfterAllocation(financeDataCollection);
    if (financeDataCollection.getUpdateType().equals(FyFinanceDataCollection.UpdateType.PREVIEW)) {
      log.info("putFinanceData:: Running dry-run mode finance data collection");
      return succeededFuture(financeDataCollection);
    }

    return processAllocationTransaction(financeDataCollection, requestContext)
      .compose(v -> updateFinanceData(financeDataCollection, requestContext))
      .map(v -> financeDataCollection)
      .onSuccess(asyncResult -> processLogs(financeDataCollection, requestContext, COMPLETED))
      .onFailure(asyncResult -> processLogs(financeDataCollection, requestContext, ERROR));
  }

  private void validateFinanceDataCollection(FyFinanceDataCollection financeDataCollection, String fiscalYearId) {
    for (int i = 0; i < financeDataCollection.getFyFinanceData().size(); i++) {
      var financeData = financeDataCollection.getFyFinanceData().get(i);
      validateFinanceDataFields(financeData, i, fiscalYearId);

      var allocationChange = financeData.getBudgetAllocationChange();
      var currentAllocation = financeData.getBudgetCurrentAllocation();

      if (allocationChange < 0 && Math.abs(allocationChange) > currentAllocation) {
        var error = createError("Allocation change cannot be greater than current allocation",
          String.format("financeData[%s].budgetAllocationChange", i), String.valueOf(financeData.getBudgetAllocationChange()));
        throw new HttpException(422, new Errors().withErrors(List.of(error)));
      }
    }
  }

  private void calculateAfterAllocation(FyFinanceDataCollection financeDataCollection) {
    financeDataCollection.getFyFinanceData().forEach(financeData -> {
      var allocationChange = BigDecimal.valueOf(financeData.getBudgetAllocationChange());
      var currentAllocation = BigDecimal.valueOf(financeData.getBudgetCurrentAllocation());
      var afterAllocation = currentAllocation.add(allocationChange);
      financeData.setBudgetAfterAllocation(afterAllocation.doubleValue());
    });
  }

  private Future<Void> processAllocationTransaction(FyFinanceDataCollection fyFinanceDataCollection,
                                                    RequestContext requestContext) {
    return fiscalYearService.getFiscalYearById(getFiscalYearId(fyFinanceDataCollection), requestContext)
      .map(fiscalYear -> createAllocationTransactions(fyFinanceDataCollection, fiscalYear.getCurrency()))
      .compose(transactions -> createBatchTransaction(transactions, requestContext));
  }

  private String getFiscalYearId(FyFinanceDataCollection fyFinanceDataCollection) {
    return fyFinanceDataCollection.getFyFinanceData().get(0).getFiscalYearId();
  }

  private List<Transaction> createAllocationTransactions(FyFinanceDataCollection financeDataCollection, String currency) {
    return financeDataCollection.getFyFinanceData().stream()
      .map(financeData -> createAllocationTransaction(financeData, currency))
      .toList();
  }

  private Transaction createAllocationTransaction(FyFinanceData financeData, String currency) {
    var allocationChange = financeData.getBudgetAllocationChange();
    log.info("createAllocationTransaction:: Creating allocation transaction for fund '{}' and budget '{}' with allocation '{}'",
      financeData.getFundId(), financeData.getBudgetId(), allocationChange);

    var transaction = new Transaction()
      .withTransactionType(Transaction.TransactionType.ALLOCATION)
      .withId(UUID.randomUUID().toString())
      .withAmount(Math.abs(allocationChange))
      .withFiscalYearId(financeData.getFiscalYearId())
      .withSource(Transaction.Source.USER)
      .withCurrency(currency);

    // For negative allocation (decrease), use fromFundId
    // For positive allocation (increase), use toFundId
    if (allocationChange > 0) {
      transaction.withToFundId(financeData.getFundId());
    } else {
      transaction.withFromFundId(financeData.getFundId());
    }

    return transaction;
  }

  public Future<Void> createBatchTransaction(List<Transaction> transactions, RequestContext requestContext) {
    Batch batch = new Batch().withTransactionsToCreate(transactions);
    return transactionApiService.processBatch(batch, requestContext);
  }

  private Future<Void> updateFinanceData(FyFinanceDataCollection financeDataCollection,
                                         RequestContext requestContext) {
    log.debug("updateFinanceData:: Trying to update finance data collection with size: {}", financeDataCollection.getTotalRecords());
    return restClient.put(resourcesPath(FINANCE_DATA_STORAGE), financeDataCollection, requestContext);
  }

  private void processLogs(FyFinanceDataCollection financeDataCollection,
                           RequestContext requestContext, FundUpdateLog.Status status) {
    var jobDetails = new JobDetails().withAdditionalProperty("fyFinanceData", financeDataCollection.getFyFinanceData());
    var fundUpdateLog = new FundUpdateLog().withId(UUID.randomUUID().toString())
      .withJobName("Update finance data") // TODO: Update job name generation
      .withStatus(status)
      .withRecordsCount(financeDataCollection.getTotalRecords())
      .withJobDetails(jobDetails)
      .withJobNumber(1);
    fundUpdateLogService.createFundUpdateLog(fundUpdateLog, requestContext);
  }

  private void validateFinanceDataFields(FyFinanceData financeData, int i, String fiscalYearId) {
    var errors = new Errors().withErrors(new ArrayList<>());

    if (!financeData.getFiscalYearId().equals(fiscalYearId)) {
      errors.getErrors().add(createError(
        String.format("Fiscal year ID must be the same as other fiscal year ID(s) '[%s]' in the request", fiscalYearId),
        String.format("financeData[%s].fiscalYearId", i), financeData.getFiscalYearId())
      );
    }

    validateField(errors, String.format("financeData[%s].fundCode", i), financeData.getFundCode(), "Fund code is required");
    validateField(errors, String.format("financeData[%s].fundName", i), financeData.getFundName(), "Fund name is required");
    validateField(errors, String.format("financeData[%s].fundDescription", i), financeData.getFundDescription(), "Fund description is required");
    validateField(errors, String.format("financeData[%s].fundStatus", i), financeData.getFundStatus(), "Fund status is required");
    if (financeData.getBudgetId() != null) {
      validateField(errors, String.format("financeData[%s].budgetName", i), financeData.getBudgetName(), "Budget name is required");
      validateField(errors, String.format("financeData[%s].budgetStatus", i), financeData.getBudgetStatus(), "Budget status is required");
      validateField(errors, String.format("financeData[%s].budgetInitialAllocation", i), financeData.getBudgetInitialAllocation(), "Budget initial allocation is required");
      validateField(errors, String.format("financeData[%s].budgetCurrentAllocation", i), financeData.getBudgetCurrentAllocation(), "Budget current allocation is required");
      validateField(errors, String.format("financeData[%s].budgetAllocationChange", i), financeData.getBudgetAllocationChange(), "Allocation change is required");
      validateField(errors, String.format("financeData[%s].budgetAllowableExpenditure", i), financeData.getBudgetAllowableExpenditure(), "Budget allowable expenditure is required");
      validateField(errors, String.format("financeData[%s].budgetAllowableEncumbrance", i), financeData.getBudgetAllowableEncumbrance(), "Budget allowable encumbrance is required");
    }
    validateField(errors, String.format("financeData[%s].transactionDescription", i), financeData.getTransactionDescription(), "Transaction description is required");
    validateField(errors, String.format("financeData[%s].transactionTag", i), financeData.getTransactionTag(), "Transaction tag is required");

    if (CollectionUtils.isNotEmpty(errors.getErrors())) {
      throw new HttpException(422, errors);
    }
  }

  private void validateField(Errors errors, String fieldName, Object fieldValue, String errorMessage) {
    if (fieldValue == null) {
      errors.getErrors().add(createError(errorMessage, fieldName, "null"));
    }
  }

  private Error createError(String message, String key, String value) {
    log.warn("Validation error: {}", message);
    var param = new Parameter().withKey(key).withValue(value);
    return new Error().withMessage(message).withParameters(List.of(param));
  }
}

