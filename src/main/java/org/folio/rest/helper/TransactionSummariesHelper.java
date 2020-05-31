package org.folio.rest.helper;

import static org.folio.rest.util.ResourcePathResolver.ORDER_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.ResourcePathResolver.INVOICE_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.InvoiceTransactionSummary;
import org.folio.rest.jaxrs.model.OrderTransactionSummary;
import org.folio.rest.util.ErrorCodes;

public class TransactionSummariesHelper extends AbstractHelper {

  public TransactionSummariesHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<OrderTransactionSummary> createOrderTransactionSummary(OrderTransactionSummary orderSummary) {
    return VertxCompletableFuture.runAsync(ctx, () -> validateOrderTransactionCount(orderSummary.getNumTransactions()))
      .thenCompose(ok -> handleCreateRequest(resourcesPath(ORDER_TRANSACTION_SUMMARIES), orderSummary))
      .thenApply(orderSummary::withId);
  }

  public CompletableFuture<InvoiceTransactionSummary> createInvoiceTransactionSummary(InvoiceTransactionSummary invoiceSummary) {
    return VertxCompletableFuture
      .runAsync(ctx,
          () -> validateInvoiceTransactionCount(invoiceSummary.getNumPaymentsCredits(), invoiceSummary.getNumPendingPayments()))
      .thenCompose(ok -> handleCreateRequest(resourcesPath(INVOICE_TRANSACTION_SUMMARIES), invoiceSummary))
      .thenApply(invoiceSummary::withId);
  }

  /**
   * There has to be atleast 1 transaction that needs to be updated upon invoice approval otherwise throw an exception
   */
  private void validateOrderTransactionCount(Integer numTransactions) {
    if (numTransactions <= 0) {
      throw new CompletionException(new HttpException(422, ErrorCodes.INVALID_ORDER_TRANSACTION_COUNT));
    }
  }

  /**
   * There has to be atleast 1 payment/credits, otherwise no point in having an invoice. Also, it is possible that there are no
   * encumbrances related to an invoice upon approval. If not, throw an exception
   */
  private void validateInvoiceTransactionCount(Integer numPaymentsCredits, Integer numEncumbrances) {
    if (numPaymentsCredits <= 0 || numEncumbrances < 0) {
      throw new CompletionException(new HttpException(422, ErrorCodes.INVALID_INVOICE_TRANSACTION_COUNT));
    }
  }

  public CompletableFuture<Void> updateOrderTransactionSummary(OrderTransactionSummary orderSummary) {
    return VertxCompletableFuture.runAsync(ctx, () -> validateOrderTransactionCount(orderSummary.getNumTransactions()))
      .thenCompose(ok -> handleUpdateRequest(resourceByIdPath(ORDER_TRANSACTION_SUMMARIES, orderSummary.getId(), lang), orderSummary));
  }
}
