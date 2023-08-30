package org.folio.services.transactions;

import io.vertx.core.Future;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;

import org.folio.rest.util.HelperUtils;

public class PendingPaymentService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;

  public PendingPaymentService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public Future<Transaction> createTransaction(Transaction pendingPayment, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(pendingPayment, Transaction.TransactionType.PENDING_PAYMENT);
        return null;
      })
      .compose(aVoid -> transactionService.createTransaction(pendingPayment, requestContext));
  }

  @Override
  public Future<Void> updateTransaction(Transaction pendingPayment, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(pendingPayment, Transaction.TransactionType.PENDING_PAYMENT);
        return null;
      })
      .compose(aVoid -> transactionService.updateTransaction(pendingPayment, requestContext));
  }

  @Override
  public Future<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.PENDING_PAYMENT;
  }
}
