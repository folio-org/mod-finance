package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.resource.FinanceFiscalYearHierarchy;
import org.folio.services.fiscalyear.FiscalYearHierarchyService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class FiscalYearHierarchyApi extends BaseApi implements FinanceFiscalYearHierarchy {

  @Autowired
  private FiscalYearHierarchyService fiscalYearHierarchyService;

  public FiscalYearHierarchyApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getFinanceFiscalYearHierarchy(String fiscalYearId, String ledgerStatus, String groupStatus, String fundStatus,
      String budgetStatus, String expenseClassStatus, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    fiscalYearHierarchyService.retrieveFiscalYearHierarchy(fiscalYearId, ledgerStatus, groupStatus, fundStatus, budgetStatus,
        expenseClassStatus, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(hierarchy -> asyncResultHandler.handle(succeededFuture(buildOkResponse(hierarchy))))
      .onFailure(fail -> handleErrorResponse(asyncResultHandler, fail));
  }
}
