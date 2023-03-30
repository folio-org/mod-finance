package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversLogs;
import org.folio.services.ledger.LedgerRolloverLogsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class LedgerRolloverLogsApi extends BaseApi implements FinanceLedgerRolloversLogs {

  @Autowired
  private LedgerRolloverLogsService ledgerRolloverLogsService;

  public LedgerRolloverLogsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getFinanceLedgerRolloversLogs(String query, String totalRecords, int offset, int limit, Map<String, String> okapiHeaders,
                                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverLogsService.retrieveLedgerRolloverLogs(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(ledgerRolloverLogs -> asyncResultHandler.handle(succeededFuture(buildOkResponse(ledgerRolloverLogs))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  public void getFinanceLedgerRolloversLogsById(String id, Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverLogsService.retrieveLedgerRolloverLogById(id, new RequestContext(vertxContext, okapiHeaders))
        .thenAccept(rolloverLog -> asyncResultHandler.handle(succeededFuture(buildOkResponse(rolloverLog))))
        .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
