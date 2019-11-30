package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.TransactionsHelper;
import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.resource.FinanceAwaitingPayment;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class AwaitingPaymentApi implements FinanceAwaitingPayment {

  @Override
  @Validate
  public void postFinanceAwaitingPayment(String lang, AwaitingPayment awaitingPayment, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> handler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);
    helper.moveToAwaitingPayment(awaitingPayment)
      .thenAccept(v -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(t -> handleErrorResponse(handler, helper, t));
  }
}