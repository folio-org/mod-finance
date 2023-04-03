package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.logging.log4j.util.Strings.isEmpty;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgress;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversProgress;
import org.folio.services.ledger.LedgerRolloverProgressService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class LedgerRolloverProgressApi extends BaseApi implements FinanceLedgerRolloversProgress {

  private static final String LEDGER_ROLLOVER_PROGRESS_LOCATION_PREFIX = getEndpoint(FinanceLedgerRolloversProgress.class) + "/%s";

  @Autowired
  private LedgerRolloverProgressService ledgerRolloverProgressService;

  public LedgerRolloverProgressApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getFinanceLedgerRolloversProgress(String query, String totalRecords, int offset, int limit,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverProgressService.retrieveLedgerRolloverProgresses(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(rolloverProgresses -> asyncResultHandler.handle(succeededFuture(buildOkResponse(rolloverProgresses))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  public void postFinanceLedgerRolloversProgress(LedgerFiscalYearRolloverProgress entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverProgressService.createLedgerRolloverProgress(entity, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(progress -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(
        LEDGER_ROLLOVER_PROGRESS_LOCATION_PREFIX, progress.getId()), progress))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void getFinanceLedgerRolloversProgressById(String id, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverProgressService.retrieveLedgerRolloverProgressById(id, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(progress -> asyncResultHandler.handle(succeededFuture(buildOkResponse(progress))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  @Validate
  public void putFinanceLedgerRolloversProgressById(String id, LedgerFiscalYearRolloverProgress entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    // Set id if this is available only in path
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      asyncResultHandler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY))));
      return;
    }

    ledgerRolloverProgressService.updateLedgerRolloverProgressById(id, entity, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(progress -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
