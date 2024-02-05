package org.folio.services.transactions;

import static org.folio.rest.util.ErrorCodes.UPDATE_CREDIT_TO_CANCEL_INVOICE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.HelperUtils;

import io.vertx.core.Future;

public class CreditService implements TransactionTypeManagingStrategy {

  private static final Logger log = LogManager.getLogger();
  private final TransactionService transactionService;

  public CreditService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public Future<Transaction> createTransaction(Transaction credit, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v-> {
        transactionService.validateTransactionType(credit, Transaction.TransactionType.CREDIT);
        HelperUtils.validateAmount(credit.getAmount(), "amount");
        return null;
    })
    .compose(aVoid -> transactionService.createTransaction(credit, requestContext));
  }

  @Override
  public Future<Void> updateTransaction(Transaction credit, RequestContext requestContext) {
    log.debug("updateTransaction:: Updating transaction '{}'", credit.getId());
    return Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(credit, Transaction.TransactionType.CREDIT);
        if (!Boolean.TRUE.equals(credit.getInvoiceCancelled())) {
          log.error("updateTransaction:: Credit '{}' invoice is not cancelled", credit.getId());
          throw new HttpException(422, UPDATE_CREDIT_TO_CANCEL_INVOICE.toError());
        }
        return null;
      })
      .compose(v -> transactionService.retrieveTransactionById(credit.getId(), requestContext))
      .map(existingTransaction -> {
        if (Boolean.TRUE.equals(existingTransaction.getInvoiceCancelled())) {
          log.error("updateTransaction:: Existing transaction '{}' already has invoiceCancelled flag set to true", existingTransaction.getId());
          throw new HttpException(422, UPDATE_CREDIT_TO_CANCEL_INVOICE.toError());
        }
        // compare new transaction with existing one: ignore invoiceCancelled and metadata changes
        existingTransaction.setInvoiceCancelled(true);
        existingTransaction.setMetadata(credit.getMetadata());
        if (!existingTransaction.equals(credit)) {
          log.error("updateTransaction:: Existing transaction '{}' is equal to credit '{}'", existingTransaction.getId(), credit.getId());
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
