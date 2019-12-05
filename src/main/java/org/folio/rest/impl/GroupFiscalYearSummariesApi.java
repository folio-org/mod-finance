package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.GroupFiscalYearSummariesHelper;
import org.folio.rest.jaxrs.resource.FinanceGroupFiscalYearSummaries;

import javax.ws.rs.core.Response;
import java.util.Map;


public class GroupFiscalYearSummariesApi implements FinanceGroupFiscalYearSummaries {

  @Override
  @Validate
  public void getFinanceGroupFiscalYearSummaries(String query, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler, Context ctx) {
    GroupFiscalYearSummariesHelper helper = new GroupFiscalYearSummariesHelper(headers, ctx, lang);
    helper.getGroupFiscalYearSummaries(query)
      .thenAccept(groupFundFiscalYearSummaries -> handler.handle(succeededFuture(helper.buildOkResponse(groupFundFiscalYearSummaries))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }
}
