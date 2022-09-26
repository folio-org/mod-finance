package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversBudgets;
import org.folio.services.ledger.LedgerRolloverBudgetsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class LedgerRolloverBudgetsApi extends BaseApi implements FinanceLedgerRolloversBudgets {

  @Autowired
  private LedgerRolloverBudgetsService ledgerRolloverBudgetsService;

  public LedgerRolloverBudgetsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getFinanceLedgerRolloversBudgets(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders,
                                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgets(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(ledgerRolloverBudgets -> asyncResultHandler.handle(succeededFuture(buildOkResponse(ledgerRolloverBudgets))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  public void getFinanceLedgerRolloversBudgetsById(String id, String lang, Map<String, String> okapiHeaders,
                                                   Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(id, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(rolloverBudget -> asyncResultHandler.handle(succeededFuture(buildOkResponse(rolloverBudget))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
