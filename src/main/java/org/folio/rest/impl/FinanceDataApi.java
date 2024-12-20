package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.Map;
import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.resource.FinanceFinanceData;
import org.folio.services.financedata.FinanceDataService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class FinanceDataApi extends BaseApi implements FinanceFinanceData {

  @Autowired
  private FinanceDataService financeDataService;

  public FinanceDataApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getFinanceFinanceData(String query, String totalRecords, int offset, int limit,
                                    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                    Context vertxContext) {
    financeDataService.getFinanceDataWithAcqUnitsRestriction(query, offset, limit,
        new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(financeData -> asyncResultHandler.handle(succeededFuture(buildOkResponse(financeData))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void putFinanceFinanceData(FyFinanceDataCollection entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    financeDataService.putFinanceData(entity, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(financeDataCollection -> asyncResultHandler.handle(succeededFuture(buildOkResponse(financeDataCollection))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
