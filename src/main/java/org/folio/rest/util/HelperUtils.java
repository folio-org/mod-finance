package org.folio.rest.util;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.ErrorCodes.NEGATIVE_VALUE;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.Path;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.exception.HttpException;
import org.folio.rest.helper.AbstractHelper;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;

public final class HelperUtils {
  public static final String ID = "id";
  public static final String OKAPI_URL = "X-Okapi-Url";
  private static final String ERROR_CAUSE = "cause";
  private static final Pattern CQL_SORT_BY_PATTERN = Pattern.compile("(.*)(\\ssortBy\\s.*)", Pattern.CASE_INSENSITIVE);

  private HelperUtils() {
  }

  public static JsonObject convertToJson(Object data) {
    return data instanceof JsonObject jsonObject ? jsonObject : JsonObject.mapFrom(data);
  }

  /**
   * @param query  string representing CQL query
   * @return URL encoded string
   */
  public static String encodeQuery(String query) {
    return URLEncoder.encode(query, StandardCharsets.UTF_8);
  }


  public static Optional<ErrorCodes> getErrorByCode(String errorCode){
    return EnumSet.allOf(ErrorCodes.class).stream()
      .filter(errorCodes -> errorCodes.getCode().equals(errorCode))
      .findAny();
  }


  public static String getEndpoint(Class<?> clazz) {
    return clazz.getAnnotation(Path.class).value();
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
  public static <T> Future<List<T>> collectResultsOnSuccess(List<Future<T>> futures) {
    return GenericCompositeFuture.join(new ArrayList<>(futures))
      .map(CompositeFuture::list);
  }


  public static Void handleErrorResponse(Handler<AsyncResult<javax.ws.rs.core.Response>> handler, AbstractHelper helper, Throwable t) {
    handler.handle(succeededFuture(helper.buildErrorResponse(t)));
    return null;

  }

  public static <T> boolean isJsonOfType(String errorMessage, Class<T> clazz) {
    try {
      Json.decodeValue(errorMessage, clazz);
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  public static int defineErrorCode(Throwable throwable) {
    if (throwable instanceof HttpException httpException) {
      return httpException.getCode();
    }
    return INTERNAL_SERVER_ERROR.getStatusCode();
  }

  public static Errors convertToErrors(Throwable throwable) {
    final Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
    Errors errors;

    if (cause instanceof HttpException httpException) {
      errors = httpException.getErrors();
      List<Error> errorList = errors.getErrors().stream()
        .map(HelperUtils::mapToError)
        .collect(Collectors.toList());
      errors.setErrors(errorList);
    } else {
      errors = new Errors().withErrors(Collections.singletonList(GENERIC_ERROR_CODE.toError()
        .withAdditionalProperty(ERROR_CAUSE, cause.getMessage())))
        .withTotalRecords(1);
    }
    return errors;
  }

  private static Error mapToError(Error error) {
    if (isJsonOfType(error.getMessage(), Error.class)) {
      return new JsonObject(error.getMessage()).mapTo(Error.class);
    }
    return error;
  }

  public static Errors mapToErrors(String errorStr) {
    return new JsonObject(errorStr).mapTo(Errors.class);
  }

  public static javax.ws.rs.core.Response.ResponseBuilder createResponseBuilder(int code) {
    return switch (code) {
      case 400, 403, 404, 409, 422 -> javax.ws.rs.core.Response.status(code);
      default -> javax.ws.rs.core.Response.status(INTERNAL_SERVER_ERROR);
    };
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

  public static Future<Void> unsupportedOperationExceptionFuture() {
    return Future.failedFuture(new UnsupportedOperationException());
  }

  public static <T> double calculateTotals(List<T> budgets, ToDoubleFunction<T> getDouble) {
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
