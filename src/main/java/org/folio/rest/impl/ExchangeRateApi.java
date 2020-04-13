package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.ExchangeRateHelper;
import org.folio.rest.jaxrs.resource.FinanceExchangeRate;

import javax.ws.rs.core.Response;
import java.util.Map;

public class ExchangeRateApi implements FinanceExchangeRate {
  @Override
  @Validate
  public void getFinanceExchangeRate(String from, String to, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ExchangeRateHelper helper = new ExchangeRateHelper(vertxContext);
    helper.getExchangeRate(from, to)
      .thenAccept(body -> asyncResultHandler.handle(succeededFuture(Response.ok(body, APPLICATION_JSON).build())))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, helper, t));
  }
}
