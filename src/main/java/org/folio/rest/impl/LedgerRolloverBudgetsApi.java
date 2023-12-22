package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversBudgets;
import org.folio.services.ledger.LedgerRolloverBudgetsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class LedgerRolloverBudgetsApi extends BaseApi implements FinanceLedgerRolloversBudgets {

  @Autowired
  private LedgerRolloverBudgetsService ledgerRolloverBudgetsService;

  public LedgerRolloverBudgetsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getFinanceLedgerRolloversBudgets(String query, String totalRecords, int offset, int limit, Map<String, String> okapiHeaders,
                                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgets(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(ledgerRolloverBudgets -> asyncResultHandler.handle(succeededFuture(buildOkResponse(ledgerRolloverBudgets))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Override
  public void getFinanceLedgerRolloversBudgetsById(String id, Map<String, String> okapiHeaders,
                                                   Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(id, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(rolloverBudget -> asyncResultHandler.handle(succeededFuture(buildOkResponse(rolloverBudget))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
