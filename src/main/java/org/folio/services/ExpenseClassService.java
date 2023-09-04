package org.folio.services;

import static java.util.stream.Collectors.toList;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;

import io.vertx.core.Future;
import one.util.streamex.StreamEx;

public class ExpenseClassService {

  private final RestClient restClient;

  public ExpenseClassService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<ExpenseClassCollection> getExpenseClasses(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(EXPENSE_CLASSES_STORAGE_URL))
      .withQuery(query)
      .withOffset(offset)
      .withLimit(limit);
    return restClient.get(requestEntry, ExpenseClassCollection.class, requestContext);
  }

  public Future<ExpenseClass> getExpenseClassById(String id, RequestContext requestContext) {
    String endpoint = resourceByIdPath(EXPENSE_CLASSES_STORAGE_URL, id);
    return restClient.get(endpoint, ExpenseClass.class, requestContext);
  }

  public Future<ExpenseClass> createExpenseClass(ExpenseClass expenseClass, RequestContext requestContext) {
    return restClient.post(resourcesPath(EXPENSE_CLASSES_STORAGE_URL),expenseClass, ExpenseClass.class, requestContext);
  }

  public Future<Void> updateExpenseClass(String id, ExpenseClass expenseClass, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(EXPENSE_CLASSES_STORAGE_URL, id), expenseClass, requestContext);
  }

  public Future<Void> deleteExpenseClass(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(EXPENSE_CLASSES_STORAGE_URL, id), requestContext);
  }

  public Future<List<ExpenseClass>> getExpenseClassesByBudgetId(String budgetId, RequestContext requestContext) {
    String query = String.format("budgetExpenseClass.budgetId==%s", budgetId);
    var requestEntry = new RequestEntry(resourcesPath(EXPENSE_CLASSES_STORAGE_URL))
      .withQuery(query)
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE);
    return restClient.get(requestEntry, ExpenseClassCollection.class, requestContext)
      .map(ExpenseClassCollection::getExpenseClasses);
  }

  public Future<List<ExpenseClass>> getExpenseClassesByBudgetIds(List<String> budgetIds, RequestContext requestContext) {
    List<Future<List<ExpenseClass>>> futures = StreamEx
      .ofSubLists(budgetIds, MAX_IDS_FOR_GET_RQ)
      .map(ids ->  getExpenseClassesChunk(ids, requestContext))
      .collect(toList());
    return collectResultsOnSuccess(futures)
      .map(listList -> listList.stream().flatMap(Collection::stream).collect(toList()))
      .map(expenseClasses -> expenseClasses.stream().distinct().collect(Collectors.toList()));
  }

  private Future<List<ExpenseClass>> getExpenseClassesChunk(List<String> ids, RequestContext requestContext) {
    String query = convertIdsToCqlQuery(ids, "budgetExpenseClass.budgetId", true);
    return getExpenseClasses(query, 0, Integer.MAX_VALUE, requestContext)
      .map(ExpenseClassCollection::getExpenseClasses);
  }
}
