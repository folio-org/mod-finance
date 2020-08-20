package org.folio.services;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;

public class FiscalYearService {

  private final RestClient fiscalYearRestClient;

  public FiscalYearService(RestClient fiscalYearRestClient) {
    this.fiscalYearRestClient = fiscalYearRestClient;
  }


  public CompletableFuture<FiscalYearsCollection> getFiscalYears(int limit, int offset, String query, RequestContext requestContext) {
    return fiscalYearRestClient.get(query, offset, limit, requestContext, FiscalYearsCollection.class);
  }

  public CompletableFuture<FiscalYear> getFiscalYear(String fiscalYearId, RequestContext requestContext) {
    return fiscalYearRestClient.getById(fiscalYearId, requestContext, FiscalYear.class);
  }


}
