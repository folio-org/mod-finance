package org.folio.rest.helper;

import static org.folio.rest.util.ResourcePathResolver.INVOICE_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.ResourcePathResolver.ORDER_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletionException;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.InvoiceTransactionSummary;
import org.folio.rest.jaxrs.model.OrderTransactionSummary;
import org.folio.rest.util.ErrorCodes;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class TransactionSummariesHelper extends AbstractHelper {
  @Autowired
  private RestClient restClient;

  public TransactionSummariesHelper(Map<String, String> okapiHeaders, Context ctx) {
    super(okapiHeaders, ctx);
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  public Future<OrderTransactionSummary> createOrderTransactionSummary(OrderTransactionSummary orderSummary, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        validateOrderTransactionCount(orderSummary.getNumTransactions());
        return null;
      })
      .compose(ok -> restClient.post(resourcesPath(ORDER_TRANSACTION_SUMMARIES), orderSummary, OrderTransactionSummary.class, requestContext));
  }

  public Future<InvoiceTransactionSummary> createInvoiceTransactionSummary(InvoiceTransactionSummary invoiceSummary, RequestContext requestContext) {
    return Future.succeededFuture().map(v -> {
      validateInvoiceTransactionCount(invoiceSummary.getNumPaymentsCredits(), invoiceSummary.getNumPendingPayments());
      return null;
      })
      .compose(ok -> restClient.post(resourcesPath(INVOICE_TRANSACTION_SUMMARIES), invoiceSummary, InvoiceTransactionSummary.class, requestContext))
      .map(invoiceTransactionSummary -> invoiceTransactionSummary);
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

  public Future<Void> updateOrderTransactionSummary(OrderTransactionSummary orderSummary, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        validateOrderTransactionCount(orderSummary.getNumTransactions());
        return null;
      })
      .compose(v -> restClient.put(resourceByIdPath(ORDER_TRANSACTION_SUMMARIES, orderSummary.getId()), orderSummary, requestContext));
  }

  public Future<Void> updateInvoiceTransactionSummary(InvoiceTransactionSummary invoiceSummary, RequestContext requestContext) {
    return Future.succeededFuture().map(v-> {

       validateInvoiceTransactionCount(invoiceSummary.getNumPaymentsCredits(), invoiceSummary.getNumPendingPayments());
    return null;
    })
      .compose(ok -> restClient.put(resourceByIdPath(INVOICE_TRANSACTION_SUMMARIES, invoiceSummary.getId()), invoiceSummary, requestContext));
  }
}
