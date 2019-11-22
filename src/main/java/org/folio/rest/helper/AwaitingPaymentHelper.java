package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

import io.vertx.core.Context;

public class AwaitingPaymentHelper extends AbstractHelper {

  private static final String GET_TRANSACTIONS_BY_QUERY = resourcesPath(TRANSACTIONS) + SEARCH_PARAMS;

  public AwaitingPaymentHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  /**
   * Get the {@link Transaction} (encumbrance) from storage Update the encumbered / awaiting payment amounts, Call PUT in the
   * storage module
   *
   * @param awaitingPayment {@link AwaitingPayment} object
   * @return {@link AwaitingPayment} with PO line id as a key and value is map with piece id as a
   */
  public CompletableFuture<Void> moveToAwaitingPayment(AwaitingPayment awaitingPayment) {
    String query = "paymentEncumbranceId==" + awaitingPayment.getEncumbranceId();

    TransactionsHelper transactionsHelper = new TransactionsHelper(okapiHeaders, ctx, lang);

    return transactionsHelper.getTransactions(1, 0, query)
      .thenApply(tr -> modifyTransaction(tr.getTransactions().get(0)))
      .thenCompose(transactionsHelper::updateTransaction);
  }

  private Transaction modifyTransaction(Transaction transaction) {
    transaction.getEncumbrance().set()
      return transaction;
  }

}
