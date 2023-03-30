package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.TransactionSummariesHelper;
import org.folio.rest.jaxrs.model.InvoiceTransactionSummary;
import org.folio.rest.jaxrs.model.OrderTransactionSummary;
import org.folio.rest.jaxrs.resource.FinanceInvoiceTransactionSummaries;
import org.folio.rest.jaxrs.resource.FinanceOrderTransactionSummaries;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class TransactionSummariesAPI implements FinanceOrderTransactionSummaries, FinanceInvoiceTransactionSummaries {

  private static final String ORDER_TRANSACTION_SUMMARIES_LOCATION_PREFIX = getEndpoint(FinanceOrderTransactionSummaries.class) + "/%s";
  private static final String INVOICE_TRANSACTION_SUMMARIES_LOCATION_PREFIX = getEndpoint(FinanceInvoiceTransactionSummaries.class) + "/%s";

  @Override
  @Validate
  public void postFinanceOrderTransactionSummaries(OrderTransactionSummary entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionSummariesHelper helper = new TransactionSummariesHelper(okapiHeaders, vertxContext);
    helper.createOrderTransactionSummary(entity)
      .thenAccept(orderTxSummary -> asyncResultHandler.handle(succeededFuture(
          helper.buildResponseWithLocation(String.format(ORDER_TRANSACTION_SUMMARIES_LOCATION_PREFIX, orderTxSummary.getId()), orderTxSummary))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Override
  @Validate
  public void putFinanceOrderTransactionSummariesById(String id, OrderTransactionSummary entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionSummariesHelper helper = new TransactionSummariesHelper(okapiHeaders, vertxContext);

    // Set id if this is available only in path
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateOrderTransactionSummary(entity)
      .thenAccept(types -> asyncResultHandler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Override
  @Validate
  public void postFinanceInvoiceTransactionSummaries(InvoiceTransactionSummary entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionSummariesHelper helper = new TransactionSummariesHelper(okapiHeaders, vertxContext);
    helper.createInvoiceTransactionSummary(entity)
      .thenAccept(invoiceTxSummary -> asyncResultHandler.handle(succeededFuture(helper
        .buildResponseWithLocation(String.format(INVOICE_TRANSACTION_SUMMARIES_LOCATION_PREFIX, invoiceTxSummary.getId()), invoiceTxSummary))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Override
  @Validate
  public void putFinanceInvoiceTransactionSummariesById(String id, InvoiceTransactionSummary entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionSummariesHelper helper = new TransactionSummariesHelper(okapiHeaders, vertxContext);

    // Set id if this is available only in path
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateInvoiceTransactionSummary(entity)
      .thenAccept(types -> asyncResultHandler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

}

