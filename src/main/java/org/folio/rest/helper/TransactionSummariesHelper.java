package org.folio.rest.helper;

import static org.folio.rest.util.ResourcePathResolver.ORDER_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.ResourcePathResolver.INVOICE_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
    return VertxCompletableFuture.runAsync(ctx, () -> validateTransactionCount(orderSummary.getNumTransactions()))
      .thenCompose(ok -> handleCreateRequest(resourcesPath(ORDER_TRANSACTION_SUMMARIES), orderSummary))
      .thenApply(orderSummary::withId);
  }

  public CompletableFuture<InvoiceTransactionSummary> createInvoiceTransactionSummary(InvoiceTransactionSummary invoiceSummary) {
    return VertxCompletableFuture.runAsync(ctx, () -> {
      validateTransactionCount(invoiceSummary.getNumPaymentsCredits());
      validateTransactionCount(invoiceSummary.getNumEncumbrances());
    })
      .thenCompose(ok -> handleCreateRequest(resourcesPath(INVOICE_TRANSACTION_SUMMARIES), invoiceSummary))
      .thenApply(invoiceSummary::withId);
  }

  private void validateTransactionCount(Integer summaryCounts) {
    if (summaryCounts < 1) {
      throw new CompletionException(new HttpException(422, ErrorCodes.INVALID_TRANSACTION_COUNT));
    }
  }
}
