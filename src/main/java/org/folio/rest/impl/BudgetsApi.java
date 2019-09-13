package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.AbstractHelper;
import org.folio.rest.helper.BudgetsHelper;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.resource.FinanceBudgets;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class BudgetsApi implements FinanceBudgets {

  private static final String BUDGETS_LOCATION_PREFIX = getEndpoint(FinanceBudgets.class) + "/%s";

  @Validate
  @Override
  public void postFinanceBudgets(String lang, Budget entity, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    BudgetsHelper helper = new BudgetsHelper(headers, ctx, lang);
    helper.createBudget(entity)
      .thenAccept(type -> handler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(BUDGETS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));

  }

  @Validate
  @Override
  public void getFinanceBudgets(int offset, int limit, String query, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    BudgetsHelper helper = new BudgetsHelper(headers, ctx, lang);

    helper.getBudgets(limit, offset, query)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildOkResponse(types))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));

  }

  @Validate
  @Override
  public void putFinanceBudgetsById(String id, String lang, Budget entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    BudgetsHelper helper = new BudgetsHelper(headers, ctx, lang);

    // Set id if this is available only in path
    if (StringUtils.isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      handler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateBudget(entity)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));

  }

  @Validate
  @Override
  public void getFinanceBudgetsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    BudgetsHelper helper = new BudgetsHelper(headers, ctx, lang);

    helper.getBudget(id)
      .thenAccept(type -> handler.handle(succeededFuture(helper.buildOkResponse(type))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));

  }

  @Validate
  @Override
  public void deleteFinanceBudgetsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    BudgetsHelper helper = new BudgetsHelper(headers, ctx, lang);

    helper.deleteBudget(id)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));

  }

  private Void handleErrorResponse(Handler<AsyncResult<Response>> handler, AbstractHelper helper, Throwable t) {
    handler.handle(succeededFuture(helper.buildErrorResponse(t)));
    return null;
  }
}
