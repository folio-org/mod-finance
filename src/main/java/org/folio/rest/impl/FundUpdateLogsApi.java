package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.ResourcePathResolver.FUND_UPDATE_LOGS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.resource.FinanceFundUpdateLogs;
import org.folio.services.fund.FundUpdateLogService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class FundUpdateLogsApi extends BaseApi implements FinanceFundUpdateLogs {

  @Autowired
  private FundUpdateLogService fundUpdateLogService;

  public FundUpdateLogsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getFinanceFundUpdateLogs(String query, String totalRecords, int offset, int limit,
                                       Map<String, String> okapiHeaders,
                                       Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    fundUpdateLogService.getFundUpdateLogs(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(logs -> asyncResultHandler.handle(succeededFuture(buildOkResponse(logs))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void getFinanceFundUpdateLogsById(String id, Map<String, String> okapiHeaders,
                                           Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    fundUpdateLogService.getFundUpdateLogById(id, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(log -> asyncResultHandler.handle(succeededFuture(buildOkResponse(log))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void postFinanceFundUpdateLogs(FundUpdateLog entity, Map<String, String> okapiHeaders,
                                        Handler<AsyncResult<Response>> handler, Context vertxContext) {
    fundUpdateLogService.createFundUpdateLog(entity, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(log -> handler.handle(succeededFuture(buildResponseWithLocation(
        okapiHeaders.get(OKAPI_URL), resourceByIdPath(FUND_UPDATE_LOGS, log.getId()), log))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  @Override
  @Validate
  public void putFinanceFundUpdateLogsById(String id, FundUpdateLog entity, Map<String, String> okapiHeaders,
                                           Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(
        new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
      return;
    }

    fundUpdateLogService.updateFundUpdateLog(entity, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(updatedLog -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void deleteFinanceFundUpdateLogsById(String id, Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    fundUpdateLogService.deleteFundUpdateLog(id, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(deletedLog -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
