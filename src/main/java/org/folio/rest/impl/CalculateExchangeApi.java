package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.helper.ExchangeRateHelper;
import org.folio.rest.jaxrs.resource.FinanceCalculateExchange;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

public class CalculateExchangeApi implements FinanceCalculateExchange {

  @Override
  public void getFinanceCalculateExchange(String sourceCurrency, String targetCurrency, Number amount, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ExchangeRateHelper helper = new ExchangeRateHelper(vertxContext);
    var rate = helper.getExchangeRate(sourceCurrency, targetCurrency);
    vertxContext.executeBlocking(promise -> promise.complete(helper.calculateExchange(rate.getExchangeRate(), amount.doubleValue())))
      .onSuccess(body -> asyncResultHandler.handle(succeededFuture(Response.ok(body, APPLICATION_JSON).build())))
      .onFailure(e -> handleErrorResponse(asyncResultHandler, helper, e));
  }
}
