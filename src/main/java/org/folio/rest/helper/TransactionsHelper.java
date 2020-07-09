package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class TransactionsHelper extends AbstractHelper {

  private static final String GET_TRANSACTIONS_BY_QUERY = resourcesPath(TRANSACTIONS) + SEARCH_PARAMS;

  public TransactionsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Transaction> createTransaction(Transaction transaction) {
    TransactionRestrictHelper transactionRestrictHelper = new TransactionRestrictHelper(okapiHeaders,ctx, lang);
    return transactionRestrictHelper.checkRestrictions(transaction)
      .thenCompose(res -> handleCreateRequest(resourcesPath(TRANSACTIONS), res)
      .thenApply(res::withId));
  }

  public CompletableFuture<TransactionCollection> getTransactions(int limit, int offset, String query) {
    String endpoint = String.format(GET_TRANSACTIONS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(TransactionCollection.class)));
  }

  public CompletableFuture<Transaction> getTransaction(String id) {
    return handleGetRequest(resourceByIdPath(TRANSACTIONS, id, lang))
      .thenApply(json -> json.mapTo(Transaction.class));
  }

  public CompletableFuture<Void> updateTransaction(Transaction transaction) {
    return handleUpdateRequest(resourceByIdPath(TRANSACTIONS, transaction.getId(), lang), transaction);
  }

  private void validateTransactionType(Transaction transaction, Transaction.TransactionType type) {
    if (transaction.getTransactionType() != type) {
      logger.info("Transaction {} type mismatch. {} expected", transaction.getId(), type) ;
      throw new HttpException(400, String.format("Transaction type mismatch. %s expected", type));
    }
  }

  public CompletableFuture<Void> releaseTransaction(Transaction transaction) {
    logger.info("Start releasing transaction {}", transaction.getId()) ;

    validateTransactionType(transaction, Transaction.TransactionType.ENCUMBRANCE);

    if (transaction.getEncumbrance().getStatus() == Encumbrance.Status.RELEASED) {
      return CompletableFuture.completedFuture(null);
    }

    transaction.getEncumbrance().setStatus(Encumbrance.Status.RELEASED);
    return updateTransaction(transaction);
  }
}
