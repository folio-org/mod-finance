package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEAR_INVALID_CODE;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEAR_INVALID_PERIOD;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.resource.FinanceFiscalYears;
import org.folio.services.fiscalyear.FiscalYearApiService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class FiscalYearsApi extends BaseApi implements FinanceFiscalYears {

  private static final String FISCAL_YEARS_LOCATION_PREFIX = getEndpoint(FinanceFiscalYears.class) + "/%s";
  private static final int FISCAL_YEAR_LENGTH = 4;
  private static final String FISCAL_YEAR_CODE_PATTERN = "^[a-zA-Z]+[0-9]{4}$";

  @Autowired
  private FiscalYearApiService fiscalYearApiService;

  public FiscalYearsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postFinanceFiscalYears(FiscalYear fiscalYear, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    if (!isFiscalYearValid(fiscalYear)) {
       handleInvalidFiscalYearCode(handler);
       return;
    }

    if (!isPeriodValid(fiscalYear)) {
      handleInvalidPeriod(handler);
      return;
    }

    // series should always be calculated
    setFYearWithSeries(fiscalYear);

    fiscalYearApiService.createFiscalYear(fiscalYear, new RequestContext(ctx, headers))
      .onSuccess(fy -> handler
        .handle(succeededFuture(buildResponseWithLocation(headers.get(OKAPI_URL), String.format(FISCAL_YEARS_LOCATION_PREFIX, fy.getId()), fy))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  private boolean isFiscalYearValid(FiscalYear fiscalYear) {
    return fiscalYear.getCode().matches(FISCAL_YEAR_CODE_PATTERN);
  }

  @Validate
  @Override
  public void getFinanceFiscalYears(String totalRecords, int offset, int limit, String query, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    fiscalYearApiService.getFiscalYearsWithAcqUnitsRestriction(query, offset, limit, new RequestContext(ctx, headers))
      .onSuccess(fiscalYears -> handler.handle(succeededFuture(buildOkResponse(fiscalYears))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void putFinanceFiscalYearsById(String id, FiscalYear fiscalYearRequest, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {

    if (!isFiscalYearValid(fiscalYearRequest)) {
      handleInvalidFiscalYearCode(handler);
      return;
    }

    if (!isPeriodValid(fiscalYearRequest)) {
      handleInvalidPeriod(handler);
      return;
    }

    // Set id if this is available only in path
    if (isEmpty(fiscalYearRequest.getId())) {
      fiscalYearRequest.setId(id);
    } else if (!id.equals(fiscalYearRequest.getId())) {
      handler.handle(succeededFuture(buildErrorResponse(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()))));
      return;
    }

    // series should always be calculated
    setFYearWithSeries(fiscalYearRequest);

    fiscalYearApiService.updateFiscalYear(fiscalYearRequest, new RequestContext(ctx, headers))
      .onSuccess(fiscalYear -> handler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void getFinanceFiscalYearsById(String id, boolean withFinancialSummary, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {

    fiscalYearApiService.getFiscalYearById(id, withFinancialSummary, new RequestContext(ctx, headers))
      .onSuccess(fiscalYear -> handler.handle(succeededFuture(buildOkResponse(fiscalYear))))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  @Validate
  @Override
  public void deleteFinanceFiscalYearsById(String id, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    fiscalYearApiService.deleteFiscalYear(id, new RequestContext(ctx, headers))
      .onSuccess(fiscalYear -> handler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(fail -> handleErrorResponse(handler, fail));
  }

  private void setFYearWithSeries(FiscalYear fiscalYear) {
    String code = fiscalYear.getCode();
    fiscalYear.withSeries(code.substring(0, code.length() - FISCAL_YEAR_LENGTH));
  }

  private boolean isPeriodValid(FiscalYear fiscalYear) {
    return fiscalYear.getPeriodStart().before(fiscalYear.getPeriodEnd());
  }

  private void handleInvalidPeriod(Handler<AsyncResult<Response>> handler) {
    handler.handle(succeededFuture(buildErrorResponse(new HttpException(422, FISCAL_YEAR_INVALID_PERIOD.toError()))));
  }

  private void handleInvalidFiscalYearCode(Handler<AsyncResult<Response>> handler) {
    handler.handle(succeededFuture(buildErrorResponse(new HttpException(422, FISCAL_YEAR_INVALID_CODE.toError()))));
  }
}
