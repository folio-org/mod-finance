package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.TransactionType;
import org.folio.rest.jaxrs.resource.Finance;
import org.folio.services.transactions.BatchTransactionService;
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
  @Autowired
  private BatchTransactionService batchTransactionService;

  public TransactionsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postFinanceAllocations(Transaction allocation, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.ALLOCATION, allocation, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(type -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceTransfers(Transaction transfer, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.TRANSFER, transfer, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceEncumbrances(Transaction encumbrance, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.ENCUMBRANCE, encumbrance, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void putFinanceEncumbrancesById(String id, Transaction encumbrance, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (isEmpty(encumbrance.getId())) {
      encumbrance.setId(id);
    }
    if (id.equals(encumbrance.getId())) {

      transactionStrategyFactory.updateTransaction(Transaction.TransactionType.ENCUMBRANCE, encumbrance, new RequestContext(vertxContext, okapiHeaders))
        .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
        .onFailure(fail -> handleErrorResponse(asyncResultHandler,  fail));
    } else {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
    }
  }

  @Validate
  @Override
  public void deleteFinanceEncumbrancesById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    transactionService.retrieveTransactionById(id, new RequestContext(vertxContext, okapiHeaders))
        .compose(transaction -> transactionStrategyFactory.deleteTransaction(Transaction.TransactionType.ENCUMBRANCE,
          transaction, new RequestContext(vertxContext, okapiHeaders)))
        .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
        .onFailure(fail -> handleErrorResponse(asyncResultHandler,  fail));
  }

  @Validate
  @Override
  public void getFinanceTransactions(String query, String totalRecords, int offset, int limit, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionService.retrieveTransactions(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildOkResponse(types))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void getFinanceTransactionsById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionService.retrieveTransactionById(id, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(type -> asyncResultHandler.handle(succeededFuture(buildOkResponse(type))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinancePayments(Transaction payment, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.PAYMENT, payment, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  public void putFinancePaymentsById(String id, Transaction payment,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (isEmpty(payment.getId())) {
      payment.setId(id);
    } else if (!id.equals(payment.getId())) {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
      return;
    }
    transactionStrategyFactory.updateTransaction(
        TransactionType.PAYMENT, payment, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinancePendingPayments(Transaction pendingPayment, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.PENDING_PAYMENT, pendingPayment, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(transaction -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, transaction.getId()), transaction))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  public void putFinancePendingPaymentsById(String id, Transaction pendingPayment,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (isEmpty(pendingPayment.getId())) {
      pendingPayment.setId(id);
    } else if (!id.equals(pendingPayment.getId())) {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
      return;
    }
    transactionStrategyFactory.updateTransaction(
      TransactionType.PENDING_PAYMENT, pendingPayment, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceCredits(Transaction credit, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionStrategyFactory.createTransaction(Transaction.TransactionType.CREDIT, credit, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, type.getId()), type))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  public void putFinanceCreditsById(String id, Transaction credit,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (isEmpty(credit.getId())) {
      credit.setId(id);
    } else if (!id.equals(credit.getId())) {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
      return;
    }
    transactionStrategyFactory.updateTransaction(
        TransactionType.CREDIT, credit, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceBatchAllOrNothing(Batch batch, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    batchTransactionService.processBatch(batch, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
