package org.folio.rest.helper;

import static org.folio.rest.util.ResourcePathResolver.ORDER_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Context;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.jaxrs.model.OrderTransactionSummary;

public class TransactionSummariesHelper extends AbstractHelper {

  public TransactionSummariesHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<OrderTransactionSummary> createOrderTransactionSummary(OrderTransactionSummary orderSummary) {
    return handleCreateRequest(resourcesPath(ORDER_TRANSACTION_SUMMARIES), orderSummary).thenApply(orderSummary::withId);
  }
}
