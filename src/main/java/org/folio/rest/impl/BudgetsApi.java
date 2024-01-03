package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.resource.FinanceBudgets;
import org.folio.services.budget.BudgetExpenseClassTotalsService;
import org.folio.services.budget.BudgetService;
import org.folio.services.budget.CreateBudgetService;
import org.folio.services.budget.RecalculateBudgetService;
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
  @Autowired
  private BudgetService budgetService;
  @Autowired
  private CreateBudgetService createBudgetService;
  @Autowired
  private RecalculateBudgetService recalculateBudgetService;


  public BudgetsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postFinanceBudgets(SharedBudget budget, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
                                 Context ctx) {

    RequestContext requestContext = new RequestContext(ctx, headers);
    createBudgetService.createBudget(budget, requestContext)
      .onSuccess(createdBudget -> handler.handle(succeededFuture(buildResponseWithLocation(headers.get(OKAPI_URL), String.format(BUDGETS_LOCATION_PREFIX, createdBudget.getId()), createdBudget))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void getFinanceBudgets(String totalRecords, int offset, int limit, String query, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    budgetService.getBudgets(query, offset, limit, new RequestContext(ctx, headers))
      .onSuccess(budgets -> handler.handle(succeededFuture(buildOkResponse(budgets))))
      .onFailure(fail -> handleErrorResponse(handler, fail));

  }

  @Validate
  @Override
  public void putFinanceBudgetsById(String id, SharedBudget budget, Map<String, String> headers,
                                    Handler<AsyncResult<Response>> handler, Context ctx) {


    // Set id if this is available only in path
    if (StringUtils.isEmpty(budget.getId())) {
      budget.setId(id);
    } else if (!id.equals(budget.getId())) {
      handler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
      return;
    }

    budgetService.updateBudget(budget, new RequestContext(ctx, headers))
      .onSuccess(v -> handler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(handler, fail));

  }

  @Validate
  @Override
  public void getFinanceBudgetsById(String id, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    budgetService.getBudgetById(id, new RequestContext(ctx, headers))
      .onSuccess(budget -> handler.handle(succeededFuture(buildOkResponse(budget))))
      .onFailure(fail -> handleErrorResponse(handler, fail));

  }

  @Validate
  @Override
  public void deleteFinanceBudgetsById(String id, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    budgetService.deleteBudget(id, new RequestContext(ctx, headers))
      .onSuccess(v -> handler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(handler, fail));

  }

  @Override
  public void getFinanceBudgetsExpenseClassesTotalsById(String budgetId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    budgetExpenseClassTotalsService.getExpenseClassTotals(budgetId, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(obj -> asyncResultHandler.handle(succeededFuture(buildOkResponse(obj))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  public void postFinanceBudgetsRecalculateById(String budgetId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    recalculateBudgetService.recalculateBudget(budgetId, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(obj -> asyncResultHandler.handle(succeededFuture(buildOkResponse(obj))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

}
