package org.folio.services.transactions;

import static org.folio.rest.util.ErrorCodes.DELETE_CONNECTED_TO_INVOICE;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_NOT_RELEASED;

import java.util.Collections;
import io.vertx.core.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Transaction;

public class EncumbranceService implements TransactionTypeManagingStrategy {

  private static final Logger logger = LogManager.getLogger(EncumbranceService.class);
  private final TransactionService transactionService;
  private final CommonTransactionService commonTransactionService;

  public EncumbranceService(TransactionService transactionService, CommonTransactionService commonTransactionService) {
    this.transactionService = transactionService;
    this.commonTransactionService = commonTransactionService;
  }

  @Override
  public Future<Transaction> createTransaction(Transaction encumbrance, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE))
      .thenCompose(aVoid -> transactionService.createTransaction(encumbrance, requestContext));
  }

  @Override
  public Future<Void> updateTransaction(Transaction encumbrance, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE))
      .thenCompose(aVoid -> transactionService.updateTransaction(encumbrance, requestContext));
  }

  @Override
  public Future<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
        () -> transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE))
      .thenCompose(aVoid -> validateDeletion(encumbrance, requestContext))
      .thenCompose(aVoid -> transactionService.deleteTransaction(encumbrance, requestContext));
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.ENCUMBRANCE;
  }

  private Future<Void> validateDeletion(Transaction encumbrance, RequestContext requestContext) {
    checkEncumbranceStatusNotReleased(encumbrance);

    return transactionService.isConnectedToInvoice(encumbrance.getId(), requestContext)
      .thenAccept(connected -> {
        if (connected) {
          logger.info("Tried to delete transaction {} but it is connected to an invoice.", encumbrance.getId());
          Parameter parameter = new Parameter().withKey("id").withValue(encumbrance.getId());
          throw new HttpException(422, DELETE_CONNECTED_TO_INVOICE.toError().withParameters(Collections.singletonList(parameter)));
        }
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
