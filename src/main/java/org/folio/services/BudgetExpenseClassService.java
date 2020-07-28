package org.folio.services;

import static java.lang.Integer.MAX_VALUE;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;

public class BudgetExpenseClassService {

  private final RestClient restClient;

  public BudgetExpenseClassService(RestClient restClient) {
    this.restClient = restClient;
  }

  public CompletableFuture<List<BudgetExpenseClass>> getBudgetExpenseClasses(String budgetId, RequestContext requestContext) {
    String query = String.format("budgetId==%s", budgetId);
    return restClient.get(query, 0, MAX_VALUE, requestContext, BudgetExpenseClassCollection.class)
      .thenApply(BudgetExpenseClassCollection::getBudgetExpenseClasses);
  }
}
