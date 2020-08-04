package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.helper.FiscalYearsHelper;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.resource.FinanceFiscalYears;
import org.folio.rest.util.HelperUtils;
import org.folio.services.FiscalYearService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class FiscalYearsApi extends BaseApi implements FinanceFiscalYears {

  private static final String FISCAL_YEARS_LOCATION_PREFIX = getEndpoint(FinanceFiscalYears.class) + "/%s";
  private static final int FISCAL_YEAR_LENGTH = 4;

  @Autowired
  private FiscalYearService fiscalYearService;

  public FiscalYearsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postFinanceFiscalYears(String lang, FiscalYear fiscalYear, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    FiscalYearsHelper helper = new FiscalYearsHelper(headers, ctx, lang);

    // series should always be calculated
    setFYearWithSeries(fiscalYear);

    helper.createFiscalYear(fiscalYear)
      .thenAccept(fy -> handler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(FISCAL_YEARS_LOCATION_PREFIX, fy.getId()), fy))))
      .exceptionally(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceFiscalYears(int offset, int limit, String query, String lang, Map<String, String> headers,
                        Handler<AsyncResult<Response>> handler, Context ctx) {
    fiscalYearService.getFiscalYears(limit, offset, query, new RequestContext(ctx, headers))
      .thenAccept(obj -> handler.handle(succeededFuture(buildOkResponse(obj))))
      .exceptionally(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void putFinanceFiscalYearsById(String id, String lang, FiscalYear fiscalYearRequest, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    FiscalYearsHelper helper = new FiscalYearsHelper(headers, ctx, lang);

    // Set id if this is available only in path
    if (isEmpty(fiscalYearRequest.getId())) {
      fiscalYearRequest.setId(id);
    } else if (!id.equals(fiscalYearRequest.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      handler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    // series should always be calculated
    setFYearWithSeries(fiscalYearRequest);

    helper.updateFiscalYear(fiscalYearRequest)
      .thenAccept(fiscalYear -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceFiscalYearsById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    fiscalYearService.getFiscalYear(id, new RequestContext(ctx, headers))
                    .thenAccept(obj -> handler.handle(succeededFuture(buildOkResponse(obj))))
                    .exceptionally(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void deleteFinanceFiscalYearsById(String id, String lang, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    FiscalYearsHelper helper = new FiscalYearsHelper(headers, ctx, lang);
    helper.deleteFiscalYear(id)
      .thenAccept(fiscalYear -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> HelperUtils.handleErrorResponse(handler, helper, fail));
  }

  private void setFYearWithSeries(FiscalYear fiscalYear) {
    String code = fiscalYear.getCode();
    fiscalYear.withSeries(code.substring(0, code.length() - FISCAL_YEAR_LENGTH));
  }
}
