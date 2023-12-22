package org.folio.rest.helper;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.HelperUtils.convertToErrors;
import static org.folio.rest.util.HelperUtils.createResponseBuilder;
import static org.folio.rest.util.HelperUtils.defineErrorCode;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;

import io.vertx.core.Context;

public abstract class AbstractHelper {

  protected final Logger logger = LogManager.getLogger(this.getClass());
  protected final Context ctx;
  protected final Map<String, String> okapiHeaders;
  private final Errors processingErrors = new Errors();

  AbstractHelper(Map<String, String> okapiHeaders, Context ctx) {
    this.okapiHeaders = okapiHeaders;
    this.ctx = ctx;
  }

  protected AbstractHelper(Context ctx) {
    this.okapiHeaders = null;
    this.ctx = ctx;
  }

  /**
   * Some requests do not have body and might not produce response body. The Accept header is required for calls to storage
   */
  public List<Error> getErrors() {
    return processingErrors.getErrors();
  }

  protected Errors getProcessingErrors() {
    processingErrors.setTotalRecords(processingErrors.getErrors()
      .size());
    return processingErrors;
  }

  public void addProcessingError(Error error) {
    processingErrors.getErrors()
      .add(error);
  }

  protected int handleProcessingError(Throwable throwable) {
    logger.error("Exception encountered", throwable);
    if (getErrors().isEmpty()) {
      final Errors errors = convertToErrors(throwable);
      addProcessingError(errors);
    }
    return defineErrorCode(throwable);
  }

  private void addProcessingError(Errors errors) {
    processingErrors.getErrors()
      .addAll(errors.getErrors());
  }

  public Response buildErrorResponse(Throwable throwable) {
    return buildErrorResponse(handleProcessingError(throwable));
  }

  public Response buildErrorResponse(int code) {
    final Response.ResponseBuilder responseBuilder = createResponseBuilder(code);

    return responseBuilder.header(CONTENT_TYPE, APPLICATION_JSON)
      .entity(getProcessingErrors())
      .build();
  }

  public Response buildOkResponse(Object body) {
    return Response.ok(body, APPLICATION_JSON)
      .build();
  }

  public Response buildNoContentResponse() {
    return Response.noContent()
      .build();
  }

  public Response buildResponseWithLocation(String endpoint, Object body) {
    // Adding relative endpoint as location header because "x-okapi-url" might contain private <schema:ip:port>
    return Response.created(URI.create(endpoint))
      .header(CONTENT_TYPE, APPLICATION_JSON)
      .entity(body)
      .build();
  }
}
