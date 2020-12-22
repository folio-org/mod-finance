package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversErrors;
import org.folio.services.LedgerRolloverErrorsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class LedgerRolloversErrorsApi extends BaseApi implements FinanceLedgerRolloversErrors {

  @Autowired
  private LedgerRolloverErrorsService ledgerRolloverErrorsService;

  public LedgerRolloversErrorsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getFinanceLedgerRolloversErrors(String query, int offset, int limit, String lang, String accept,
                                              Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    ledgerRolloverErrorsService.retrieveLedgersRolloverErrors(query, offset, limit, accept, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(rolloverErrors -> asyncResultHandler.handle(succeededFuture(buildOkResponse(rolloverErrors))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
