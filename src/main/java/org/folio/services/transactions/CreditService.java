package org.folio.services.transactions;

import static org.folio.rest.util.HelperUtils.unsupportedOperationExceptionFuture;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.HelperUtils;

import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class CreditService implements TransactionTypeManagingStrategy {

  private final TransactionService transactionService;

  public CreditService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public CompletableFuture<Transaction> createTransaction(Transaction credit, RequestContext requestContext) {
    return VertxCompletableFuture.runAsync(requestContext.getContext(),
      () -> {
        transactionService.validateTransactionType(credit, Transaction.TransactionType.CREDIT);
        HelperUtils.validateAmount(credit.getAmount(), "amount");
      })
      .thenCompose(aVoid -> transactionService.createTransaction(credit, requestContext));
  }

  @Override
  public CompletableFuture<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    return unsupportedOperationExceptionFuture();
  }

  @Override
  public Transaction.TransactionType getStrategyName() {
    return Transaction.TransactionType.CREDIT;
  }

}
