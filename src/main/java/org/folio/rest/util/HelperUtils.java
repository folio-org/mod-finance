package org.folio.rest.util;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.ws.rs.Path;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.client.ConfigurationsClient;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;


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

  private static final String ERROR_MESSAGE = "errorMessage";

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

  public static CompletableFuture<JsonObject> handleGetRequest(String endpoint, HttpClientInterface httpClient, Context ctx,
      Map<String, String> okapiHeaders, Logger logger) {
    CompletableFuture<JsonObject> future = new VertxCompletableFuture<>(ctx);
    try {
      logger.info(CALLING_ENDPOINT_MSG, HttpMethod.GET, endpoint);

      httpClient.request(HttpMethod.GET, endpoint, okapiHeaders)
        .thenApply(response -> {
          logger.debug("Validating response for GET {}", endpoint);
          return verifyAndExtractBody(response);
        })
        .thenAccept(body -> {
          if (logger.isInfoEnabled()) {
            logger.info("The response body for GET {}: {}", endpoint, nonNull(body) ? body.encodePrettily() : null);
          }
          future.complete(body);
        })
        .exceptionally(t -> {
          logger.error(EXCEPTION_CALLING_ENDPOINT_MSG, t, HttpMethod.GET, endpoint);
          future.completeExceptionally(t);
          return null;
        });
    } catch (Exception e) {
      logger.error(EXCEPTION_CALLING_ENDPOINT_MSG, e, HttpMethod.GET, endpoint);
      future.completeExceptionally(e);
    }
    return future;
  }

  public static void verifyResponse(Response response) {
    if (!Response.isSuccess(response.getCode())) {
      throw new CompletionException(new HttpException(response.getCode(), response.getError()
        .getString(ERROR_MESSAGE)));
    }
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
}
