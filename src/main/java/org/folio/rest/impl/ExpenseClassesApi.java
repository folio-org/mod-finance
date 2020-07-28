package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.ResourcePathResolver.EXPENSE_CLASSES_URL;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.resource.FinanceExpenseClasses;
import org.folio.services.ExpenseClassService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class ExpenseClassesApi extends BaseApi implements FinanceExpenseClasses {
  @Autowired
  private ExpenseClassService expenseClassService;

  public ExpenseClassesApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void postFinanceExpenseClasses(String lang, ExpenseClass entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    expenseClassService.createExpenseClass(entity, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(obj -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), resourceByIdPath(EXPENSE_CLASSES_URL, obj.getId()), obj))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void getFinanceExpenseClasses(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    expenseClassService.getExpenseClasses(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(obj -> asyncResultHandler.handle(succeededFuture(buildOkResponse(obj))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void putFinanceExpenseClassesById(String id, String lang, ExpenseClass entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(new CompletionException(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY)))));
      return;
    }
    expenseClassService.updateExpenseClass(id, entity, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(v -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void getFinanceExpenseClassesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    expenseClassService.getExpenseClassById(id, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(reasonForClosure -> asyncResultHandler.handle(succeededFuture(buildOkResponse(reasonForClosure))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void deleteFinanceExpenseClassesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    expenseClassService.deleteExpenseClass(id, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(v -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
