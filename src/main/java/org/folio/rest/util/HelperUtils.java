package org.folio.rest.util;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.ErrorCodes.INVALID_TRANSACTION_TYPE;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.finance.LedgerFY;
import org.folio.rest.client.ConfigurationsClient;
import org.folio.rest.exception.HttpException;
import org.folio.rest.helper.AbstractHelper;
import org.folio.rest.helper.TransactionsHelper;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.tools.client.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import one.util.streamex.StreamEx;


public class HelperUtils {
  public static final String ID = "id";
  public static final String LANG = "lang";
  public static final String OKAPI_URL = "X-Okapi-Url";
  public static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {}";
  public static final String CALLING_ENDPOINT_MSG = "Sending {} {}";
  private static final String CONFIG_QUERY = "(module=ORG and configName=localeSettings)";
  public static final String CONFIGS = "configs";
  public static final String CONFIG_NAME = "configName";
  public static final String CONFIG_VALUE = "value";

  private static final String ERROR_CAUSE = "cause";
  private static final String ERROR_MESSAGE = "errorMessage";
  private static final String INTERNAL_SERVER_ERROR_CODE = String.valueOf(INTERNAL_SERVER_ERROR.getStatusCode());

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


  /**
   * Retrieve configuration for localeSettings from mod-configuration.
   * @param okapiHeaders the headers provided by okapi
   * @param ctx the context
   * @param logger logger instance
   * @return CompletableFuture with JsonObject
   */
  public static CompletableFuture<JsonObject> getConfigurationEntries(Map<String, String> okapiHeaders, Context ctx,
      Logger logger) {

    String okapiURL = StringUtils.trimToEmpty(okapiHeaders.get(OKAPI_URL));
    String tenant = okapiHeaders.get(OKAPI_HEADER_TENANT);
    String token = okapiHeaders.get(OKAPI_HEADER_TOKEN);
    CompletableFuture<JsonObject> future = new VertxCompletableFuture<>(ctx);
    JsonObject config = new JsonObject();

    ConfigurationsClient configurationsClient = new ConfigurationsClient(okapiURL, tenant, token);
    try {
      configurationsClient.getConfigurationsEntries(CONFIG_QUERY, 0, 100, null, null, response -> response.bodyHandler(body -> {
        if (response.statusCode() != 200) {
          logger.error(String.format("Expected status code 200, got '%s' :%s", response.statusCode(), body.toString()));
          future.completeExceptionally(new HttpException(500, ErrorCodes.CURRENCY_NOT_FOUND));
        }

        JsonObject entries = body.toJsonObject();

        if (logger.isDebugEnabled()) {
          logger.debug("The response from mod-configuration: {}", entries.encodePrettily());
        }
        entries.getJsonArray(CONFIGS)
          .stream()
          .map(o -> (JsonObject) o)
          .forEach(entry -> config.put(entry.getString(CONFIG_NAME), entry.getValue(CONFIG_VALUE)));

        future.complete(config);
      }));
    } catch (Exception e) {
      logger.error("Error while fetching configs", e);
      future.completeExceptionally(e);
    }
    return future;
  }

  public static Void handleErrorResponse(Handler<AsyncResult<javax.ws.rs.core.Response>> handler, AbstractHelper helper, Throwable t) {
    handler.handle(succeededFuture(helper.buildErrorResponse(t)));
    return null;

  }

  public static void handleTransactionError(TransactionsHelper helper, Handler<AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler) {
    helper.addProcessingError(INVALID_TRANSACTION_TYPE.toError());
    asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
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

  public static Error convertToError(Throwable cause) {
    final Error error;
    if (cause instanceof HttpException) {
      error = ((HttpException) cause).getError();
      if (StringUtils.isEmpty(error.getCode())) {
        error.withCode(INTERNAL_SERVER_ERROR_CODE);
      }
    } else {
      error = GENERIC_ERROR_CODE.toError()
        .withAdditionalProperty(ERROR_CAUSE, cause.getMessage())
        .withCode(String.valueOf(INTERNAL_SERVER_ERROR_CODE));
    }
    return error;
  }
}
