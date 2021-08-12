package org.folio.services.transactions;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;

import java.util.concurrent.CompletableFuture;

public interface TransactionManagingService {
  CompletableFuture<Transaction> createTransaction(Transaction transaction, RequestContext requestContext);
  CompletableFuture<Void> updateTransaction(Transaction transaction, RequestContext requestContext);
  CompletableFuture<Void> deleteTransaction(Transaction transaction, RequestContext requestContext);
}
