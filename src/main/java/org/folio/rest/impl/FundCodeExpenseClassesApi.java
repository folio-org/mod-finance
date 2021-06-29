package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.resource.FinanceFundCodesExpenseClasses;
import org.folio.services.fund.FundDetailsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.folio.rest.util.HelperUtils.getEndpoint;

public class FundCodeExpenseClassesApi implements FinanceFundCodesExpenseClasses {

  private static final String FUND_CODES_EXPENSE_CLASSES = getEndpoint(FundCodeExpenseClassesApi.class);
  @Autowired
  private FundDetailsService fundDetailsService;

  public FundCodeExpenseClassesApi()  {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getFinanceFundCodesExpenseClasses(String fiscalYearCode, int offset, int limit, String lang,
                                                Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler,
                                                Context vertxContext) {
    //fundDetailsService.retrieveCombinationFundCodeExpClasses(query, offset, limit, new RequestContext(ctx, headers));
  }
}
