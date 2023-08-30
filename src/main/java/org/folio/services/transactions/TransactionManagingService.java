package org.folio.services.transactions;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;

import io.vertx.core.Future;

public interface TransactionManagingService {
  Future<Transaction> createTransaction(Transaction transaction, RequestContext requestContext);
  Future<Void> updateTransaction(Transaction transaction, RequestContext requestContext);
  Future<Void> deleteTransaction(Transaction transaction, RequestContext requestContext);
}
