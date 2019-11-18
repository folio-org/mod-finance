package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.AbstractHelper;
import org.folio.rest.helper.LedgersHelper;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.resource.FinanceLedgers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class LedgersApi implements FinanceLedgers {

  private static final String LEDGERS_LOCATION_PREFIX = getEndpoint(FinanceLedgers.class) + "/%s";

  @Validate
  @Override
  public void postFinanceLedgers(String lang, Ledger entity, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);
    helper.createLedger(entity)
      .thenAccept(type -> handler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(LEDGERS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceLedgers(int offset, int limit, String query, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);

    helper.getLedgers(limit, offset, query)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildOkResponse(types))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void putFinanceLedgersById(String id, String lang, Ledger entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);

    // Set id if this is available only in path
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      handler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateLedger(entity)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceLedgersById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);
    helper.getLedger(id)
      .thenAccept(type -> handler.handle(succeededFuture(helper.buildOkResponse(type))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void deleteFinanceLedgersById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);
    helper.deleteLedger(id)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  private Void handleErrorResponse(Handler<AsyncResult<Response>> handler, AbstractHelper helper, Throwable t) {
    handler.handle(succeededFuture(helper.buildErrorResponse(t)));
    return null;
  }
}
