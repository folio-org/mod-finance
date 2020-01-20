package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.getEndpoint;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.TransactionSummariesHelper;
import org.folio.rest.jaxrs.model.OrderTransactionSummary;
import org.folio.rest.jaxrs.model.InvoiceTransactionSummary;
import org.folio.rest.jaxrs.resource.FinanceOrderTransactionSummaries;
import org.folio.rest.jaxrs.resource.FinanceInvoiceTransactionSummaries;

public class TransactionSummariesAPI implements FinanceOrderTransactionSummaries, FinanceInvoiceTransactionSummaries {

  private static final String ORDER_TRANSACTION_SUMMARIES_LOCATION_PREFIX = getEndpoint(FinanceOrderTransactionSummaries.class) + "/%s";
  private static final String INVOICE_TRANSACTION_SUMMARIES_LOCATION_PREFIX = getEndpoint(FinanceInvoiceTransactionSummaries.class) + "/%s";

  @Override
  @Validate
  public void postFinanceOrderTransactionSummaries(String lang, OrderTransactionSummary entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionSummariesHelper helper = new TransactionSummariesHelper(okapiHeaders, vertxContext, lang);
    helper.createOrderTransactionSummary(entity)
      .thenAccept(orderTxSummary -> asyncResultHandler.handle(succeededFuture(
          helper.buildResponseWithLocation(String.format(ORDER_TRANSACTION_SUMMARIES_LOCATION_PREFIX, orderTxSummary.getId()), orderTxSummary))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Override
  @Validate
  public void postFinanceInvoiceTransactionSummaries(String lang, InvoiceTransactionSummary entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionSummariesHelper helper = new TransactionSummariesHelper(okapiHeaders, vertxContext, lang);
    helper.createInvoiceTransactionSummary(entity)
      .thenAccept(invoiceTxSummary -> asyncResultHandler.handle(succeededFuture(helper
        .buildResponseWithLocation(String.format(INVOICE_TRANSACTION_SUMMARIES_LOCATION_PREFIX, invoiceTxSummary.getId()), invoiceTxSummary))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }
}

