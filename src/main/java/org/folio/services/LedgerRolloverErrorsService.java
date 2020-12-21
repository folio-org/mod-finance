package org.folio.services;

import org.folio.rest.acq.model.finance.LedgerFiscalYearRolloverErrorCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;

import java.util.concurrent.CompletableFuture;

public class LedgerRolloverErrorsService {
  private final RestClient ledgerRolloverErrorsStorageRestClient;

  public LedgerRolloverErrorsService(RestClient ledgerRolloverErrorsStorageRestClient) {
    this.ledgerRolloverErrorsStorageRestClient = ledgerRolloverErrorsStorageRestClient;
  }

  public CompletableFuture<LedgerFiscalYearRolloverErrorCollection> retrieveLedgersRolloverErrors(String query, int offset, int limit, RequestContext requestContext) {
    return ledgerRolloverErrorsStorageRestClient.get(query, offset, limit, requestContext, LedgerFiscalYearRolloverErrorCollection.class);
  }

}
