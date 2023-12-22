package org.folio.services.transactions;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Future;

public interface TransactionService extends TransactionManagingService {

  Future<TransactionCollection> retrieveTransactions(String query, int offset, int limit, RequestContext requestContext);
  Future<Transaction> retrieveTransactionById(String id, RequestContext requestContext);
  void validateTransactionType(Transaction transaction, Transaction.TransactionType transactionType);
  Future<Boolean> isConnectedToInvoice(String transactionId, RequestContext requestContext);
}
