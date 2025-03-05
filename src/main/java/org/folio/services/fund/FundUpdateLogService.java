package org.folio.services.fund;

import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FUND_UPDATE_LOGS;
import static org.folio.rest.util.ResourcePathResolver.JOB_NUMBER;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Future;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FundUpdateLogCollection;
import org.folio.rest.jaxrs.model.JobNumber;
import org.folio.services.protection.AcqUnitsService;

public class FundUpdateLogService {

  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;

  public FundUpdateLogService(RestClient restClient, AcqUnitsService acqUnitsService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<FundUpdateLogCollection> getFundUpdateLogs(String query, int offset, int limit,
                                                           RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .compose(effectiveQuery -> {
        var requestEntry = new RequestEntry(resourcesPath(FUND_UPDATE_LOGS))
          .withOffset(offset).withLimit(limit).withQuery(effectiveQuery);
        return restClient.get(requestEntry.buildEndpoint(), FundUpdateLogCollection.class, requestContext);
      });
  }

  public Future<FundUpdateLog> getFundUpdateLogById(String jobId, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(FUND_UPDATE_LOGS, jobId), FundUpdateLog.class, requestContext);
  }

  public Future<FundUpdateLog> createFundUpdateLog(FundUpdateLog fundUpdateLog, RequestContext requestContext) {
    return restClient.post(resourcesPath(FUND_UPDATE_LOGS), fundUpdateLog, FundUpdateLog.class, requestContext);
  }

  public Future<Void> updateFundUpdateLog(FundUpdateLog fundUpdateLog, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(FUND_UPDATE_LOGS, fundUpdateLog.getId()), fundUpdateLog, requestContext);
  }

  public Future<Void> deleteFundUpdateLog(String jobId, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(FUND_UPDATE_LOGS, jobId), requestContext);
  }

  public Future<JobNumber> getJobNumber(RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(JOB_NUMBER))
      .withQueryParameter("type", JobNumber.Type.FUND_UPDATE_LOGS.value());
    return restClient.get(requestEntry.buildEndpoint(), JobNumber.class, requestContext);
  }
}
