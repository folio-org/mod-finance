package org.folio.services.transactions;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import java.util.concurrent.CompletableFuture;

public interface TransactionService extends TransactionManagingService {

  CompletableFuture<TransactionCollection> retrieveTransactions(String query, int offset, int limit, RequestContext requestContext);
  CompletableFuture<Transaction> retrieveTransactionById(String id, RequestContext requestContext);
  void validateTransactionType(Transaction transaction, Transaction.TransactionType transactionType);
}
