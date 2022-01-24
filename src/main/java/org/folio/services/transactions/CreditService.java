package org.folio.services.transactions;

import static org.folio.rest.util.ErrorCodes.UPDATE_CREDIT_TO_CANCEL_INVOICE;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.HelperUtils;

import org.folio.completablefuture.FolioVertxCompletableFuture;

public class CreditService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;

  public CreditService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public CompletableFuture<Transaction> createTransaction(Transaction credit, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> {
        transactionService.validateTransactionType(credit, Transaction.TransactionType.CREDIT);
        HelperUtils.validateAmount(credit.getAmount(), "amount");
      })
      .thenCompose(aVoid -> transactionService.createTransaction(credit, requestContext));
  }

  @Override
  public CompletableFuture<Void> updateTransaction(Transaction credit, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(), () -> {
        transactionService.validateTransactionType(credit, Transaction.TransactionType.CREDIT);
        if (!Boolean.TRUE.equals(credit.getInvoiceCancelled()))
          throw new HttpException(422, UPDATE_CREDIT_TO_CANCEL_INVOICE.toError());
      })
      .thenCompose(v -> transactionService.retrieveTransactionById(credit.getId(), requestContext))
      .thenAccept(existingTransaction -> {
        // compare new transaction with existing one: ignore invoiceCancelled and metadata changes
        existingTransaction.setInvoiceCancelled(true);
        existingTransaction.setMetadata(credit.getMetadata());
        if (!existingTransaction.equals(credit))
          throw new HttpException(422, UPDATE_CREDIT_TO_CANCEL_INVOICE.toError());
      })
      .thenCompose(v -> transactionService.updateTransaction(credit, requestContext));
  }

  @Override
  public CompletableFuture<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.CREDIT;
  }

}
