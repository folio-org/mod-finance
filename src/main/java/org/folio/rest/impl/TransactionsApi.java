package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.INVALID_TRANSACTION_TYPE;
import static org.folio.rest.util.HelperUtils.getEndpoint;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.TransactionsHelper;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.resource.Finance;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class TransactionsApi implements Finance {

  private static final String TRANSACTIONS_LOCATION_PREFIX = getEndpoint(Finance.class) + "/%s";

  @Validate
  @Override
  public void postFinanceAllocations(String lang, Transaction allocation, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);

    if (allocation.getTransactionType() != Transaction.TransactionType.ALLOCATION) {
      helper.addProcessingError(INVALID_TRANSACTION_TYPE.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }
    helper.createTransaction(allocation)
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Validate
  @Override
  public void postFinanceTransfers(String lang, Transaction transfer, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);

    if (transfer.getTransactionType() != Transaction.TransactionType.TRANSFER) {
      helper.addProcessingError(INVALID_TRANSACTION_TYPE.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }
    helper.createTransaction(transfer)
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Validate
  @Override
  public void postFinanceEncumbrances(String lang, Transaction encumbrance, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);

    if (encumbrance.getTransactionType() != Transaction.TransactionType.ENCUMBRANCE) {
      helper.addProcessingError(INVALID_TRANSACTION_TYPE.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }
    helper.createTransaction(encumbrance)
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceTransactions(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);
    helper.getTransactions(limit, offset, query)
      .thenAccept(types -> asyncResultHandler.handle(succeededFuture(helper.buildOkResponse(types))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceTransactionsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);
    helper.getTransaction(id)
      .thenAccept(type -> asyncResultHandler.handle(succeededFuture(helper.buildOkResponse(type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Validate
  @Override
  public void postFinancePayments(String lang, Transaction payment, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);

    if (payment.getTransactionType() != Transaction.TransactionType.PAYMENT) {
      helper.addProcessingError(INVALID_TRANSACTION_TYPE.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }
    helper.createTransaction(payment)
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Validate
  @Override
  public void postFinanceCredits(String lang, Transaction credit, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionsHelper helper = new TransactionsHelper(okapiHeaders, vertxContext, lang);

    if (credit.getTransactionType() != Transaction.TransactionType.CREDIT) {
      helper.addProcessingError(INVALID_TRANSACTION_TYPE.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }
    helper.createTransaction(credit)
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }
}
