package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.helper.AwaitingPaymentHelper;
import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.resource.FinanceAwaitingPayment;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class AwaitingPaymentApi implements FinanceAwaitingPayment {

  @Override
  public void postFinanceAwaitingPayment(String lang, AwaitingPayment awaitingPayment, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> handler, Context vertxContext) {
    AwaitingPaymentHelper helper = new AwaitingPaymentHelper(okapiHeaders, vertxContext, lang);
    helper.moveToAwaitingPayment(awaitingPayment)
      .thenAccept(type -> handler.handle(succeededFuture(helper.buildOkResponse(awaitingPayment))))
      .exceptionally(t -> {
        handler.handle(succeededFuture(helper.buildErrorResponse(t)));
        return null;
      });
  }
}
