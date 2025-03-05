package org.folio.services.financedata;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.requireNonNullElse;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.ERROR;
import static org.folio.rest.jaxrs.model.FundUpdateLog.Status.IN_PROGRESS;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FINANCE_DATA_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.JobDetails;
import org.folio.rest.jaxrs.model.JobNumber;
import org.folio.services.fund.FundUpdateLogService;
import org.folio.services.protection.AcqUnitsService;

@Log4j2
public class FinanceDataService {

  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;
  private final FundUpdateLogService fundUpdateLogService;
  private final FinanceDataValidator financeDataValidator;

  public FinanceDataService(RestClient restClient, AcqUnitsService acqUnitsService,
                            FundUpdateLogService fundUpdateLogService, FinanceDataValidator financeDataValidator) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
    this.fundUpdateLogService = fundUpdateLogService;
    this.financeDataValidator = financeDataValidator;
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

    financeDataValidator.validateFinanceDataCollection(financeDataCollection, getFiscalYearId(financeDataCollection));
    calculateAfterAllocation(financeDataCollection);

    return financeDataValidator.compareWithExistingData(financeDataCollection, requestContext)
      .compose(v -> processFinanceData(financeDataCollection, requestContext))
      .onSuccess(ar -> log.info("putFinanceData:: Finance data collection was updated successfully"))
      .onFailure(t -> log.warn("putFinanceData:: Failed to update finance data collection", t));
  }

  private Future<FyFinanceDataCollection> processFinanceData(FyFinanceDataCollection fdCollection, RequestContext requestContext) {
    if (fdCollection.getUpdateType().equals(FyFinanceDataCollection.UpdateType.PREVIEW)) {
      log.info("putFinanceData:: Running dry-run mode finance data collection");
      return succeededFuture(fdCollection);
    }
    var fundUpdateLogId = UUID.randomUUID().toString();
    return processLogs(fundUpdateLogId, fdCollection, requestContext)
      .compose(log -> updateFinanceData(fdCollection, requestContext))
      .onSuccess(updatedFdCollection -> updateLogs(fundUpdateLogId, COMPLETED, updatedFdCollection, requestContext))
      .onFailure(asyncResult -> updateLogs(fundUpdateLogId, ERROR, null, requestContext));
  }

  private String getFiscalYearId(FyFinanceDataCollection fyFinanceDataCollection) {
    return fyFinanceDataCollection.getFyFinanceData().getFirst().getFiscalYearId();
  }

  private Future<FyFinanceDataCollection> updateFinanceData(FyFinanceDataCollection financeDataCollection,
                                                            RequestContext requestContext) {
    log.debug("updateFinanceData:: Trying to update finance data collection with size: {}", financeDataCollection.getTotalRecords());
    return restClient.put(resourcesPath(FINANCE_DATA_STORAGE), financeDataCollection, FyFinanceDataCollection.class, requestContext);
  }

  private Future<FundUpdateLog> processLogs(String fundUpdateLogId, FyFinanceDataCollection financeDataCollection,
                                            RequestContext requestContext) {
    return fundUpdateLogService.getJobNumber(requestContext)
      .compose(jobNumber -> {
        var fundUpdateLog = createFundUpdateLog(fundUpdateLogId, jobNumber, financeDataCollection);
        return fundUpdateLogService.createFundUpdateLog(fundUpdateLog, requestContext);
      });
  }

  private FundUpdateLog createFundUpdateLog(String fundUpdateLogId, JobNumber jobNumber, FyFinanceDataCollection financeDataCollection) {
    var jobDetails = new JobDetails().withAdditionalProperty("fyFinanceData", financeDataCollection.getFyFinanceData());
    var financeData = financeDataCollection.getFyFinanceData().getFirst();
    var jobName = StringUtils.isNotEmpty(financeDataCollection.getWorksheetName())
      ? financeDataCollection.getWorksheetName()
      : String.format("%s-%s-%s", financeData.getFiscalYearCode(), financeData.getLedgerCode(),
      new SimpleDateFormat("yyyyMMdd").format(new Date()));

    return new FundUpdateLog().withId(fundUpdateLogId)
      .withJobName(jobName)
      .withStatus(IN_PROGRESS)
      .withRecordsCount(financeDataCollection.getTotalRecords())
      .withJobDetails(jobDetails)
      .withJobNumber(Integer.valueOf(jobNumber.getSequenceNumber()));
  }

  private void updateLogs(String fundUpdateLogId, FundUpdateLog.Status status, FyFinanceDataCollection updateFdCollection,
                          RequestContext requestContext) {
    fundUpdateLogService.getFundUpdateLogById(fundUpdateLogId, requestContext)
      .compose(fundUpdateLog -> {
        if (updateFdCollection != null) {
          var jobDetails = new JobDetails().withAdditionalProperty("fyFinanceData", updateFdCollection.getFyFinanceData());
          fundUpdateLog.setJobDetails(jobDetails);
        }
        fundUpdateLog.setStatus(status);
        return fundUpdateLogService.updateFundUpdateLog(fundUpdateLog, requestContext);
      });
  }

  private void calculateAfterAllocation(FyFinanceDataCollection financeDataCollection) {
    log.info("calculateAfterAllocation:: Calculating after allocation for finance data collection, FY:{}", getFiscalYearId(financeDataCollection));
    financeDataCollection.getFyFinanceData().forEach(financeData -> {
      if (financeData.getBudgetId() == null && financeData.getBudgetAllocationChange() == null) {
        financeData.setBudgetAfterAllocation(null);
        return;
      }
      var allocationChange = BigDecimal.valueOf(requireNonNullElse(financeData.getBudgetAllocationChange(), 0.0));
      var currentAllocation = BigDecimal.valueOf(requireNonNullElse(financeData.getBudgetCurrentAllocation(), 0.0));
      var afterAllocation = currentAllocation.add(allocationChange);
      financeData.setBudgetAfterAllocation(afterAllocation.doubleValue());
    });
  }
}

