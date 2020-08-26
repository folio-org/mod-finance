package org.folio.services.transactions;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;

import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.rest.util.HelperUtils;

public class TransferService implements TransactionTypeManagingStrategy {

  private final TransactionRestrictService transactionRestrictService;
  private final TransactionService transactionService;

  public TransferService(BaseTransactionService transactionService, TransactionRestrictService transactionRestrictService) {
    this.transactionService = transactionService;
    this.transactionRestrictService = transactionRestrictService;
  }

  @Override
  public CompletableFuture<Transaction> createTransaction(Transaction transfer, RequestContext requestContext) {
    return  VertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(transfer, Transaction.TransactionType.TRANSFER))
      .thenCompose(aVoid ->  transactionRestrictService.checkTransfer(transfer, requestContext))
      .thenCompose(transaction -> transactionService.createTransaction(transaction, requestContext));
  }

  @Override
  public CompletableFuture<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.TRANSFER;
  }
}
