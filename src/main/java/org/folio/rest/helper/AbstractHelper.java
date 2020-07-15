package org.folio.rest.helper;

import static java.util.Objects.nonNull;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.util.HelperUtils.CALLING_ENDPOINT_MSG;
import static org.folio.rest.util.HelperUtils.EXCEPTION_CALLING_ENDPOINT_MSG;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.converToError;
import static org.folio.rest.util.HelperUtils.convertToJson;
import static org.folio.rest.util.HelperUtils.createResponseBuilder;
import static org.folio.rest.util.HelperUtils.defineErrorCode;
import static org.folio.rest.util.HelperUtils.verifyAndExtractBody;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.HelperUtils;

import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public abstract class AbstractHelper {
  static final String ERROR_CAUSE = "cause";
  static final String EXCEPTION_CALLING_ENDPOINT_WITH_BODY_MSG = "{} {} request failed. Request body: {}";
  static final String CALLING_ENDPOINT_WITH_BODY_MSG = "Sending {} {} with body: {}";
  static final String SEARCH_PARAMS = "?limit=%s&offset=%s%s&lang=%s";

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  protected final String lang;
  protected final Context ctx;
  protected final HttpClientInterface httpClient;
  protected final Map<String, String> okapiHeaders;
  private final Errors processingErrors = new Errors();


  AbstractHelper(HttpClientInterface httpClient, Map<String, String> okapiHeaders, Context ctx, String lang) {
    setDefaultHeaders(httpClient);
    this.httpClient = httpClient;
    this.okapiHeaders = okapiHeaders;
    this.ctx = ctx;
    this.lang = lang;
  }

  AbstractHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    this.httpClient = getHttpClient(okapiHeaders, true);
    this.okapiHeaders = okapiHeaders;
    this.ctx = ctx;
    this.lang = lang;
  }

  protected AbstractHelper(Context ctx) {
    this.httpClient = null;
    this.okapiHeaders = null;
    this.lang = null;
    this.ctx = ctx;
  }


  public static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders, boolean setDefaultHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

    HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);

    // Some requests do not have body and in happy flow do not produce response body. The Accept header is required for calls to storage
    if (setDefaultHeaders) {
      setDefaultHeaders(httpClient);
    }
    return httpClient;
  }

  /**
   * Some requests do not have body and might not produce response body. The Accept header is required for calls to storage
   */
  private static void setDefaultHeaders(HttpClientInterface httpClient) {
    httpClient.setDefaultHeaders(Collections.singletonMap("Accept", APPLICATION_JSON + ", " + TEXT_PLAIN));
  }

  public void closeHttpClient() {
    if (Objects.nonNull(httpClient)) {
      httpClient.closeClient();
    }
  }

  public List<Error> getErrors() {
    return processingErrors.getErrors();
  }

  protected Errors getProcessingErrors() {
    processingErrors.setTotalRecords(processingErrors.getErrors().size());
    return processingErrors;
  }

  public void addProcessingError(Error error) {
    processingErrors.getErrors().add(error);
  }

  /**
   * A common method to create a new entry in the storage based on the Json Object.
   *
   * @param endpoint   endpoint
   * @param recordData json to post
   * @return completable future holding id of newly created entity Record or an exception if process failed
   */
  protected CompletableFuture<String> handleCreateRequest(String endpoint, Object recordData) {
    CompletableFuture<String> future = new VertxCompletableFuture<>(ctx);
    try {
      JsonObject json = convertToJson(recordData);

      if (logger.isDebugEnabled()) {
        logger.debug(CALLING_ENDPOINT_WITH_BODY_MSG, HttpMethod.POST, endpoint, json.encodePrettily());
      }

      httpClient.request(HttpMethod.POST, json.toBuffer(), endpoint, okapiHeaders)
        .thenApply(this::verifyAndExtractRecordId)
        .thenAccept(id -> {
          future.complete(id);
          logger.debug("'POST {}' request successfully processed. Record with '{}' id has been created", endpoint, id);
        })
        .exceptionally(throwable -> {
          future.completeExceptionally(throwable);
          logger.error(EXCEPTION_CALLING_ENDPOINT_WITH_BODY_MSG, throwable, HttpMethod.POST, endpoint, json.encodePrettily());
          return null;
        });
    } catch (Exception e) {
      future.completeExceptionally(e);
    }

    return future;
  }

  /**
   * A common method to update an entry in the storage
   *
   * @param endpoint   endpoint
   * @param recordData json to use for update operation
   */
  protected CompletableFuture<Void> handleUpdateRequest(String endpoint, Object recordData) {
    CompletableFuture<Void> future = new VertxCompletableFuture<>(ctx);
    try {
      JsonObject json = convertToJson(recordData);

      if (logger.isDebugEnabled()) {
        logger.debug(CALLING_ENDPOINT_WITH_BODY_MSG, HttpMethod.PUT, endpoint, json.encodePrettily());
      }

      httpClient.request(HttpMethod.PUT, json.toBuffer(), endpoint, okapiHeaders)
        .thenApply(HelperUtils::verifyAndExtractBody)
        .thenAccept(response -> {
          logger.debug("'PUT {}' request successfully processed", endpoint);
          future.complete(null);
        })
        .exceptionally(e -> {
          future.completeExceptionally(e);
          logger.error(EXCEPTION_CALLING_ENDPOINT_WITH_BODY_MSG, e, HttpMethod.PUT, endpoint, json.encodePrettily());
          return null;
        });
    } catch (Exception e) {
      logger.error(EXCEPTION_CALLING_ENDPOINT_WITH_BODY_MSG, e, HttpMethod.PUT, endpoint, recordData);
      future.completeExceptionally(e);
    }

    return future;
  }

  /**
   * A common method to delete an entry in the storage
   *
   * @param endpoint endpoint
   */
  protected CompletableFuture<Void> handleDeleteRequest(String endpoint) {
    CompletableFuture<Void> future = new VertxCompletableFuture<>(ctx);

    logger.debug(CALLING_ENDPOINT_MSG, HttpMethod.DELETE, endpoint);

    try {
      httpClient.request(HttpMethod.DELETE, endpoint, okapiHeaders)
        .thenAccept(HelperUtils::verifyResponse)
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


  public CompletableFuture<JsonObject> handleGetRequest(String endpoint) {
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


  private String verifyAndExtractRecordId(org.folio.rest.tools.client.Response response) {
    logger.debug("Validating received response");

    JsonObject body = verifyAndExtractBody(response);

    if (body != null && body.containsKey(ID)) {
      return body.getString(ID);
    }

    String location = response.getHeaders().get(LOCATION);
    return location.substring(location.lastIndexOf('/') + 1);
  }

  protected int handleProcessingError(Throwable throwable) {
    logger.error("Exception encountered", throwable.getCause());
    if (getErrors().isEmpty()) {
      final Error error = converToError(throwable);
      addProcessingError(error);
    }
    return defineErrorCode(throwable);
  }

  public Response buildErrorResponse(Throwable throwable) {
    return buildErrorResponse(handleProcessingError(throwable));
  }

  public Response buildErrorResponse(int code) {
    final Response.ResponseBuilder responseBuilder = createResponseBuilder(code);
    closeHttpClient();

    return responseBuilder.header(CONTENT_TYPE, APPLICATION_JSON)
      .entity(getProcessingErrors())
      .build();
  }

  public Response buildOkResponse(Object body) {
    closeHttpClient();
    return Response.ok(body, APPLICATION_JSON).build();
  }

  public Response buildNoContentResponse() {
    closeHttpClient();
    return Response.noContent().build();
  }

  public Response buildResponseWithLocation(String endpoint, Object body) {
    closeHttpClient();
    // Adding relative endpoint as location header because "x-okapi-url" might contain private <schema:ip:port>
    return Response.created(URI.create(endpoint))
      .header(CONTENT_TYPE, APPLICATION_JSON)
      .entity(body)
      .build();
  }
}
