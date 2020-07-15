package org.folio.services;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.dao.ExpenseClassDAO;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;

import io.vertx.core.Context;

public class ExpenseClassService {
  private final ExpenseClassDAO expenseClassDAO;

  public ExpenseClassService(ExpenseClassDAO expenseClassDAO) {
    this.expenseClassDAO = expenseClassDAO;
  }

  public CompletableFuture<ExpenseClassCollection> getExpenseClasses(String query, int offset, int limit, Context context,
      Map<String, String> headers) {
    return expenseClassDAO.get(query, offset, limit, context, headers);
  }

  public CompletableFuture<ExpenseClass> getExpenseClassById(String id, Context context, Map<String, String> headers) {
    return expenseClassDAO.getById(id, context, headers);
  }

  public CompletableFuture<ExpenseClass> createExpenseClass(ExpenseClass expenseClass, Context context, Map<String, String> headers) {
    return expenseClassDAO.save(expenseClass, context, headers);
  }

  public CompletableFuture<Void> updateExpenseClass(String id, ExpenseClass expenseClass, Context context, Map<String, String> headers) {
    return expenseClassDAO.update(id, expenseClass, context, headers);
  }

  public CompletableFuture<Void> deleteExpenseClass(String id, Context context, Map<String, String> headers) {
    return expenseClassDAO.delete(id, context, headers);
  }
}
