package org.folio.rest.helper;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.HelperUtils.CALLING_ENDPOINT_MSG;
import static org.folio.rest.util.HelperUtils.EXCEPTION_CALLING_ENDPOINT_MSG;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.convertToJson;
import static org.folio.rest.util.HelperUtils.verifyAndExtractBody;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.HelperUtils;

import io.vertx.core.Context;
import io.vertx.core.http.HttpHeaders;
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

  AbstractHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    this.httpClient = getHttpClient(okapiHeaders);
    this.okapiHeaders = okapiHeaders;
    this.ctx = ctx;
    this.lang = lang;
  }

  public static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

    HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);

    setDefaultHeaders(httpClient);
    return httpClient;
  }

  /**
   * Some requests do not have body and might not produce response body. The Accept header is required for calls to storage
   */
  private static void setDefaultHeaders(HttpClientInterface httpClient) {
    Map<String, String> customHeader = new HashMap<>();
    customHeader.put(HttpHeaders.ACCEPT.toString(), APPLICATION_JSON + ", " + TEXT_PLAIN);
    httpClient.setDefaultHeaders(customHeader);
  }

  public void closeHttpClient() {
    httpClient.closeClient();
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
    final Throwable cause = throwable.getCause();
    logger.error("Exception encountered", cause);
    final Error error;
    final int code;

    if (cause instanceof HttpException) {
      code = ((HttpException) cause).getCode();
      error = ((HttpException) cause).getError();
    } else {
      code = INTERNAL_SERVER_ERROR.getStatusCode();
      error = GENERIC_ERROR_CODE.toError()
        .withAdditionalProperty(ERROR_CAUSE, cause.getMessage());
    }

    if (getErrors().isEmpty()) {
      addProcessingError(error);
    }

    return code;
  }

  public Response buildErrorResponse(Throwable throwable) {
    return buildErrorResponse(handleProcessingError(throwable));
  }

  public Response buildErrorResponse(int code) {
    final Response.ResponseBuilder responseBuilder;
    switch (code) {
    case 400:
    case 403:
    case 404:
    case 422:
      responseBuilder = Response.status(code);
      break;
    default:
      responseBuilder = Response.status(INTERNAL_SERVER_ERROR);
    }
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
