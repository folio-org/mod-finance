package org.folio.services.transactions;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;

import org.folio.completablefuture.FolioVertxCompletableFuture;
import org.folio.rest.util.HelperUtils;

public class AllocationService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;
  private final TransactionRestrictService transactionRestrictService;

  public AllocationService(TransactionService transactionService, TransactionRestrictService transactionRestrictService) {
    this.transactionService = transactionService;
    this.transactionRestrictService = transactionRestrictService;
  }

  @Override
  public CompletableFuture<Transaction> createTransaction(Transaction allocation, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(allocation, Transaction.TransactionType.ALLOCATION))
      .thenCompose(aVoid -> transactionRestrictService.checkAllocation(allocation, requestContext))
      .thenCompose(transaction -> transactionService.createTransaction(transaction, requestContext));
  }

  @Override
  public CompletableFuture<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public CompletableFuture<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.ALLOCATION;
  }

}
