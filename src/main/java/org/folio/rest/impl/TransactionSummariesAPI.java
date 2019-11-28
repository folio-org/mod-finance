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
import org.folio.rest.jaxrs.resource.FinanceOrderTransactionSummaries;

public class TransactionSummariesAPI implements FinanceOrderTransactionSummaries{

  private static final String ORDER_TRANSACTION_SUMMARIES_LOCATION_PREFIX = getEndpoint(FinanceOrderTransactionSummaries.class) + "/%s";


  @Override
  @Validate
  public void postFinanceOrderTransactionSummaries(String lang, OrderTransactionSummary entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    TransactionSummariesHelper helper = new TransactionSummariesHelper(okapiHeaders, vertxContext, lang);
    helper.createOrderTransactionSummary(entity)
      .thenAccept(type -> asyncResultHandler.handle(succeededFuture(
          helper.buildResponseWithLocation(String.format(ORDER_TRANSACTION_SUMMARIES_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

}

