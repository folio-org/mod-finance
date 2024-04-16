package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.resource.Finance;
import org.folio.services.transactions.TransactionApiService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class TransactionsApi extends BaseApi implements Finance {

  private static final String TRANSACTIONS_LOCATION_PREFIX = getEndpoint(Finance.class) + "/%s";

  @Autowired
  private TransactionApiService transactionApiService;

  public TransactionsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postFinanceAllocations(Transaction allocation, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionApiService.createAllocation(allocation, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(tr -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(
        okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, tr.getId()), tr))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceTransfers(Transaction transfer, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionApiService.createTransfer(transfer, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(tr -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(
        okapiHeaders.get(OKAPI_URL), String.format(TRANSACTIONS_LOCATION_PREFIX, tr.getId()), tr))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void getFinanceTransactions(String query, String totalRecords, int offset, int limit, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionApiService.getTransactionCollectionByQuery(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildOkResponse(types))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void getFinanceTransactionsById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionApiService.getTransactionById(id, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(type -> asyncResultHandler.handle(succeededFuture(buildOkResponse(type))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void postFinanceTransactionsBatchAllOrNothing(Batch batch, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    transactionApiService.processBatch(batch, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
