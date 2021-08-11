package org.folio.services.transactions;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Transaction;

import org.folio.completablefuture.FolioVertxCompletableFuture;

import static org.folio.rest.util.ErrorCodes.DELETE_WITH_EXPENDED_AMOUNT;
import static org.folio.rest.util.ErrorCodes.DELETE_CONNECTED_TO_INVOICE;

public class EncumbranceService implements TransactionTypeManagingStrategy {

  private static final Logger logger = LogManager.getLogger(EncumbranceService.class);
  private final TransactionService transactionService;
  private final CommonTransactionService commonTransactionService;

  public EncumbranceService(TransactionService transactionService, CommonTransactionService commonTransactionService) {
    this.transactionService = transactionService;
    this.commonTransactionService = commonTransactionService;
  }

  @Override
  public CompletableFuture<Transaction> createTransaction(Transaction encumbrance, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE))
      .thenCompose(aVoid -> transactionService.createTransaction(encumbrance, requestContext));
  }

  @Override
  public CompletableFuture<Void> updateTransaction(Transaction encumbrance, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE))
      .thenCompose(aVoid -> transactionService.updateTransaction(encumbrance, requestContext));
  }

  @Override
  public CompletableFuture<Void> deleteTransaction(Transaction encumbrance, RequestContext requestContext) {
    return FolioVertxCompletableFuture.runAsync(requestContext.getContext(),
        () -> transactionService.validateTransactionType(encumbrance, Transaction.TransactionType.ENCUMBRANCE))
      .thenCompose(aVoid -> validateDeletion(encumbrance, requestContext))
      .thenCompose(aVoid -> commonTransactionService.releaseTransaction(encumbrance, requestContext))
      .thenCompose(aVoid -> transactionService.deleteTransaction(encumbrance, requestContext));
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.ENCUMBRANCE;
  }

  private CompletableFuture<Void> validateDeletion(Transaction encumbrance, RequestContext requestContext) {
    if (encumbrance.getEncumbrance() != null && encumbrance.getEncumbrance().getAmountExpended() > 0) {
      logger.info("Tried to delete transaction {} but it has an expended amount.", encumbrance.getId()) ;
      Parameter parameter = new Parameter().withKey("id").withValue(encumbrance.getId());
      throw new HttpException(422, DELETE_WITH_EXPENDED_AMOUNT.toError().withParameters(Collections.singletonList(parameter)));
    }
    return transactionService.isConnectedToInvoice(encumbrance.getId(), requestContext)
      .thenAccept(connected -> {
        if (connected) {
          logger.info("Tried to delete transaction {} but it is connected to an invoice.", encumbrance.getId());
          Parameter parameter = new Parameter().withKey("id").withValue(encumbrance.getId());
          throw new HttpException(422, DELETE_CONNECTED_TO_INVOICE.toError().withParameters(Collections.singletonList(parameter)));
        }
      });
  }
}
