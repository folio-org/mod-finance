package org.folio.rest.helper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class TransactionsHelper extends AbstractHelper {

  @Autowired
  public RestClient transactionRestClient;

  public TransactionsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  public CompletableFuture<Transaction> createTransaction(Transaction transaction) {
    TransactionRestrictHelper transactionRestrictHelper = new TransactionRestrictHelper(okapiHeaders,ctx, lang);
    return transactionRestrictHelper.checkRestrictions(transaction)
      .thenCompose(res -> transactionRestClient.post(res, new RequestContext(ctx, okapiHeaders), Transaction.class));
  }

  public CompletableFuture<TransactionCollection> getTransactions(int limit, int offset, String query) {
    return transactionRestClient.get(query, offset, limit, new RequestContext(ctx, okapiHeaders), TransactionCollection.class);
  }

  public CompletableFuture<Transaction> getTransaction(String id) {
    return transactionRestClient.getById(id, new RequestContext(ctx, okapiHeaders), Transaction.class);
  }

  public CompletableFuture<Void> updateTransaction(Transaction transaction) {
    return transactionRestClient.put(transaction.getId(), transaction, new RequestContext(ctx, okapiHeaders));
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
