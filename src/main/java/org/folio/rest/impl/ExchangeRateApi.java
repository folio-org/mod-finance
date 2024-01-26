package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.ExchangeHelper;
import org.folio.rest.jaxrs.resource.FinanceExchangeRate;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class ExchangeRateApi implements FinanceExchangeRate {
  @Override
  @Validate
  public void getFinanceExchangeRate(String from, String to, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ExchangeHelper helper = new ExchangeHelper(vertxContext);
    vertxContext.executeBlocking(promise -> promise.complete(helper.getExchangeRate(from, to)))
      .onSuccess(body -> asyncResultHandler.handle(succeededFuture(Response.ok(body, APPLICATION_JSON).build())))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, helper, t));
  }
}
