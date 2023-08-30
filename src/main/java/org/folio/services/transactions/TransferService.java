package org.folio.services.transactions;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.HelperUtils;

import io.vertx.core.Future;

public class TransferService implements TransactionTypeManagingStrategy {

  private final TransactionRestrictService transactionRestrictService;
  private final TransactionService transactionService;

  public TransferService(BaseTransactionService transactionService, TransactionRestrictService transactionRestrictService) {
    this.transactionService = transactionService;
    this.transactionRestrictService = transactionRestrictService;
  }

  @Override
  public Future<Transaction> createTransaction(Transaction transfer, RequestContext requestContext) {
    return  Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(transfer, Transaction.TransactionType.TRANSFER);
        return null;
      })
      .compose(v ->  transactionRestrictService.checkTransfer(transfer, requestContext))
      .compose(v -> transactionService.createTransaction(transfer, requestContext));
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
    return Transaction.TransactionType.TRANSFER;
  }
}
