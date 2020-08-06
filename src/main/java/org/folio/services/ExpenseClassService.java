package org.folio.services;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;

import static java.util.stream.Collectors.toList;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;

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

  public CompletableFuture<List<ExpenseClass>> getExpenseClassesByBudgetIds(List<String> budgetIds, RequestContext requestContext) {
    List<CompletableFuture<List<ExpenseClass>>> futures = StreamEx
      .ofSubLists(budgetIds, MAX_IDS_FOR_GET_RQ)
      .map(ids ->  getExpenseClassesChunk(ids, requestContext))
      .collect(toList());
    return collectResultsOnSuccess(futures)
      .thenApply(listList -> listList.stream().flatMap(Collection::stream).collect(toList()))
      .thenApply(expenseClasses -> expenseClasses.stream().distinct().collect(Collectors.toList()));
  }

  private CompletableFuture<List<ExpenseClass>> getExpenseClassesChunk(List<String> ids, RequestContext requestContext) {
    String query = convertIdsToCqlQuery(ids, "budgetExpenseClass.budgetId", true);
    return getExpenseClasses(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(ExpenseClassCollection::getExpenseClasses);
  }
}
