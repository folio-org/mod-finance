package org.folio.services.transactions;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;

import org.folio.completablefuture.FolioVertxCompletableFuture;

public class EncumbranceService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;

  public EncumbranceService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public CompletableFuture<Transaction> createTransaction(Transaction encumbrance, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE))
      .thenCompose(aVoid -> transactionService.createTransaction(encumbrance, requestContext));
  }

  @Override
  public CompletableFuture<Void> updateTransaction(Transaction encumbrance, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE))
      .thenCompose(aVoid -> transactionService.updateTransaction(encumbrance, requestContext));
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.ENCUMBRANCE;
  }

}
