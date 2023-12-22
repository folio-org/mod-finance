package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.resource.FinanceFundCodesExpenseClasses;
import org.folio.services.fund.FundCodeExpenseClassesService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class FundCodeExpenseClassesApi extends BaseApi implements FinanceFundCodesExpenseClasses {

  @Autowired
  private FundCodeExpenseClassesService fundCodeExpenseClassesService;

  public FundCodeExpenseClassesApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void getFinanceFundCodesExpenseClasses(String fiscalYearCode, String totalRecords, int offset, int limit,
                                                Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> handler,
                                                Context vertxContext) {
    fundCodeExpenseClassesService.retrieveCombinationFundCodeExpClasses(fiscalYearCode, new RequestContext(vertxContext, okapiHeaders))
      .onSuccess(groupFundFiscalYearSummaries -> handler.handle(succeededFuture(buildOkResponse(groupFundFiscalYearSummaries))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }
}
