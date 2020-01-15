package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.helper.TransactionsHelper;
import org.folio.rest.jaxrs.resource.FinanceReleaseEncumbranceId;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class EncumbranceApi implements FinanceReleaseEncumbranceId {

  @Override
  public void postFinanceReleaseEncumbranceById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);
    helper.getTransaction(id)
      .thenCompose(helper::releaseTransaction)
      .thenAccept(v -> asyncResultHandler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

}
