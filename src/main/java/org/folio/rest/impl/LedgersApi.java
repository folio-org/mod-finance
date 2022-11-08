package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.Response;

import org.folio.HttpStatus;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.resource.FinanceLedgers;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class LedgersApi extends BaseApi implements FinanceLedgers {

  private static final String LEDGERS_LOCATION_PREFIX = getEndpoint(FinanceLedgers.class) + "/%s";

  @Autowired
  private LedgerDetailsService ledgerDetailsService;
  @Autowired
  private LedgerService ledgerService;

  public LedgersApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postFinanceLedgers(String lang, Ledger entity, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    ledgerService.createLedger(entity, new RequestContext(ctx, headers))
      .thenAccept(type -> handler
        .handle(succeededFuture(buildResponseWithLocation(headers.get(OKAPI_URL), String.format(LEDGERS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void getFinanceLedgers(String fiscalYearId, int offset, int limit, String query, String lang, Map<String, String> headers,
     Handler<AsyncResult<Response>> handler, Context ctx) {

    ledgerService.retrieveLedgersWithAcqUnitsRestrictionAndTotals(query, offset, limit, fiscalYearId, new RequestContext(ctx, headers))
      .thenAccept(ledgersCollection -> handler.handle(succeededFuture(buildOkResponse(ledgersCollection))))
      .exceptionally(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void putFinanceLedgersById(String id, String lang, Ledger entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    // Set id if this is available only in path
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      handler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
      return;
    }

    ledgerService.updateLedger(entity, new RequestContext(ctx, headers))
      .thenAccept(types -> handler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void getFinanceLedgersById(String ledgerId, String fiscalYearId, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    ledgerService.retrieveLedgerWithTotals(ledgerId, fiscalYearId, new RequestContext(ctx, headers))
      .thenAccept(type -> handler.handle(succeededFuture(buildOkResponse(type))))
      .exceptionally(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void deleteFinanceLedgersById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    ledgerService.deleteLedger(id, new RequestContext(ctx, headers))
      .thenAccept(types -> handler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void getFinanceLedgersCurrentFiscalYearById(String ledgerId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> handler, Context vertxContext) {

    ledgerDetailsService.getCurrentFiscalYear(ledgerId, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(currentFiscalYear -> {
        if(Objects.nonNull(currentFiscalYear)) {
          handler.handle(succeededFuture(buildOkResponse(currentFiscalYear)));
        } else {
          handler.handle(succeededFuture(buildErrorResponse(new HttpException(HttpStatus.HTTP_NOT_FOUND.toInt(), HttpStatus.HTTP_NOT_FOUND.name()))));
        }
      })
      .exceptionally(fail -> handleErrorResponse(handler, fail));
  }

}
