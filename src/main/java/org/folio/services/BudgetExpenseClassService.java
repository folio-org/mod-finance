package org.folio.services;

import static java.lang.Integer.MAX_VALUE;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.dao.BudgetExpenseClassDAO;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;

import io.vertx.core.Context;

public class BudgetExpenseClassService {

  private final BudgetExpenseClassDAO budgetExpenseClassDAO;

  public BudgetExpenseClassService(BudgetExpenseClassDAO budgetExpenseClassDAO) {
    this.budgetExpenseClassDAO = budgetExpenseClassDAO;
  }

  public CompletableFuture<List<BudgetExpenseClass>> getBudgetExpenseClasses(String budgetId, Context context, Map<String, String> headers) {
    String query = String.format("budgetId==%s", budgetId);
    return budgetExpenseClassDAO.get(query, 0, MAX_VALUE, context, headers)
      .thenApply(BudgetExpenseClassCollection::getBudgetExpenseClasses);
  }
}
