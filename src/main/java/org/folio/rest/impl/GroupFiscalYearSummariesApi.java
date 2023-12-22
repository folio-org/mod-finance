package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.resource.FinanceGroupFiscalYearSummaries;
import org.folio.services.group.GroupFiscalYearTotalsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;


public class GroupFiscalYearSummariesApi extends BaseApi implements FinanceGroupFiscalYearSummaries {

  @Autowired
  private GroupFiscalYearTotalsService groupFiscalYearTotalsService;

  public GroupFiscalYearSummariesApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getFinanceGroupFiscalYearSummaries(String query, Map<String, String> headers, Handler<AsyncResult<Response>> handler, Context ctx) {
    groupFiscalYearTotalsService.getGroupFiscalYearSummaries(query, new RequestContext(ctx, headers))
      .onSuccess(groupFundFiscalYearSummaries -> handler.handle(succeededFuture(buildOkResponse(groupFundFiscalYearSummaries))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }
}
