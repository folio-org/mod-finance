package org.folio.services.transactions;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.HelperUtils;

import io.vertx.core.Future;

public class AllocationService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;
  private final TransactionRestrictService transactionRestrictService;

  public AllocationService(TransactionService transactionService, TransactionRestrictService transactionRestrictService) {
    this.transactionService = transactionService;
    this.transactionRestrictService = transactionRestrictService;
  }

  @Override
  public Future<Transaction> createTransaction(Transaction allocation, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(allocation, Transaction.TransactionType.ALLOCATION);
        return null;
      })
      .compose(aVoid -> transactionRestrictService.checkAllocation(allocation, requestContext))
      .compose(v -> transactionService.createTransaction(allocation, requestContext));
  }

  @Override
  public Future<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public Future<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.ALLOCATION;
  }

}
