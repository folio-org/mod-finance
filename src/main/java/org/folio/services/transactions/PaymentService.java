package org.folio.services.transactions;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.HelperUtils;

import org.folio.completablefuture.FolioVertxCompletableFuture;

import static org.folio.rest.util.ErrorCodes.UPDATE_PAYMENT_TO_CANCEL_INVOICE;

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
  public CompletableFuture<Void> updateTransaction(Transaction payment, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(), () -> {
        transactionService.validateTransactionType(payment, Transaction.TransactionType.PAYMENT);
        if (!Boolean.TRUE.equals(payment.getInvoiceCancelled()))
          throw new HttpException(422, UPDATE_PAYMENT_TO_CANCEL_INVOICE.toError());
      })
      .thenCompose(v -> transactionService.retrieveTransactionById(payment.getId(), requestContext))
      .thenAccept(existingTransaction -> {
        if (Boolean.TRUE.equals(existingTransaction.getInvoiceCancelled()))
          throw new HttpException(422, UPDATE_PAYMENT_TO_CANCEL_INVOICE.toError());
        // compare new transaction with existing one: ignore invoiceCancelled and metadata changes
        existingTransaction.setInvoiceCancelled(true);
        existingTransaction.setMetadata(payment.getMetadata());
        if (!existingTransaction.equals(payment))
          throw new HttpException(422, UPDATE_PAYMENT_TO_CANCEL_INVOICE.toError());
      })
      .thenCompose(v -> transactionService.updateTransaction(payment, requestContext));
  }

  @Override
  public CompletableFuture<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.PAYMENT;
  }

}
