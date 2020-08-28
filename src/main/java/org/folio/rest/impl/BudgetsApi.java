package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.BudgetsHelper;
import org.folio.rest.helper.GroupFundFiscalYearHelper;
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
    BudgetsHelper budgetsHelper = new BudgetsHelper(headers, ctx, lang);
    GroupFundFiscalYearHelper groupFundFiscalYearHelper = new GroupFundFiscalYearHelper(headers, ctx, lang);
    budgetsHelper.createBudget(entity)
      .thenCompose(createdBudget -> groupFundFiscalYearHelper.updateBudgetIdForGroupFundFiscalYears(createdBudget)
        .thenApply(v -> createdBudget))
      .thenAccept(createdBudget -> handler.handle(
          succeededFuture(budgetsHelper.buildResponseWithLocation(String.format(BUDGETS_LOCATION_PREFIX, createdBudget.getId()), createdBudget))))
      .exceptionally(fail -> handleErrorResponse(handler, budgetsHelper, fail));
  }

  @Validate
  @Override
  public void getFinanceBudgets(int offset, int limit, String query, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    BudgetsHelper budgetsHelper = new BudgetsHelper(headers, ctx, lang);

    budgetsHelper.getBudgets(limit, offset, query)
      .thenAccept(budgets -> handler.handle(succeededFuture(budgetsHelper.buildOkResponse(budgets))))
      .exceptionally(fail -> handleErrorResponse(handler, budgetsHelper, fail));

  }

  @Validate
  @Override
  public void putFinanceBudgetsById(String id, String lang, Budget budget, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    BudgetsHelper budgetsHelper = new BudgetsHelper(headers, ctx, lang);

    // Set id if this is available only in path
    if (StringUtils.isEmpty(budget.getId())) {
      budget.setId(id);
    } else if (!id.equals(budget.getId())) {
      budgetsHelper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      handler.handle(succeededFuture(budgetsHelper.buildErrorResponse(422)));
      return;
    }

    budgetsHelper.updateBudget(budget)
      .thenAccept(v -> handler.handle(succeededFuture(budgetsHelper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, budgetsHelper, fail));

  }

  @Validate
  @Override
  public void getFinanceBudgetsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    BudgetsHelper budgetsHelper = new BudgetsHelper(headers, ctx, lang);

    budgetsHelper.getBudget(id)
      .thenAccept(budget -> handler.handle(succeededFuture(budgetsHelper.buildOkResponse(budget))))
      .exceptionally(fail -> handleErrorResponse(handler, budgetsHelper, fail));

  }

  @Validate
  @Override
  public void deleteFinanceBudgetsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    BudgetsHelper budgetsHelper = new BudgetsHelper(headers, ctx, lang);

    budgetsHelper.deleteBudget(id)
      .thenAccept(v -> handler.handle(succeededFuture(budgetsHelper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, budgetsHelper, fail));

  }

}
