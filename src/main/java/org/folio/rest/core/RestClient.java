package org.folio.rest.core;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getErrorByCode;
import static org.folio.rest.util.HelperUtils.isJsonOfType;
import static org.folio.rest.util.HelperUtils.mapToErrors;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.WebClientFactory;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Errors;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ErrorConverter;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

public class RestClient {

  private static final Logger log = LogManager.getLogger();
  private static final ErrorConverter ERROR_CONVERTER = ErrorConverter.createFullBody(
    result -> {
      String errorResponse = result.response().bodyAsString();
      if (isJsonOfType(errorResponse, Errors.class)) {
        return new HttpException(result.response().statusCode(), mapToErrors(errorResponse));
      }
      else {
        return getErrorByCode(errorResponse)
          .map(errorCode -> new HttpException(result.response().statusCode(), errorCode))
          .orElse(new HttpException(result.response().statusCode(), errorResponse));
      }
    });
  protected static final ResponsePredicate SUCCESS_RESPONSE_PREDICATE =
    ResponsePredicate.create(ResponsePredicate.SC_SUCCESS, ERROR_CONVERTER);
  public static final String REQUEST_MESSAGE_LOG_INFO = "Calling {} {}";

  public <T> Future<T> post(String endpoint, T entity, Class<T> responseType, RequestContext requestContext) {
    log.info(REQUEST_MESSAGE_LOG_INFO, HttpMethod.POST, endpoint);
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.headers());
    return getVertxWebClient(requestContext.context())
      .postAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
      .putHeaders(caseInsensitiveHeader)
      .expect(SUCCESS_RESPONSE_PREDICATE)
      .sendJson(entity)
      .map(HttpResponse::bodyAsJsonObject)
      .map(body -> body.mapTo(responseType))
      .onFailure(log::error);
  }

  public <T> Future<Void> postEmptyResponse(String endpoint, T entity, RequestContext requestContext) {
    log.info(REQUEST_MESSAGE_LOG_INFO, HttpMethod.POST, endpoint);
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.headers());
    return getVertxWebClient(requestContext.context())
      .postAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
      .putHeaders(caseInsensitiveHeader)
      .expect(SUCCESS_RESPONSE_PREDICATE)
      .sendJson(entity)
      .onFailure(log::error)
      .mapEmpty();
  }

  protected MultiMap convertToCaseInsensitiveMap(Map<String, String> okapiHeaders) {
    return MultiMap.caseInsensitiveMultiMap()
      .addAll(okapiHeaders)
      // set default Accept header
      .add("Accept", APPLICATION_JSON + ", " + TEXT_PLAIN);
  }

  public <T> Future<Void> put(String endpoint, T dataObject, RequestContext requestContext) {
    log.info(REQUEST_MESSAGE_LOG_INFO, HttpMethod.PUT, endpoint);

    var recordData = JsonObject.mapFrom(dataObject);
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.headers());

    return getVertxWebClient(requestContext.context())
      .putAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
      .putHeaders(caseInsensitiveHeader)
      .expect(SUCCESS_RESPONSE_PREDICATE)
      .sendJson(recordData)
      .onFailure(log::error)
      .mapEmpty();
  }

  public <T> Future<T> put(String endpoint, T dataObject, Class<T> responseType ,RequestContext requestContext) {
    log.info(REQUEST_MESSAGE_LOG_INFO, HttpMethod.PUT, endpoint);

    var recordData = JsonObject.mapFrom(dataObject);
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.headers());

    return getVertxWebClient(requestContext.context())
      .putAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
      .putHeaders(caseInsensitiveHeader)
      .expect(SUCCESS_RESPONSE_PREDICATE)
      .sendJson(recordData)
      .map(HttpResponse::bodyAsJsonObject)
      .map(jsonObject -> jsonObject.mapTo(responseType))
      .onFailure(log::error);
  }

  public Future<Void> delete(String endpointById, boolean skipError404, RequestContext requestContext) {
    log.info(REQUEST_MESSAGE_LOG_INFO, HttpMethod.DELETE, endpointById);

    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.headers());
    Promise<Void> promise = Promise.promise();

    getVertxWebClient(requestContext.context())
      .deleteAbs(buildAbsEndpoint(caseInsensitiveHeader, endpointById))
      .putHeaders(caseInsensitiveHeader)
      .expect(SUCCESS_RESPONSE_PREDICATE)
      .send()
      .onSuccess(f -> promise.complete())
      .onFailure(t -> handleErrorResponse(promise, t, skipError404));

    return promise.future();
  }

  private <T> void handleGetMethodErrorResponse(Promise<T> promise, Throwable t, boolean skipError404) {
    if (skipError404 && t instanceof HttpException httpException && httpException.getCode() == 404) {
      log.warn(t);
      promise.complete();
    } else {
      log.error(t);
      promise.fail(t);
    }
  }

  private void handleErrorResponse(Promise<Void> promise, Throwable t, boolean skipError404) {
    if (skipError404 && t instanceof HttpException httpException && httpException.getCode() == 404){
      log.warn(t);
      promise.complete();
    } else {
      log.error(t);
      promise.fail(t);
    }
  }

  public Future<Void> delete(String endpoint, RequestContext requestContext) {
    return delete(endpoint, false, requestContext);
  }

  public <T> Future<T> get(String endpoint, Class<T> responseType, RequestContext requestContext) {
    return get(endpoint, false, responseType, requestContext);
  }

  public <T> Future<T> get(String endpoint, boolean skipError404, Class<T> responseType, RequestContext requestContext) {
    log.info(REQUEST_MESSAGE_LOG_INFO, HttpMethod.GET, endpoint);
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.headers());
    var absEndpoint = buildAbsEndpoint(caseInsensitiveHeader, endpoint);

    Promise<T> promise = Promise.promise();
    getVertxWebClient(requestContext.context())
      .getAbs(absEndpoint)
      .putHeaders(caseInsensitiveHeader)
      .expect(SUCCESS_RESPONSE_PREDICATE)
      .send()
      .map(HttpResponse::bodyAsJsonObject)
      .map(jsonObject -> jsonObject.mapTo(responseType))
      .onSuccess(promise::complete)
      .onFailure(t -> handleGetMethodErrorResponse(promise, t, skipError404));

    return promise.future();
  }

  protected WebClient getVertxWebClient(Context context) {
    WebClientOptions options = new WebClientOptions();
    options.setLogActivity(true);
    options.setKeepAlive(true);
    options.setConnectTimeout(2000);
    options.setIdleTimeout(5000);
    options.setTryUseCompression(true);

    return WebClientFactory.getWebClient(context.owner(), options);
  }

  protected String buildAbsEndpoint(MultiMap okapiHeaders, String endpoint) {
    var okapiURL = okapiHeaders.get(OKAPI_URL);
    return okapiURL + endpoint;
  }

}
