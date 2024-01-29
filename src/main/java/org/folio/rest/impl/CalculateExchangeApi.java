package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import javax.ws.rs.core.Response;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.helper.ExchangeHelper;
import org.folio.rest.jaxrs.resource.FinanceCalculateExchange;

public class CalculateExchangeApi implements FinanceCalculateExchange {

  @Override
  public void getFinanceCalculateExchange(String from, String to, Number amount,
                                          Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                          Context vertxContext) {
    ExchangeHelper helper = new ExchangeHelper(vertxContext);
    helper.calculateExchange(from, to, amount)
      .onSuccess(body -> asyncResultHandler.handle(succeededFuture(Response.ok(body, APPLICATION_JSON).build())))
      .onFailure(e -> handleErrorResponse(asyncResultHandler, helper, e));
  }
}
