package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.BudgetsHelper;
import org.folio.rest.helper.GroupFundFiscalYearHelper;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.resource.FinanceBudgets;
import org.folio.rest.util.HelperUtils;
import org.folio.services.BudgetExpenseClassTotalsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class BudgetsApi extends BaseApi implements FinanceBudgets {

  private static final String BUDGETS_LOCATION_PREFIX = getEndpoint(FinanceBudgets.class) + "/%s";

  @Autowired
  private BudgetExpenseClassTotalsService budgetExpenseClassTotalsService;

  public BudgetsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

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
      .exceptionally(fail -> HelperUtils.handleErrorResponse(handler, budgetsHelper, fail));
  }

  @Validate
  @Override
  public void getFinanceBudgets(int offset, int limit, String query, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    BudgetsHelper budgetsHelper = new BudgetsHelper(headers, ctx, lang);

    budgetsHelper.getBudgets(query, offset, limit)
      .thenAccept(budgets -> handler.handle(succeededFuture(budgetsHelper.buildOkResponse(budgets))))
      .exceptionally(fail -> HelperUtils.handleErrorResponse(handler, budgetsHelper, fail));

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

    if (budgetsHelper.newAllowableAmountsExceeded(budget)){
      handler.handle(succeededFuture(budgetsHelper.buildErrorResponse(422)));
      return;
    }

    budgetsHelper.updateBudget(budget)
      .thenAccept(v -> handler.handle(succeededFuture(budgetsHelper.buildNoContentResponse())))
      .exceptionally(fail -> HelperUtils.handleErrorResponse(handler, budgetsHelper, fail));

  }

  @Validate
  @Override
  public void getFinanceBudgetsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    BudgetsHelper budgetsHelper = new BudgetsHelper(headers, ctx, lang);

    budgetsHelper.getBudget(id)
      .thenAccept(budget -> handler.handle(succeededFuture(budgetsHelper.buildOkResponse(budget))))
      .exceptionally(fail -> HelperUtils.handleErrorResponse(handler, budgetsHelper, fail));

  }

  @Validate
  @Override
  public void deleteFinanceBudgetsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    BudgetsHelper budgetsHelper = new BudgetsHelper(headers, ctx, lang);

    budgetsHelper.deleteBudget(id)
      .thenAccept(v -> handler.handle(succeededFuture(budgetsHelper.buildNoContentResponse())))
      .exceptionally(fail -> HelperUtils.handleErrorResponse(handler, budgetsHelper, fail));

  }

  @Override
  public void getFinanceBudgetsExpenseClassesTotalsById(String budgetId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    budgetExpenseClassTotalsService.getExpenseClassTotals(budgetId, vertxContext, okapiHeaders)
      .thenAccept(obj -> asyncResultHandler.handle(succeededFuture(buildOkResponse(obj))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

}
