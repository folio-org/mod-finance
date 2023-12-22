package org.folio.services.transactions;

import static org.folio.rest.util.ErrorCodes.UPDATE_CREDIT_TO_CANCEL_INVOICE;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.HelperUtils;

import io.vertx.core.Future;

public class CreditService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;

  public CreditService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public Future<Transaction> createTransaction(Transaction credit, RequestContext requestContext) {
    return Future.succeededFuture().map(v-> {
        transactionService.validateTransactionType(credit, Transaction.TransactionType.CREDIT);
        HelperUtils.validateAmount(credit.getAmount(), "amount");
return null;
      })
      .compose(aVoid -> transactionService.createTransaction(credit, requestContext));
  }

  @Override
  public Future<Void> updateTransaction(Transaction credit, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(credit, Transaction.TransactionType.CREDIT);
        if (!Boolean.TRUE.equals(credit.getInvoiceCancelled())) {
          throw new HttpException(422, UPDATE_CREDIT_TO_CANCEL_INVOICE.toError());
        }
        return null;
      })
      .compose(v -> transactionService.retrieveTransactionById(credit.getId(), requestContext))
      .map(existingTransaction -> {
        if (Boolean.TRUE.equals(existingTransaction.getInvoiceCancelled()))
          throw new HttpException(422, UPDATE_CREDIT_TO_CANCEL_INVOICE.toError());
        // compare new transaction with existing one: ignore invoiceCancelled and metadata changes
        existingTransaction.setInvoiceCancelled(true);
        existingTransaction.setMetadata(credit.getMetadata());
        if (!existingTransaction.equals(credit)) {
          throw new HttpException(422, UPDATE_CREDIT_TO_CANCEL_INVOICE.toError());
        }
        return null;
      })
      .compose(v -> transactionService.updateTransaction(credit, requestContext));
  }

  @Override
  public Future<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return HelperUtils.unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.CREDIT;
  }

}
