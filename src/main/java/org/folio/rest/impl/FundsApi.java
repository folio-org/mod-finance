package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.helper.FundsHelper;
import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.resource.FinanceFundTypes;
import org.folio.rest.jaxrs.resource.FinanceFunds;
import org.folio.rest.util.HelperUtils;
import org.folio.services.fund.FundDetailsService;
import org.folio.services.fund.FundService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class FundsApi extends BaseApi implements FinanceFunds, FinanceFundTypes{

  private static final String FUNDS_LOCATION_PREFIX = getEndpoint(FinanceFunds.class) + "/%s";
  private static final String FUND_TYPES_LOCATION_PREFIX = getEndpoint(FinanceFundTypes.class) + "/%s";

  @Autowired
  private FundDetailsService fundDetailsService;
  @Autowired
  private FundService fundService;

  public FundsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void postFinanceFunds(CompositeFund compositeFund, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    FundsHelper helper = new FundsHelper(okapiHeaders, vertxContext);

    helper.createFund(compositeFund, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(fund -> asyncResultHandler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(FUNDS_LOCATION_PREFIX, fund.getFund().getId()), fund))))
      .onFailure(fail -> HelperUtils.handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Override
  @Validate
  public void getFinanceFunds(String totalRecords, int offset, int limit, String query, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx);

    fundService.getFundsWithAcqUnitsRestriction(query, offset, limit, new RequestContext(ctx, headers))
      .onSuccess(funds -> handler.handle(succeededFuture(helper.buildOkResponse(funds))))
      .onFailure(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Override
  @Validate
  public void putFinanceFundsById(String id, CompositeFund compositeFund, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    FundsHelper helper = new FundsHelper(okapiHeaders, vertxContext);

    // Set id if this is available only in path
    if (StringUtils.isEmpty(compositeFund.getFund().getId())) {
      compositeFund.getFund().setId(id);
    } else if (!id.equals(compositeFund.getFund().getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateFund(compositeFund, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(types -> asyncResultHandler.handle(succeededFuture(helper.buildNoContentResponse())))
      .onFailure(fail -> HelperUtils.handleErrorResponse(asyncResultHandler, helper, fail));
  }

  @Override
  @Validate
  public void getFinanceFundsById(String id, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx);

    helper.getCompositeFund(id, new RequestContext(ctx, headers))
      .onSuccess(fund -> handler.handle(succeededFuture(helper.buildOkResponse(fund))))
      .onFailure(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void deleteFinanceFundsById(String id, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx);

    helper.deleteFund(id, new RequestContext(ctx, headers))
      .onSuccess(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .onFailure(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void postFinanceFundTypes(FundType entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    FundsHelper helper = new FundsHelper(headers, ctx);
    helper.createFundType(entity, new RequestContext(ctx, headers))
      .onSuccess(type -> handler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(FUND_TYPES_LOCATION_PREFIX, type.getId()), type))))
      .onFailure(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceFundTypes(String totalRecords, int offset, int limit, String query, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx);

    helper.getFundTypes(offset, limit, query, new RequestContext(ctx, headers))
      .onSuccess(types -> handler.handle(succeededFuture(helper.buildOkResponse(types))))
      .onFailure(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void putFinanceFundTypesById(String id, FundType fundType, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx);

    // Set id if this is available only in path
    if (StringUtils.isEmpty(fundType.getId())) {
      fundType.setId(id);
    } else if (!id.equals(fundType.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      handler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateFundType(fundType, new RequestContext(ctx, headers))
      .onSuccess(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .onFailure(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceFundTypesById(String id, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx);

    helper.getFundType(id, new RequestContext(ctx, headers))
      .onSuccess(type -> handler.handle(succeededFuture(helper.buildOkResponse(type))))
      .onFailure(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void deleteFinanceFundTypesById(String id, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    FundsHelper helper = new FundsHelper(headers, ctx);

    helper.deleteFundType(id, new RequestContext(ctx, headers))
      .onSuccess(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .onFailure(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceFundsExpenseClassesById(String id, String status, String fiscalYearId,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> handler, Context ctx) {
    fundDetailsService.retrieveExpenseClasses(id, fiscalYearId, status, new RequestContext(ctx, okapiHeaders))
      .onSuccess(obj -> handler.handle(succeededFuture(buildOkResponse(obj))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void getFinanceFundsBudgetById(String id, String status, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    fundDetailsService.retrieveCurrentBudget(id, status, new RequestContext(ctx, okapiHeaders))
      .onSuccess(obj -> handler.handle(succeededFuture(buildOkResponse(obj))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

}
