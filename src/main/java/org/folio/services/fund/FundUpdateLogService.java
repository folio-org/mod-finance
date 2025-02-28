package org.folio.services.fund;

import static org.folio.rest.util.ResourcePathResolver.FUND_UPDATE_LOGS;
import static org.folio.rest.util.ResourcePathResolver.JOB_NUMBER;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Future;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FundUpdateLogCollection;
import org.folio.rest.jaxrs.model.SequenceNumber;

public class FundUpdateLogService {

  private final RestClient restClient;

  public FundUpdateLogService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<FundUpdateLogCollection> getFundUpdateLogs(String query, int offset, int limit,
                                                           RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FUND_UPDATE_LOGS))
      .withOffset(offset).withLimit(limit).withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FundUpdateLogCollection.class, requestContext);
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

  public Future<SequenceNumber> getJobNumber(RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(JOB_NUMBER))
      .withQueryParameter("type", "Logs");
    return restClient.get(requestEntry.buildEndpoint(), SequenceNumber.class, requestContext);
  }
}
