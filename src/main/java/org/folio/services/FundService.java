package org.folio.services;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Fund;
import java.util.concurrent.CompletableFuture;

public class FundService {
  private final RestClient fundStorageRestClient;

  public FundService(RestClient fundStorageRestClient) {
    this.fundStorageRestClient = fundStorageRestClient;
  }

  public CompletableFuture<Fund> retrieveFundById(String fundId, RequestContext requestContext) {
    return fundStorageRestClient.getById(fundId, requestContext, Fund.class);
  }
}
