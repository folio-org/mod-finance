package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_ERRORS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverError;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversErrors;
import org.folio.services.ledger.LedgerRolloverErrorsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class LedgerRolloversErrorsApi extends BaseApi implements FinanceLedgerRolloversErrors {

  @Autowired
  private LedgerRolloverErrorsService ledgerRolloverErrorsService;

  public LedgerRolloversErrorsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getFinanceLedgerRolloversErrors(String query, String totalRecords, int offset, int limit, String accept,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    RequestContext requestContext = new RequestContext(vertxContext, okapiHeaders);
    ledgerRolloverErrorsService.getLedgerRolloverErrors(query, offset, limit, accept, requestContext)
      .onSuccess(rolloverErrors -> asyncResultHandler.handle(succeededFuture(buildOkResponse(rolloverErrors))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void postFinanceLedgerRolloversErrors(LedgerFiscalYearRolloverError entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    RequestContext requestContext = new RequestContext(vertxContext, okapiHeaders);
    ledgerRolloverErrorsService.createLedgerRolloverError(entity, requestContext)
      .onSuccess(rolloverError -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(
        okapiHeaders.get(OKAPI_URL), resourceByIdPath(LEDGER_ROLLOVERS_ERRORS_STORAGE, rolloverError.getId()),
        rolloverError))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void deleteFinanceLedgerRolloversErrorsById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    RequestContext requestContext = new RequestContext(vertxContext, okapiHeaders);
    ledgerRolloverErrorsService.deleteLedgerRolloverError(id, requestContext)
      .onSuccess(rolloverError -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
