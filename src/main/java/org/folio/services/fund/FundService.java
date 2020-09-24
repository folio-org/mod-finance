package org.folio.services.fund;

import static org.folio.rest.RestConstants.NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.FUND_NOT_FOUND_ERROR;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Fund;

public class FundService {
  private final RestClient fundStorageRestClient;

  public FundService(RestClient fundStorageRestClient) {
    this.fundStorageRestClient = fundStorageRestClient;
  }

  public CompletableFuture<Fund> retrieveFundById(String fundId, RequestContext requestContext) {
    return fundStorageRestClient.getById(fundId, requestContext, Fund.class)
                                .exceptionally(t -> {
                                  if (t instanceof HttpException) {
                                    HttpException httpException = (HttpException) t;
                                    if (httpException.getCode() == NOT_FOUND) {
                                      Error error = new Error().withCode(FUND_NOT_FOUND_ERROR.getCode()).withMessage(String.format(FUND_NOT_FOUND_ERROR.getDescription(), fundId));
                                      throw new CompletionException(new HttpException(NOT_FOUND, error));
                                    }
                                  }
                                  throw new CompletionException(t);
                                });
  }
}
