package org.folio.services.transactions;

import static org.folio.rest.util.ErrorCodes.DELETE_CONNECTED_TO_INVOICE;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_NOT_RELEASED;

import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Transaction;

import io.vertx.core.Future;

public class EncumbranceService implements TransactionTypeManagingStrategy {

  private static final Logger logger = LogManager.getLogger(EncumbranceService.class);
  private final TransactionService transactionService;

  public EncumbranceService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public Future<Transaction> createTransaction(Transaction encumbrance, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE);
        return null;
      })
      .compose(aVoid -> transactionService.createTransaction(encumbrance, requestContext));
  }

  @Override
  public Future<Void> updateTransaction(Transaction encumbrance, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE);
        return null;
      })
      .compose(aVoid -> transactionService.updateTransaction(encumbrance, requestContext));
  }

  @Override
  public Future<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE);
        return null;
      })
      .compose(aVoid -> validateDeletion(encumbrance, requestContext))
      .compose(aVoid -> transactionService.deleteTransaction(encumbrance, requestContext));
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.ENCUMBRANCE;
  }

  private Future<Void> validateDeletion(Transaction encumbrance, RequestContext requestContext) {

    return Future.succeededFuture()
      .map(v -> {
        checkEncumbranceStatusNotReleased(encumbrance);
        return null;
      })
      .compose(v-> transactionService.isConnectedToInvoice(encumbrance.getId(), requestContext))
      .map(connected -> {
        if (connected) {
          logger.info("Tried to delete transaction {} but it is connected to an invoice.", encumbrance.getId());
          Parameter parameter = new Parameter().withKey("id").withValue(encumbrance.getId());
          throw new HttpException(422, DELETE_CONNECTED_TO_INVOICE.toError().withParameters(Collections.singletonList(parameter)));
        }
        return null;
      });
  }

  private void checkEncumbranceStatusNotReleased(Transaction encumbrance) {
    if (encumbrance.getEncumbrance().getStatus() != Encumbrance.Status.RELEASED) {
      logger.info("Transaction {} should be released before deletion", encumbrance.getId());
      Parameter parameter = new Parameter().withKey("id").withValue(encumbrance.getId());
      throw new HttpException(400, TRANSACTION_NOT_RELEASED.toError().withParameters(Collections.singletonList(parameter)));

    }
  }
}
