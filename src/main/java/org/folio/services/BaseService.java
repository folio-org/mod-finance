package org.folio.services;

import static java.util.Objects.nonNull;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.util.ResourcePathResolver.ACQUISITIONS_MEMBERSHIPS;
import static org.folio.rest.util.ResourcePathResolver.ACQUISITIONS_UNITS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exception.HttpException;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import one.util.streamex.StreamEx;

public abstract class BaseService {
  public final Logger logger = LoggerFactory.getLogger(this.getClass());

  public static final String SEARCH_PARAMS = "?limit=%s&offset=%s%s&lang=%s";
  private static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {}";
  private static final String CALLING_ENDPOINT_MSG = "Sending {} {}";
  private static final String ERROR_MESSAGE = "errorMessage";
  private static final String ID = "id";
  public static final String ACQUISITIONS_UNIT_ID = "acquisitionsUnitId";
  public static final String IS_DELETED_PROP = "isDeleted";
  public static final String ALL_UNITS_CQL = IS_DELETED_PROP + "=*";
  public static final String ACTIVE_UNITS_CQL = IS_DELETED_PROP + "==false";
  private static final Pattern CQL_SORT_BY_PATTERN = Pattern.compile("(.*)(\\ssortBy\\s.*)", Pattern.CASE_INSENSITIVE);
  public static final String ACQUISITIONS_UNIT_IDS = "acqUnitIds";
  public static final String NO_ACQ_UNIT_ASSIGNED_CQL = "cql.allRecords=1 not " + ACQUISITIONS_UNIT_IDS + " <> []";
  public static final String GET_UNITS_BY_QUERY = resourcesPath(ACQUISITIONS_UNITS) + SEARCH_PARAMS;
  public static final String GET_UNITS_MEMBERSHIPS_BY_QUERY = resourcesPath(ACQUISITIONS_MEMBERSHIPS) + SEARCH_PARAMS;

  public static String buildQuery(String query, Logger logger) {
    return isEmpty(query) ? EMPTY : "&query=" + encodeQuery(query, logger);
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

  /**
   * Some requests do not have body and in happy flow do not produce response body. The Accept header is required for calls to
   * storage
   */
  private static void setDefaultHeaders(HttpClientInterface httpClient) {
    // The RMB's HttpModuleClient2.ACCEPT is in sentence case. Using the same format to avoid duplicates (issues migrating to RMB
    // 27.1.1)
    httpClient.setDefaultHeaders(Collections.singletonMap("Accept", APPLICATION_JSON + ", " + TEXT_PLAIN));
  }

  public HttpClientInterface getHttpClient(Map<String, String> okapiHeaders, boolean setDefaultHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

    HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);

    // Some requests do not have body and in happy flow do not produce response body. The Accept header is required for calls to
    // storage
    if (setDefaultHeaders) {
      setDefaultHeaders(httpClient);
    }
    return httpClient;
  }

  public HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    return getHttpClient(okapiHeaders, true);
  }

  /**
   * A common method to create a new entry in the storage based on the Json Object.
   *
   * @param recordData json to post
   * @return completable future holding id of newly created entity Record or an exception if process failed
   */
  public CompletableFuture<String> handlePostRequest(JsonObject recordData, String endpoint, HttpClientInterface httpClient,
      Context ctx, Map<String, String> okapiHeaders, Logger logger) {
    CompletableFuture<String> future = new VertxCompletableFuture<>(ctx);
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Sending 'POST {}' with body: {}", endpoint, recordData.encodePrettily());
      }
      httpClient.request(HttpMethod.POST, recordData.toBuffer(), endpoint, okapiHeaders)
        .thenApply(this::verifyAndExtractRecordId)
        .thenAccept(id -> {
          future.complete(id);
          logger.debug("'POST {}' request successfully processed. Record with '{}' id has been created", endpoint, id);
        })
        .exceptionally(throwable -> {
          future.completeExceptionally(throwable);
          logger.error("'POST {}' request failed. Request body: {}", throwable, endpoint, recordData.encodePrettily());
          return null;
        });
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  public CompletableFuture<JsonObject> handleGetRequest(String endpoint, HttpClientInterface httpClient, Context ctx,
      Map<String, String> okapiHeaders, Logger logger) {

    CompletableFuture<JsonObject> future = new VertxCompletableFuture<>(ctx);
    try {
      logger.info("Calling GET {}", endpoint);
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

  /**
   * A common method to update an entry in the storage
   *
   * @param recordData json to use for update operation
   * @param endpoint   endpoint
   */
  public CompletableFuture<Void> handlePutRequest(String endpoint, JsonObject recordData, HttpClientInterface httpClient,
      Context ctx, Map<String, String> okapiHeaders, Logger logger) {
    CompletableFuture<Void> future = new VertxCompletableFuture<>(ctx);
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Sending 'PUT {}' with body: {}", endpoint, recordData.encodePrettily());
      }
      httpClient.request(HttpMethod.PUT, recordData.toBuffer(), endpoint, okapiHeaders)
        .thenApply(this::verifyAndExtractBody)
        .thenAccept(response -> {
          logger.debug("'PUT {}' request successfully processed", endpoint);
          future.complete(null);
        })
        .exceptionally(e -> {
          future.completeExceptionally(e);
          logger.error("'PUT {}' request failed. Request body: {}", e, endpoint, recordData.encodePrettily());
          return null;
        });
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  /**
   * A common method to delete an entry in the storage
   *
   * @param endpoint endpoint
   */
  public CompletableFuture<Void> handleDeleteRequest(String endpoint, HttpClientInterface httpClient, Context ctx,
      Map<String, String> okapiHeaders, Logger logger) {
    CompletableFuture<Void> future = new VertxCompletableFuture<>(ctx);
    logger.debug(CALLING_ENDPOINT_MSG, HttpMethod.DELETE, endpoint);
    try {
      httpClient.request(HttpMethod.DELETE, endpoint, okapiHeaders)
        .thenAccept(this::verifyResponse)
        .thenApply(future::complete)
        .exceptionally(t -> {
          logger.error(EXCEPTION_CALLING_ENDPOINT_MSG, t, HttpMethod.DELETE, endpoint);
          future.completeExceptionally(t);
          return null;
        });
    } catch (Exception e) {
      logger.error(EXCEPTION_CALLING_ENDPOINT_MSG, e, HttpMethod.DELETE, endpoint);
      future.completeExceptionally(e);
    }
    return future;
  }

  public JsonObject verifyAndExtractBody(org.folio.rest.tools.client.Response response) {
    if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
      throw new HttpException(response.getCode(), response.getError()
        .getString(ERROR_MESSAGE));
    }
    return response.getBody();
  }

  public void verifyResponse(org.folio.rest.tools.client.Response response) {
    if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
      throw new CompletionException(new HttpException(response.getCode(), response.getError()
        .getString(ERROR_MESSAGE)));
    }
  }

  private String verifyAndExtractRecordId(org.folio.rest.tools.client.Response response) {
    JsonObject body = verifyAndExtractBody(response);
    String id;
    if (body != null && !body.isEmpty() && body.containsKey(ID)) {
      id = body.getString(ID);
    } else {
      String location = response.getHeaders()
        .get(LOCATION);
      id = location.substring(location.lastIndexOf('/') + 1);
    }
    return id;
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

  /**
   * Transform list of id's to CQL query using 'or' operation
   * @param ids list of id's
   * @return String representing CQL query to get records by id's
   */
  public static String convertIdsToCqlQuery(Collection<String> ids) {
    return convertIdsToCqlQuery(ids, ID, true);
  }

  public static String convertIdsToCqlQuery(Collection<String> values, String fieldName, boolean strictMatch) {
    String prefix = fieldName + (strictMatch ? "==(" : "=(");
    return StreamEx.of(values).joining(" or ", prefix, ")");
  }
}
