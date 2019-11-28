package org.folio.rest.helper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.MoneyUtils;

import io.vertx.core.Context;

public class AwaitingPaymentHelper extends AbstractHelper {

  public AwaitingPaymentHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  /**
   * Get the {@link Transaction} (encumbrance) from storage and update the encumbered / awaiting payment amounts
   *
   * @param awaitingPayment {@link AwaitingPayment} object
   * @return {@link CompletableFuture<Void>} returns empty result
   */
  public CompletableFuture<Void> moveToAwaitingPayment(AwaitingPayment awaitingPayment) {
    TransactionsHelper transactionsHelper = new TransactionsHelper(okapiHeaders, ctx, lang);

    return transactionsHelper.getTransaction(awaitingPayment.getEncumbranceId())
      .thenApply(tr -> modifyTransaction(tr, awaitingPayment))
      .thenCompose(transactionsHelper::updateTransaction);
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
