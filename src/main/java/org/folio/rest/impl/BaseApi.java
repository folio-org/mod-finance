package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.HelperUtils.converToError;
import static org.folio.rest.util.HelperUtils.createResponseBuilder;
import static org.folio.rest.util.HelperUtils.defineErrorCode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BaseApi {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public Response buildOkResponse(Object body) {
    return Response.ok(body, APPLICATION_JSON)
      .build();
  }

  public Response buildNoContentResponse() {
    return Response.noContent()
      .build();
  }

  public Response buildResponseWithLocation(String okapi, String endpoint, Object body) {
    try {
      return Response.created(new URI(okapi + endpoint))
        .header(CONTENT_TYPE, APPLICATION_JSON)
        .entity(body)
        .build();
    } catch (URISyntaxException e) {
      return Response.created(URI.create(endpoint))
        .header(CONTENT_TYPE, APPLICATION_JSON)
        .header(LOCATION, endpoint)
        .entity(body)
        .build();
    }
  }

  public Void handleErrorResponse(Handler<AsyncResult<Response>> asyncResultHandler, Throwable t) {
    asyncResultHandler.handle(succeededFuture(buildErrorResponse(t)));
    return null;
  }

  public Response buildErrorResponse(Throwable throwable) {
    logger.error("Exception encountered", throwable.getCause());
    final int code = defineErrorCode(throwable);
    final Error error = converToError(throwable);
    final Response.ResponseBuilder responseBuilder = createResponseBuilder(code);
    return responseBuilder.header(CONTENT_TYPE, APPLICATION_JSON)
      .entity(new Errors().withErrors(Collections.singletonList(error)))
      .build();
  }
}
