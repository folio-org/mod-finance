package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.FundsHelper;
import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.resource.FinanceFundTypes;
import org.folio.rest.jaxrs.resource.FinanceFunds;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class FundsApi implements FinanceFunds, FinanceFundTypes {

  private static final String FUNDS_LOCATION_PREFIX = getEndpoint(FinanceFunds.class) + "/%s";
  private static final String FUND_TYPES_LOCATION_PREFIX = getEndpoint(FinanceFundTypes.class) + "/%s";

  @Override
  @Validate
  public void postFinanceFunds(String lang, CompositeFund compositeFund, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    FundsHelper helper = new FundsHelper(okapiHeaders, vertxContext, lang);

    helper.createFund(compositeFund)
      .thenAccept(fund -> asyncResultHandler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(FUNDS_LOCATION_PREFIX, fund.getFund().getId()), fund))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Override
  @Validate
  public void getFinanceFunds(int offset, int limit, String query, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx, lang);

    helper.getFunds(limit, offset, query)
      .thenAccept(funds -> handler.handle(succeededFuture(helper.buildOkResponse(funds))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Override
  @Validate
  public void putFinanceFundsById(String id, String lang, CompositeFund compositeFund, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    FundsHelper helper = new FundsHelper(okapiHeaders, vertxContext, lang);

    // Set id if this is available only in path
    if (StringUtils.isEmpty(compositeFund.getFund().getId())) {
      compositeFund.getFund().setId(id);
    } else if (!id.equals(compositeFund.getFund().getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateFund(compositeFund)
      .thenAccept(types -> asyncResultHandler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Override
  @Validate
  public void getFinanceFundsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx, lang);

    helper.getCompositeFund(id)
      .thenAccept(fund -> handler.handle(succeededFuture(helper.buildOkResponse(fund))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void deleteFinanceFundsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx, lang);

    helper.deleteFund(id)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void postFinanceFundTypes(String lang, FundType entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    FundsHelper helper = new FundsHelper(headers, ctx, lang);
    helper.createFundType(entity)
      .thenAccept(type -> handler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(FUND_TYPES_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceFundTypes(int offset, int limit, String query, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx, lang);

    helper.getFundTypes(limit, offset, query)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildOkResponse(types))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void putFinanceFundTypesById(String id, String lang, FundType entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx, lang);

    // Set id if this is available only in path
    if (StringUtils.isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      handler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateFundType(entity)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceFundTypesById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx, lang);

    helper.getFundType(id)
      .thenAccept(type -> handler.handle(succeededFuture(helper.buildOkResponse(type))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void deleteFinanceFundTypesById(String id, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx, lang);

    helper.deleteFundType(id)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

}
