package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.util.HelperUtils;
import org.folio.services.group.GroupFundFiscalYearService;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.resource.FinanceGroupFundFiscalYears;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class GroupFundFiscalYearApi extends BaseApi implements FinanceGroupFundFiscalYears {

  private static final String GROUP_FUND_FISCAL_YEAR_LOCATION_PREFIX = getEndpoint(FinanceGroupFundFiscalYears.class) + "/%s";
  @Autowired
  private GroupFundFiscalYearService groupFundFiscalYearService;

  public GroupFundFiscalYearApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void postFinanceGroupFundFiscalYears(GroupFundFiscalYear entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    groupFundFiscalYearService.createGroupFundFiscalYear(entity,  new RequestContext(ctx, headers))
      .onSuccess(groupFundFiscalYear -> handler.handle(succeededFuture(buildResponseWithLocation(headers.get(OKAPI_URL),
          String.format(GROUP_FUND_FISCAL_YEAR_LOCATION_PREFIX, groupFundFiscalYear.getId()), groupFundFiscalYear))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  @Override
  @Validate
  public void getFinanceGroupFundFiscalYears(String totalRecords, int offset, int limit, String query, Map<String, String> headers,
    Handler<AsyncResult<Response>> handler, Context ctx) {

    groupFundFiscalYearService.getGroupFundFiscalYears(query, offset, limit, new RequestContext(ctx, headers))
      .onSuccess(groupFundFiscalYears -> handler.handle(succeededFuture(buildOkResponse(groupFundFiscalYears))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  @Override
  @Validate
  public void deleteFinanceGroupFundFiscalYearsById(String id, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    groupFundFiscalYearService.deleteGroupFundFiscalYear(id, new RequestContext(ctx, headers))
      .onSuccess(success -> handler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

}
