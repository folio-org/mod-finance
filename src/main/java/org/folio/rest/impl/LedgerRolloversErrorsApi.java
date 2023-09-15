package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverError;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversErrors;
import org.folio.services.ledger.LedgerRolloverErrorsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_ERRORS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

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
      .thenAccept(rolloverErrors -> asyncResultHandler.handle(succeededFuture(buildOkResponse(rolloverErrors))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void postFinanceLedgerRolloversErrors(LedgerFiscalYearRolloverError entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    RequestContext requestContext = new RequestContext(vertxContext, okapiHeaders);
    ledgerRolloverErrorsService.createLedgerRolloverError(entity, requestContext)
      .thenAccept(rolloverError -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(
        okapiHeaders.get(OKAPI_URL), resourceByIdPath(LEDGER_ROLLOVERS_ERRORS_STORAGE, rolloverError.getId()),
        rolloverError))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void deleteFinanceLedgerRolloversErrorsById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    RequestContext requestContext = new RequestContext(vertxContext, okapiHeaders);
    ledgerRolloverErrorsService.deleteLedgerRolloverError(id, requestContext)
      .thenAccept(rolloverError -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
