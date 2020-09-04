package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.TransactionType;
import org.folio.rest.jaxrs.resource.Finance;
import org.folio.services.transactions.TransactionService;
import org.folio.services.transactions.TransactionStrategyFactory;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class TransactionsApi extends BaseApi implements Finance {

  private static final String TRANSACTIONS_LOCATION_PREFIX = getEndpoint(Finance.class) + "/%s";

  @Autowired
  private TransactionStrategyFactory transactionStrategyFactory;
  @Autowired
  private TransactionService transactionService;

  public TransactionsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postFinanceAllocations(String lang, Transaction allocation, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.ALLOCATION, allocation, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceTransfers(String lang, Transaction transfer, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.TRANSFER, transfer, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceEncumbrances(String lang, Transaction encumbrance, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.ENCUMBRANCE, encumbrance, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void putFinanceEncumbrancesById(String id, String lang, Transaction encumbrance, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (isEmpty(encumbrance.getId())) {
      encumbrance.setId(id);
    }
    if (id.equals(encumbrance.getId())) {

      transactionStrategyFactory.updateTransaction(Transaction.TransactionType.ENCUMBRANCE, encumbrance, new RequestContext(vertxContext, okapiHeaders))
        .thenAccept(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
        .exceptionally(fail -> handleErrorResponse(asyncResultHandler,  fail));
    } else {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
    }
  }

  @Validate
  @Override
  public void getFinanceTransactions(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionService.retrieveTransactions(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(types -> asyncResultHandler.handle(succeededFuture(buildOkResponse(types))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void getFinanceTransactionsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionService.retrieveTransactionById(id, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(type -> asyncResultHandler.handle(succeededFuture(buildOkResponse(type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinancePayments(String lang, Transaction payment, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.PAYMENT, payment, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinancePendingPayments(String lang, Transaction pendingPayment, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.PENDING_PAYMENT, pendingPayment, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(transaction -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, transaction.getId()), transaction))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  public void putFinancePendingPaymentsById(String id, String lang, Transaction pendingPayment,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (isEmpty(pendingPayment.getId())) {
      pendingPayment.setId(id);
    } else if (!id.equals(pendingPayment.getId())) {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
      return;
    }
    transactionStrategyFactory.updateTransaction(
      TransactionType.PENDING_PAYMENT, pendingPayment, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceCredits(String lang, Transaction credit, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.CREDIT, credit, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
