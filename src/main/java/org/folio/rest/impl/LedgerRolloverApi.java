package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover;
import org.folio.rest.jaxrs.resource.FinanceLedgerRollovers;
import org.folio.services.ledger.LedgerRolloverService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class LedgerRolloverApi extends BaseApi implements FinanceLedgerRollovers {

  private static final String LEDGER_ROLLOVER_LOCATION_PREFIX = getEndpoint(FinanceLedgerRollovers.class) + "/%s";

  @Autowired
  private LedgerRolloverService ledgerRolloverService;

  public LedgerRolloverApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getFinanceLedgerRollovers(String query, String totalRecords, int offset, int limit,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverService.retrieveLedgerRollovers(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(ledgerRollovers -> asyncResultHandler.handle(succeededFuture(buildOkResponse(ledgerRollovers))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void postFinanceLedgerRollovers(LedgerFiscalYearRollover entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverService.createLedgerFyRollover(entity, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(rollover -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(
          LEDGER_ROLLOVER_LOCATION_PREFIX, rollover.getId()), rollover))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void getFinanceLedgerRolloversById(String id, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverService.retrieveLedgerRolloverById(id, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(rollover -> asyncResultHandler.handle(succeededFuture(buildOkResponse(rollover))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
