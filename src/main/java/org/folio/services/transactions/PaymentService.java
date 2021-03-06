package org.folio.services.transactions;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.HelperUtils;

import org.folio.completablefuture.FolioVertxCompletableFuture;

import static org.folio.rest.util.HelperUtils.unsupportedOperationExceptionFuture;

public class PaymentService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;

  public PaymentService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public CompletableFuture<Transaction> createTransaction(Transaction payment, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> {
        transactionService.validateTransactionType(payment, Transaction.TransactionType.PAYMENT);
        HelperUtils.validateAmount(payment.getAmount(), "amount");
      })
      .thenCompose(aVoid -> transactionService.createTransaction(payment, requestContext));
  }

  @Override
  public CompletableFuture<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    return unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.PAYMENT;
  }

}
