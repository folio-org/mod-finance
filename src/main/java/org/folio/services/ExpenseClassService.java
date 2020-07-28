package org.folio.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;

public class ExpenseClassService {

  private final RestClient expenseClassRestClient;

  public ExpenseClassService(RestClient expenseClassRestClient) {
    this.expenseClassRestClient = expenseClassRestClient;
  }

  public CompletableFuture<ExpenseClassCollection> getExpenseClasses(String query, int offset, int limit, RequestContext requestContext) {
    return expenseClassRestClient.get(query, offset, limit, requestContext, ExpenseClassCollection.class);
  }

  public CompletableFuture<ExpenseClass> getExpenseClassById(String id, RequestContext requestContext) {
    return expenseClassRestClient.getById(id, requestContext, ExpenseClass.class);
  }

  public CompletableFuture<ExpenseClass> createExpenseClass(ExpenseClass expenseClass, RequestContext requestContext) {
    return expenseClassRestClient.post(expenseClass, requestContext, ExpenseClass.class);
  }

  public CompletableFuture<Void> updateExpenseClass(String id, ExpenseClass expenseClass, RequestContext requestContext) {
    return expenseClassRestClient.put(id, expenseClass, requestContext);
  }

  public CompletableFuture<Void> deleteExpenseClass(String id, RequestContext requestContext) {
    return expenseClassRestClient.delete(id, requestContext);
  }

  public CompletableFuture<List<ExpenseClass>> getExpenseClassesByBudgetId(String budgetId, RequestContext requestContext) {
    String query = String.format("budgetExpenseClass.budgetId==%s", budgetId);
    return expenseClassRestClient.get(query,0, Integer.MAX_VALUE, requestContext, ExpenseClassCollection.class)
      .thenApply(ExpenseClassCollection::getExpenseClasses);
  }
}
