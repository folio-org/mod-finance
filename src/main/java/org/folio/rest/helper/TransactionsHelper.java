package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.handleGetRequest;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.rest.util.MoneyUtils;

public class TransactionsHelper extends AbstractHelper {

  private static final String GET_TRANSACTIONS_BY_QUERY = resourcesPath(TRANSACTIONS) + SEARCH_PARAMS;

  public TransactionsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Transaction> createTransaction(Transaction transaction) {
    return handleCreateRequest(resourcesPath(TRANSACTIONS), transaction).thenApply(transaction::withId);
  }

  public CompletableFuture<TransactionCollection> getTransactions(int limit, int offset, String query) {
    String endpoint = String.format(GET_TRANSACTIONS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(TransactionCollection.class)));
  }

  public CompletableFuture<Transaction> getTransaction(String id) {
    return handleGetRequest(resourceByIdPath(TRANSACTIONS, id, lang), httpClient, ctx, okapiHeaders, logger)
      .thenApply(json -> json.mapTo(Transaction.class));
  }

  public CompletableFuture<Void> updateTransaction(Transaction transaction) {
    return handleUpdateRequest(resourceByIdPath(TRANSACTIONS, transaction.getId(), lang), transaction);
  }

  /**
   * Get the {@link Transaction} (encumbrance) from storage and update the encumbered / awaiting payment amounts
   *
   * @param awaitingPayment {@link AwaitingPayment} object
   * @return {@link CompletableFuture<Void>} returns empty result
   */
  public CompletableFuture<Void> moveToAwaitingPayment(AwaitingPayment awaitingPayment) {
    return getTransaction(awaitingPayment.getEncumbranceId())
      .thenApply(tr -> modifyTransaction(tr, awaitingPayment))
      .thenCompose(this::updateTransaction);
  }

  private Transaction modifyTransaction(Transaction transaction, AwaitingPayment awaitingPayment) {
    Double currentAwaitingPaymentAmount = transaction.getEncumbrance().getAmountAwaitingPayment();
    String currency = transaction.getCurrency();

    transaction.getEncumbrance()
      .setAmountAwaitingPayment(MoneyUtils.sumDoubleValues(currentAwaitingPaymentAmount, awaitingPayment.getAmountAwaitingPayment(), currency));

    transaction.getEncumbrance().setStatus(awaitingPayment.getReleaseEncumbrance() ? Encumbrance.Status.RELEASED : Encumbrance.Status.UNRELEASED);
    return transaction;
  }

}
