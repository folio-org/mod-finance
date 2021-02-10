package org.folio.services.transactions;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;

import org.folio.completablefuture.FolioVertxCompletableFuture;

public class PendingPaymentService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;

  public PendingPaymentService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public CompletableFuture<Transaction> createTransaction(Transaction pendingPayment, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(pendingPayment, Transaction.TransactionType.PENDING_PAYMENT))
      .thenCompose(aVoid -> transactionService.createTransaction(pendingPayment, requestContext));
  }

  @Override
  public CompletableFuture<Void> updateTransaction(Transaction pendingPayment, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(pendingPayment, Transaction.TransactionType.PENDING_PAYMENT))
      .thenCompose(aVoid -> transactionService.updateTransaction(pendingPayment, requestContext));
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.PENDING_PAYMENT;
  }
}
