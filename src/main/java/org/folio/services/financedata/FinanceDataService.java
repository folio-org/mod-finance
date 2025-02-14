package org.folio.services.financedata;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.ERROR;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.IN_PROGRESS;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FINANCE_DATA_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.JobDetails;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.services.budget.BudgetService;
import org.folio.services.fund.FundService;
import org.folio.services.fund.FundUpdateLogService;
import org.folio.services.protection.AcqUnitsService;

@Log4j2
public class FinanceDataService {

  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;
  private final FundUpdateLogService fundUpdateLogService;
  private final BudgetService budgetService;
  private final FundService fundService;

  public FinanceDataService(RestClient restClient, AcqUnitsService acqUnitsService,
                            FundUpdateLogService fundUpdateLogService, BudgetService budgetService, FundService fundService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
    this.fundUpdateLogService = fundUpdateLogService;
    this.budgetService = budgetService;
    this.fundService = fundService;
  }

  /**
   * The method will fetch finance data with acq units restriction
   * 1. First fetch acq units for the user
   * 2. Build cql clause for finance data with acq units
   *
   * @param query          query to filter finance data
   * @param offset         offset
   * @param limit          limit
   * @param requestContext request context
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

    validateFinanceDataCollection(financeDataCollection, getFiscalYearId(financeDataCollection), requestContext);
    calculateAfterAllocation(financeDataCollection);
    if (financeDataCollection.getUpdateType().equals(FyFinanceDataCollection.UpdateType.PREVIEW)) {
      log.info("putFinanceData:: Running dry-run mode finance data collection");
      return succeededFuture(financeDataCollection);
    }

    var fundUpdateLogId = UUID.randomUUID().toString();
    return processLogs(fundUpdateLogId, financeDataCollection, requestContext)
      .compose(fundUpdateLog -> updateFinanceData(financeDataCollection, requestContext))
      .map(v -> financeDataCollection)
      .onSuccess(asyncResult -> updateLogs(fundUpdateLogId, COMPLETED, requestContext))
      .onFailure(asyncResult -> updateLogs(fundUpdateLogId, ERROR, requestContext));
  }

  private String getFiscalYearId(FyFinanceDataCollection fyFinanceDataCollection) {
    return fyFinanceDataCollection.getFyFinanceData().get(0).getFiscalYearId();
  }

  private void validateFinanceDataCollection(FyFinanceDataCollection financeDataCollection, String fiscalYearId,
                                             RequestContext requestContext) {
    validateForDuplication(financeDataCollection);
    for (int i = 0; i < financeDataCollection.getFyFinanceData().size(); i++) {
      var financeData = financeDataCollection.getFyFinanceData().get(i);
      validateFinanceDataFields(financeData, i, fiscalYearId, requestContext);

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

  private Future<Void> updateFinanceData(FyFinanceDataCollection financeDataCollection,
                                         RequestContext requestContext) {
    log.debug("updateFinanceData:: Trying to update finance data collection with size: {}", financeDataCollection.getTotalRecords());
    return restClient.put(resourcesPath(FINANCE_DATA_STORAGE), financeDataCollection, requestContext);
  }

  private Future<FundUpdateLog> processLogs(String fundUpdateLogId, FyFinanceDataCollection financeDataCollection,
                                            RequestContext requestContext) {
    var jobDetails = new JobDetails().withAdditionalProperty("fyFinanceData", financeDataCollection.getFyFinanceData());
    var fundUpdateLog = new FundUpdateLog().withId(fundUpdateLogId)
      .withJobName("Update finance data") // TODO: Update job name generation
      .withStatus(IN_PROGRESS)
      .withRecordsCount(financeDataCollection.getTotalRecords())
      .withJobDetails(jobDetails)
      .withJobNumber(1);
    return fundUpdateLogService.createFundUpdateLog(fundUpdateLog, requestContext);
  }

  private void updateLogs(String fundUpdateLog, FundUpdateLog.Status status,
                          RequestContext requestContext) {
    fundUpdateLogService.getFundUpdateLogById(fundUpdateLog, requestContext)
      .compose(log -> {
        log.setStatus(status);
        return fundUpdateLogService.updateFundUpdateLog(log, requestContext);
      });
  }

  private void validateForDuplication(FyFinanceDataCollection financeDataCollection) {
    var financeFundBudgetFiscalYearIds = financeDataCollection.getFyFinanceData()
      .stream().collect(Collectors.groupingBy(
        financeData -> financeData.getFundId() + financeData.getBudgetId() + financeData.getFiscalYearId()));

    if (financeFundBudgetFiscalYearIds.size() > 1) {
      var error = createError("Finance data collection contains duplicate fund, budget and fiscal year IDs",
        "financeData", "duplicate");
      log.warn("validateForDuplication:: Validation error: {}", error.getMessage());
      throw new HttpException(422, new Errors().withErrors(List.of(error)));
    }
  }

  private void validateFinanceDataFields(FyFinanceData financeData, int i, String fiscalYearId, RequestContext requestContext) {
    List<Error> combinedErrors = new ArrayList<>();

    if (!financeData.getFiscalYearId().equals(fiscalYearId)) {
      combinedErrors.add(createError(
        String.format("Fiscal year ID must be the same as other fiscal year ID(s) '[%s]' in the request", fiscalYearId),
        String.format("financeData[%s].fiscalYearId", i), financeData.getFiscalYearId())
      );
    }

    validateFundId(combinedErrors, financeData, i, requestContext);
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundCode", i), financeData.getFundCode(), "Fund code is required");
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundName", i), financeData.getFundName(), "Fund name is required");
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundDescription", i), financeData.getFundDescription(), "Fund description is required");
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundStatus", i), financeData.getFundStatus(), "Fund status is required");
    if (financeData.getBudgetId() != null) {
      validateBudgetId(combinedErrors, financeData, i, requestContext);
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetName", i), financeData.getBudgetName(), "Budget name is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetStatus", i), financeData.getBudgetStatus(), "Budget status is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetInitialAllocation", i), financeData.getBudgetInitialAllocation(), "Budget initial allocation is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetCurrentAllocation", i), financeData.getBudgetCurrentAllocation(), "Budget current allocation is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetAllowableExpenditure", i), financeData.getBudgetAllowableExpenditure(), "Budget allowable expenditure is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetAllowableEncumbrance", i), financeData.getBudgetAllowableEncumbrance(), "Budget allowable encumbrance is required");
    }

    if (CollectionUtils.isNotEmpty(combinedErrors)) {
      var errors = new Errors().withErrors(combinedErrors).withTotalRecords(combinedErrors.size());
      throw new HttpException(422, errors);
    }
  }

  private void validateFundId(List<Error> combinedErrors, FyFinanceData financeData, int i, RequestContext requestContext) {
    fundService.getFundById(financeData.getFundId(), requestContext)
      .compose(fund -> {
        if (Objects.isNull(fund)) {
          combinedErrors.add(createError("Fund is not found", String.format("financeData[%s].fundId", i), financeData.getFundId()));
          return succeededFuture();
        }
        if (financeData.getLedgerId() != null && !Objects.equals(fund.getLedgerId(), financeData.getLedgerId())) {
          combinedErrors.add(createError("Fund ledger ID must be the same as ledger ID", String.format("financeData[%s].fundId", i), financeData.getFundId()));
          return succeededFuture();
        }
        return succeededFuture();
      });
  }

  private void validateBudgetId(List<Error> combinedErrors, FyFinanceData financeData, int i, RequestContext requestContext) {
    budgetService.getBudgetById(financeData.getBudgetId(), requestContext)
      .compose(budget -> {
        if (Objects.isNull(budget)) {
          combinedErrors.add(createError("Budget is not found", String.format("financeData[%s].budgetId", i), financeData.getBudgetId()));
        }
        if (!Objects.equals(budget.getFundId(), financeData.getFundId())) {
          combinedErrors.add(createError("Budget fund ID must be the same as fund ID", String.format("financeData[%s].budgetId", i), financeData.getBudgetId()));
        }
        return succeededFuture(budget);
      });
  }

  private void validateRequiredField(List<Error> combinedErrors, String fieldName, Object fieldValue, String errorMessage) {
    if (fieldValue == null) {
      combinedErrors.add(createError(errorMessage, fieldName, "null"));
    }
  }

  private Error createError(String message, String key, String value) {
    log.warn("Validation error: {}", message);
    var param = new Parameter().withKey(key).withValue(value);
    return new Error().withMessage(message).withParameters(List.of(param));
  }
}

