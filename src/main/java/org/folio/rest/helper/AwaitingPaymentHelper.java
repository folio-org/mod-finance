package org.folio.rest.helper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AwaitingPayment;
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
      .thenApply(tr -> modifyTransaction(tr, awaitingPayment.getAmountAwaitingPayment()))
      .thenCompose(transactionsHelper::updateTransaction);
  }

  private Transaction modifyTransaction(Transaction transaction, Double amountAwaitingPayment) {
    Double currentAwaitingPaymentAmount = transaction.getEncumbrance().getAmountAwaitingPayment();
    Double currentAmountExpended = transaction.getEncumbrance().getAmountExpended();
    String currency = transaction.getCurrency();

    transaction.getEncumbrance()
      .setAmountAwaitingPayment(MoneyUtils.sumDoubleValues(currentAwaitingPaymentAmount, amountAwaitingPayment, currency));
    transaction.getEncumbrance()
      .setAmountExpended(MoneyUtils.subtractDoubleValues(currentAmountExpended, amountAwaitingPayment, currency));
    return transaction;
  }
}
