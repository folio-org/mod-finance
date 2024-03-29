package org.folio.services.transactions;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;

import io.vertx.core.Future;

public class TransactionStrategyFactory {

  private Map<Transaction.TransactionType, TransactionTypeManagingStrategy> strategies;

  public TransactionStrategyFactory(Set<TransactionTypeManagingStrategy> strategySet) {
    createStrategy(strategySet);
  }

  public TransactionTypeManagingStrategy findStrategy(Transaction.TransactionType transactionType) {
    return strategies.get(transactionType);
  }

  private void createStrategy(Set<TransactionTypeManagingStrategy> strategySet) {
    strategies = new EnumMap<>(Transaction.TransactionType.class);
    strategySet.forEach(
      strategy -> strategies.put(strategy.getStrategyName(), strategy));
  }

  public Future<Transaction> createTransaction(Transaction.TransactionType type, Transaction transaction, RequestContext requestContext) {
    return findStrategy(type).createTransaction(transaction, requestContext);
  }

  public Future<Void> updateTransaction(Transaction.TransactionType type, Transaction transaction, RequestContext requestContext) {
    return findStrategy(type).updateTransaction(transaction, requestContext);
  }

  public Future<Void> deleteTransaction(Transaction.TransactionType type, Transaction transaction, RequestContext requestContext) {
    return findStrategy(type).deleteTransaction(transaction, requestContext);
  }

}
