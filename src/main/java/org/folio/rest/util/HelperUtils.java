package org.folio.rest.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.finance.LedgerFY;
import org.folio.rest.exception.HttpException;
import org.folio.rest.helper.AbstractHelper;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.tools.client.Response;

import javax.ws.rs.Path;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.ErrorCodes.NEGATIVE_VALUE;

public class HelperUtils {
  public static final String ID = "id";
  public static final String LANG = "lang";
  public static final String OKAPI_URL = "X-Okapi-Url";
  public static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {}";
  public static final String CALLING_ENDPOINT_MSG = "Sending {} {}";

  private static final String ERROR_CAUSE = "cause";
  private static final String ERROR_MESSAGE = "errorMessage";
  private static final Pattern CQL_SORT_BY_PATTERN = Pattern.compile("(.*)(\\ssortBy\\s.*)", Pattern.CASE_INSENSITIVE);

  private HelperUtils() {
  }

  public static JsonObject convertToJson(Object data) {
    return data instanceof JsonObject ? (JsonObject) data : JsonObject.mapFrom(data);
  }

  /**
   * @param query  string representing CQL query
   * @param logger {@link Logger} to log error if any
   * @return URL encoded string
   */
  public static String encodeQuery(String query, Logger logger) {
    try {
      return URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      logger.error("Error happened while attempting to encode '{}'", e, query);
      throw new CompletionException(e);
    }
  }

  public static String buildQueryParam(String query, Logger logger) {
    return isEmpty(query) ? EMPTY : "&query=" + encodeQuery(query, logger);
  }


  public static void verifyResponse(Response response) {
    if (!Response.isSuccess(response.getCode())) {
      String errorMsg = response.getError().getString(ERROR_MESSAGE);
      HttpException httpException = getErrorByCode(errorMsg)
        .map(errorCode -> new HttpException(response.getCode(), errorCode))
        .orElse(new HttpException(response.getCode(), errorMsg));
      throw new CompletionException(httpException);
    }
  }

  public static Optional<ErrorCodes> getErrorByCode(String errorCode){
    return EnumSet.allOf(ErrorCodes.class).stream()
      .filter(errorCodes -> errorCodes.getCode().equals(errorCode))
      .findAny();
  }

  public static JsonObject verifyAndExtractBody(Response response) {
    verifyResponse(response);
    return response.getBody();
  }

  public static String getEndpoint(Class<?> clazz) {
    return clazz.getAnnotation(Path.class).value();
  }

  public static Duration getFiscalYearDuration(FiscalYear fiscalYearOne) {
    Instant start = fiscalYearOne.getPeriodStart().toInstant();
    Instant end = fiscalYearOne.getPeriodEnd().toInstant();
    return Duration.between(start, end);
  }

  /**
   * Transform list of id's to CQL query using 'or' operation
   * @param ids list of id's
   * @return String representing CQL query to get records by id's
   */
  public static String convertIdsToCqlQuery(Collection<String> ids) {
    return convertIdsToCqlQuery(ids, ID, true);
  }

  public static String convertIdsToCqlQuery(Collection<String> ids, String fieldName) {
    return convertIdsToCqlQuery(ids, fieldName, true);
  }
  /**
   * Transform list of values for some property to CQL query using 'or' operation
   * @param values list of field values
   * @param fieldName the property name to search by
   * @param strictMatch indicates whether strict match mode (i.e. ==) should be used or not (i.e. =)
   * @return String representing CQL query to get records by some property values
   */
  public static String convertIdsToCqlQuery(Collection<String> values, String fieldName, boolean strictMatch) {
    String prefix = fieldName + (strictMatch ? "==(" : "=(");
    return StreamEx.of(values).joining(" or ", prefix, ")");
  }

  /**
   * Transform list of values for some property to CQL query using 'or' operation
   * @param values list of field values
   * @param fieldName the property name to search by
   * @param logicConnector operation between ids
   * @param op global operator
   * @return String representing CQL query to get records by some property values
   */
  public static String convertIdsToCqlQuery(Collection<String> values, String fieldName, String op, String logicConnector) {
    String prefix = fieldName + op + "(";
    return StreamEx.of(values).joining(logicConnector, prefix, ")");
  }

  /**
   * Wait for all requests completion and collect all resulting objects. In case any failed, complete resulting future with the exception
   * @param futures list of futures and each produces resulting object on completion
   * @param <T> resulting objects type
   * @return CompletableFuture with resulting objects
   */
  public static <T> CompletableFuture<List<T>> collectResultsOnSuccess(List<CompletableFuture<T>> futures) {
    return allOf(futures.toArray(new CompletableFuture[0]))
      .thenApply(v -> futures
        .stream()
        // The CompletableFuture::join can be safely used because the `allOf` guaranties success at this step
        .map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .collect(toList())
      );
  }

  /**
   * This method returns the set difference of B and A - the set of elements in B but not in A
   * @param a set A
   * @param b set B
   * @return the relative complement of A in B
   */
  public static List<String> getSetDifference(Collection<String> a, Collection<String> b) {
    return b.stream()
      .filter(item -> !a.contains(item))
      .collect(toList());
  }

  /**
   * This method populates financial data (allocated, available, unavailable) from LedgerFY to Ledger
   * @param ledger initial Ledger
   * @param ledgerFY LedgerFY
   * @return transformed Ledger with financial information from LedgerFY
   */
  public static Ledger populateDataFromLedgerFY(Ledger ledger, LedgerFY ledgerFY) {
    ledger.setAllocated(ledgerFY.getAllocated());
    ledger.setAvailable(ledgerFY.getAvailable());
    ledger.setUnavailable(ledgerFY.getUnavailable());
    return ledger;
  }

  public static Void handleErrorResponse(Handler<AsyncResult<javax.ws.rs.core.Response>> handler, AbstractHelper helper, Throwable t) {
    handler.handle(succeededFuture(helper.buildErrorResponse(t)));
    return null;

  }

  public static boolean isErrorMessageJson(String errorMessage) {
    if (!StringUtils.isEmpty(errorMessage)) {
      Pattern pattern = Pattern.compile("(message).*(code).*(parameters)");
      Matcher matcher = pattern.matcher(errorMessage);
      if (matcher.find()) {
        return matcher.groupCount() == 3;
      }
    }
    return false;
  }

  public static int defineErrorCode(Throwable throwable) {
    final Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
    if (cause instanceof HttpException) {
      return ((HttpException) cause).getCode();
    }
    return INTERNAL_SERVER_ERROR.getStatusCode();
  }

  public static Errors convertToErrors(Throwable throwable) {
    final Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
    Errors errors;

    if (cause instanceof HttpException) {
      errors = ((HttpException) cause).getErrors();
      List<Error> errorList = errors.getErrors().stream().map(HelperUtils::mapToError).collect(toList());
      errors.setErrors(errorList);
    } else {
      errors = new Errors().withErrors(Collections.singletonList(GENERIC_ERROR_CODE.toError()
        .withAdditionalProperty(ERROR_CAUSE, cause.getMessage())))
        .withTotalRecords(1);
    }
    return errors;
  }

  private static Error mapToError(Error error) {
    if (HelperUtils.isErrorMessageJson(error.getMessage())) {
      return new JsonObject(error.getMessage()).mapTo(Error.class);
    }
    return error;
  }


  public static javax.ws.rs.core.Response.ResponseBuilder createResponseBuilder(int code) {
    final javax.ws.rs.core.Response.ResponseBuilder responseBuilder;
    switch (code) {
      case 400:
      case 403:
      case 404:
      case 409:
      case 422:
        responseBuilder = javax.ws.rs.core.Response.status(code);
        break;
      default:
        responseBuilder = javax.ws.rs.core.Response.status(INTERNAL_SERVER_ERROR);
    }
    return responseBuilder;
  }

  public static <T> CompletableFuture<List<T>> emptyListFuture() {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  public static void validateAmount(double doubleAmount, String fieldName) {
    BigDecimal amount = BigDecimal.valueOf(doubleAmount);
    if (isNegative(amount)) {
      Parameter parameter = new Parameter().withKey("field").withValue(fieldName);
      throw new HttpException(422, NEGATIVE_VALUE.toError().withParameters(Collections.singletonList(parameter)));
    }
  }

  private static boolean isNegative(BigDecimal amount) {
    return amount.compareTo(BigDecimal.ZERO) < 0;
  }

  public static CompletableFuture<Void> unsupportedOperationExceptionFuture() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new UnsupportedOperationException());
    return future;
  }

  public static  <T> double calculateTotals(List<T> budgets, ToDoubleFunction<T> getDouble) {
    return budgets.stream()
      .map(budget -> BigDecimal.valueOf(getDouble.applyAsDouble(budget)))
      .reduce(BigDecimal::add).orElse(BigDecimal.ZERO).doubleValue();
  }


  public static String combineCqlExpressions(String operator, String... expressions) {
    if (ArrayUtils.isEmpty(expressions)) {
      return EMPTY;
    }

    String sorting = EMPTY;

    // Check whether last expression contains sorting query. If it does, extract it to be added in the end of the resulting query
    Matcher matcher = CQL_SORT_BY_PATTERN.matcher(expressions[expressions.length - 1]);
    if (matcher.find()) {
      expressions[expressions.length - 1] = matcher.group(1);
      sorting = matcher.group(2);
    }

    return StreamEx.of(expressions)
      .filter(StringUtils::isNotBlank)
      .joining(") " + operator + " (", "(", ")") + sorting;
  }
}
