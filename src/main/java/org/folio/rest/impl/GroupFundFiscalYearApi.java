package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.AbstractHelper;
import org.folio.rest.helper.GroupFundFiscalYearHelper;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.resource.FinanceGroupFundFiscalYears;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class GroupFundFiscalYearApi implements FinanceGroupFundFiscalYears {

  private static final String GROUP_FUND_FISCAL_YEAR_LOCATION_PREFIX = getEndpoint(FinanceGroupFundFiscalYears.class) + "/%s";

  @Override
  @Validate
  public void postFinanceGroupFundFiscalYears(String lang, GroupFundFiscalYear entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    GroupFundFiscalYearHelper helper = new GroupFundFiscalYearHelper(headers, ctx, lang);

    helper.createGroupFundFiscalYear(entity)
      .thenAccept(groupFundFiscalYear -> handler.handle(succeededFuture(helper.buildResponseWithLocation(
          String.format(GROUP_FUND_FISCAL_YEAR_LOCATION_PREFIX, groupFundFiscalYear.getId()), groupFundFiscalYear))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Override
  @Validate
  public void getFinanceGroupFundFiscalYears(int offset, int limit, String query, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    GroupFundFiscalYearHelper helper = new GroupFundFiscalYearHelper(headers, ctx, lang);

    helper.getGroupFundFiscalYears(limit, offset, query)
      .thenAccept(groupFundFiscalYears -> handler.handle(succeededFuture(helper.buildOkResponse(groupFundFiscalYears))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Override
  @Validate
  public void deleteFinanceGroupFundFiscalYearsById(String id, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    GroupFundFiscalYearHelper helper = new GroupFundFiscalYearHelper(headers, ctx, lang);

    helper.deleteGroupFundFiscalYear(id)
      .thenAccept(success -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  private Void handleErrorResponse(Handler<AsyncResult<Response>> handler, AbstractHelper helper, Throwable t) {
    handler.handle(succeededFuture(helper.buildErrorResponse(t)));
    return null;
  }
}
