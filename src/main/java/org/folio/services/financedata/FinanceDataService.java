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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.GenericCompositeFuture;
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

public class FinanceDataService {
  private static final Logger log = LogManager.getLogger();

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

  public Future<Void> putFinanceData(FyFinanceDataCollection financeDataCollection, RequestContext requestContext) {
    log.debug("Trying to update finance data collection with size: {}", financeDataCollection.getTotalRecords());
    if (CollectionUtils.isEmpty(financeDataCollection.getFyFinanceData())) {
      log.info("Finance data collection is empty, nothing to update");
      return succeededFuture();
    }

    validateFinanceDataCollection(financeDataCollection);
    return processAllocationTransaction(financeDataCollection, requestContext)
      .compose(v -> updateFinanceData(financeDataCollection, requestContext))
      .onSuccess(asyncResult -> processLogs(financeDataCollection, requestContext, COMPLETED))
      .onFailure(asyncResult -> processLogs(financeDataCollection, requestContext, ERROR));
  }

  private void validateFinanceDataCollection(FyFinanceDataCollection financeDataCollection) {
    for (int i = 0; i < financeDataCollection.getFyFinanceData().size(); i++) {
      var financeData = financeDataCollection.getFyFinanceData().get(i);
      validateFinanceDataFields(financeData, i);
      var allocationChange = BigDecimal.valueOf(financeData.getBudgetAllocationChange());
      var initialAllocation = BigDecimal.valueOf(financeData.getBudgetInitialAllocation());

      if (allocationChange.doubleValue() < 0 && allocationChange.abs().doubleValue() > initialAllocation.doubleValue()) {
        var params = List.of(new Parameter().withKey(String.format("financeData[%s].budgetAllocationChange", i))
          .withValue(String.valueOf(financeData.getBudgetAllocationChange())));
        var error = new Error().withMessage("Allocation change cannot be greater than initial allocation").withParameters(params);
        throw new HttpException(422, new Errors().withErrors(List.of(error)));
      }
    }
  }

  private Future<Void> processAllocationTransaction(FyFinanceDataCollection fyFinanceDataCollection,
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

  private Future<Transaction> createAllocationTransaction(FyFinanceData financeData, RequestContext requestContext) {
    var allocation = calculateAllocation(financeData);
    var transaction = new Transaction()
      .withTransactionType(Transaction.TransactionType.ALLOCATION)
      .withId(UUID.randomUUID().toString())
      .withAmount(allocation)
      .withFiscalYearId(financeData.getFiscalYearId())
      .withToFundId(financeData.getFundId())
      .withSource(Transaction.Source.USER);
    log.info("Creating allocation transaction for fund '{}' and budget '{}' with allocation '{}'",
      financeData.getFundId(), financeData.getBudgetId(), allocation);

    return fiscalYearService.getFiscalYearById(financeData.getFiscalYearId(), requestContext)
      .map(fiscalYear -> transaction.withCurrency(fiscalYear.getCurrency()));
  }

  private Double calculateAllocation(FyFinanceData financeData) {
    var initialAllocation = BigDecimal.valueOf(financeData.getBudgetInitialAllocation());
    var allocationChange = BigDecimal.valueOf(financeData.getBudgetAllocationChange());
    return initialAllocation.add(allocationChange).doubleValue();
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
      .withJobName("Update finance data")
      .withStatus(status)
      .withRecordsCount(financeDataCollection.getTotalRecords())
      .withJobDetails(jobDetails)
      .withJobNumber(1);
    fundUpdateLogService.createFundUpdateLog(fundUpdateLog, requestContext);
  }

  private void validateFinanceDataFields(FyFinanceData financeData, int i) {
    var errors = new Errors().withErrors(new ArrayList<>());

    validateField(errors, String.format("financeData[%s].fundCode", i), financeData.getFundCode(), "Fund code is required");
    validateField(errors, String.format("financeData[%s].fundName", i), financeData.getFundName(), "Fund name is required");
    validateField(errors, String.format("financeData[%s].fundDescription", i), financeData.getFundDescription(), "Fund description is required");
    validateField(errors, String.format("financeData[%s].fundStatus", i), financeData.getFundStatus(), "Fund status is required");
    validateField(errors, String.format("financeData[%s].budgetId", i), financeData.getBudgetId(), "Budget ID is required");
    validateField(errors, String.format("financeData[%s].budgetName", i), financeData.getBudgetName(), "Budget name is required");
    validateField(errors, String.format("financeData[%s].budgetStatus", i), financeData.getBudgetStatus(), "Budget status is required");
    validateField(errors, String.format("financeData[%s].budgetInitialAllocation", i), financeData.getBudgetInitialAllocation(), "Budget initial allocation is required");
    validateField(errors, String.format("financeData[%s].budgetAllocationChange", i), financeData.getBudgetAllocationChange(), "Allocation change is required");
    validateField(errors, String.format("financeData[%s].budgetAllowableExpenditure", i), financeData.getBudgetAllowableExpenditure(), "Budget allowable expenditure is required");
    validateField(errors, String.format("financeData[%s].budgetAllowableEncumbrance", i), financeData.getBudgetAllowableEncumbrance(), "Budget allowable encumbrance is required");
    validateField(errors, String.format("financeData[%s].transactionDescription", i), financeData.getTransactionDescription(), "Transaction description is required");
    validateField(errors, String.format("financeData[%s].transactionTag", i), financeData.getTransactionTag(), "Transaction tag is required");

    if (CollectionUtils.isNotEmpty(errors.getErrors())) {
      throw new HttpException(422, errors);
    }
  }

  private void validateField(Errors errors, String fieldName, Object fieldValue, String errorMessage) {
    if (fieldValue == null) {
      var params = List.of(new Parameter().withKey(fieldName).withValue("null"));
      errors.getErrors().add(new Error().withMessage(errorMessage).withParameters(params));
    }
  }
}

