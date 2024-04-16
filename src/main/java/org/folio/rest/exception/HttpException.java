package org.folio.rest.exception;

import static org.folio.rest.util.ErrorCodes.CONFLICT;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;

import java.io.Serial;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.HelperUtils;

public class HttpException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 8109197948434861504L;

  private final int code;
  private final transient Errors errors;

  public HttpException(int code, String message) {
    super(StringUtils.isNotEmpty(message) ? message : GENERIC_ERROR_CODE.getDescription());
    this.code = code;
    ErrorCodes ec = code == 409 ? CONFLICT : GENERIC_ERROR_CODE;
    this.errors = new Errors()
      .withErrors(Collections.singletonList(new Error().withCode(ec.getCode()).withMessage(message)))
      .withTotalRecords(1);
  }

  public HttpException(int code, ErrorCodes errCodes) {
    super(errCodes.getDescription());
    this.errors = new Errors()
      .withErrors(Collections.singletonList(new Error().withCode(errCodes.getCode()).withMessage(errCodes.getDescription())))
      .withTotalRecords(1);
    this.code = code;
  }

  public HttpException(int code, ErrorCodes errCodes, Throwable cause) {
    super(errCodes.getDescription());
    Error error = new Error()
      .withCode(errCodes.getCode())
      .withMessage(errCodes.getDescription());
    if (cause.getMessage() != null) {
      Parameter causeParam = new Parameter()
        .withKey("cause")
        .withValue(cause.getMessage());
      error.setParameters(Collections.singletonList(causeParam));
    }
    this.errors = new Errors()
      .withErrors(Collections.singletonList(error))
      .withTotalRecords(1);
    this.code = code;
  }

  public HttpException(int code, Error error) {
    super(HelperUtils.errorAsString(error));
    this.code = code;
    this.errors = new Errors()
      .withErrors(Collections.singletonList(error))
      .withTotalRecords(1);
  }

  public HttpException(int code, Errors errors) {
    super(HelperUtils.errorsAsString(errors));
    this.code = code;
    this.errors = errors;
  }

  public int getCode() {
    return code;
  }

  public Errors getErrors() {
    return errors;
  }
}
