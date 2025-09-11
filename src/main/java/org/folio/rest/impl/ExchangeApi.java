package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import javax.ws.rs.core.Response;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExchangeRateCalculations;
import org.folio.rest.jaxrs.resource.FinanceCalculateExchange;
import org.folio.rest.jaxrs.resource.FinanceCalculateExchangeBatch;
import org.folio.rest.jaxrs.resource.FinanceExchangeRate;
import org.folio.services.exchange.ExchangeService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class ExchangeApi extends BaseApi implements FinanceExchangeRate, FinanceCalculateExchange, FinanceCalculateExchangeBatch {

  @Autowired
  private ExchangeService exchangeService;

  public ExchangeApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getFinanceExchangeRate(String from, String to, Map<String, String> okapiHeaders,
                                     Handler<AsyncResult<Response>> asyncResultHandler, Context context) {
    var requestContext = new RequestContext(context, okapiHeaders);
    context.executeBlocking(() -> exchangeService.getExchangeRate(from, to, requestContext)
      .onSuccess(exchangeRate -> asyncResultHandler.handle(succeededFuture(buildOkResponse(exchangeRate))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail)));
  }

  @Override
  public void getFinanceCalculateExchange(String from, String to, Number amount, Number exchangeRate, boolean manual, Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context context) {
    var requestContext = new RequestContext(context, okapiHeaders);
    context.executeBlocking(() -> exchangeService.calculateExchange(from, to, amount, exchangeRate, manual, requestContext)
      .onSuccess(convertedAmount -> asyncResultHandler.handle(succeededFuture(buildOkResponse(convertedAmount))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail)));
  }

  @Override
  public void postFinanceCalculateExchangeBatch(ExchangeRateCalculations exchangeRateCalculations, Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler, Context context) {
    var requestContext = new RequestContext(context, okapiHeaders);
    context.executeBlocking(() -> exchangeService.calculateExchangeBatch(exchangeRateCalculations, requestContext)
      .onSuccess(calculationResults -> asyncResultHandler.handle(succeededFuture(buildOkResponse(calculationResults))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail)));
  }
}
