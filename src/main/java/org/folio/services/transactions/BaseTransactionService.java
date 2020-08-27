package org.folio.services.transactions;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import static org.folio.rest.util.ErrorCodes.INVALID_TRANSACTION_TYPE;

public class BaseTransactionService implements TransactionService {

  private static final Logger logger = LoggerFactory.getLogger(BaseTransactionService.class);

  private final RestClient transactionRestClient;

  public BaseTransactionService(RestClient transactionRestClient) {
    this.transactionRestClient = transactionRestClient;
  }

  @Override
  public CompletableFuture<TransactionCollection> retrieveTransactions(String query, int offset, int limit, RequestContext requestContext) {
    return transactionRestClient.get(query, offset, limit, requestContext, TransactionCollection.class);
  }

  @Override
  public CompletableFuture<Transaction> retrieveTransactionById(String id, RequestContext requestContext) {
    return transactionRestClient.getById(id, requestContext, Transaction.class);
  }

  public CompletableFuture<Transaction> createTransaction(Transaction transaction, RequestContext requestContext) {
    return transactionRestClient.post(transaction, requestContext, Transaction.class);
  }

  public CompletableFuture<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    return transactionRestClient.put(transaction.getId(), transaction, requestContext);
  }

  public void validateTransactionType(Transaction transaction, Transaction.TransactionType transactionType) {
    if (transaction.getTransactionType() != transactionType) {
      logger.info("Transaction {} type mismatch. {} expected", transaction.getId(), transactionType) ;
      Parameter parameter = new Parameter().withKey("expected").withValue(transactionType.name());
      throw new HttpException(422, INVALID_TRANSACTION_TYPE.toError().withParameters(Collections.singletonList(parameter)));
    }
  }

}
