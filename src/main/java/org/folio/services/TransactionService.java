package org.folio.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.dao.TransactionDAO;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Context;

public class TransactionService {

  private final TransactionDAO transactionDAO;

  public TransactionService(TransactionDAO transactionDAO) {
    this.transactionDAO = transactionDAO;
  }

  public CompletableFuture<List<Transaction>> getTransactions(Budget budget, Context context, Map<String, String> headers) {
    String query = String.format("fromFundId==%s AND fiscalYearId==%s", budget.getFundId(), budget.getFiscalYearId());
    return transactionDAO.get(query, 0, Integer.MAX_VALUE, context, headers)
      .thenApply(TransactionCollection::getTransactions);
  }
}
