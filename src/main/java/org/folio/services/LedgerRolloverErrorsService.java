package org.folio.services;

import org.folio.rest.acq.model.finance.LedgerFiscalYearRolloverErrorCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;

import java.util.concurrent.CompletableFuture;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class LedgerRolloverErrorsService {
  private final RestClient ledgerRolloverErrorsStorageRestClient;

  public LedgerRolloverErrorsService(RestClient ledgerRolloverErrorsStorageRestClient) {
    this.ledgerRolloverErrorsStorageRestClient = ledgerRolloverErrorsStorageRestClient;
  }

  public CompletableFuture<LedgerFiscalYearRolloverErrorCollection> retrieveLedgersRolloverErrors(String query,
                                                                                                  int offset,
                                                                                                  int limit,
                                                                                                  String contentType,
                                                                                                  RequestContext requestContext) {
    if (contentType.toLowerCase().contains(APPLICATION_JSON.toLowerCase())) {
      return ledgerRolloverErrorsStorageRestClient.get(query, offset, limit, requestContext, LedgerFiscalYearRolloverErrorCollection.class);
    } else {
      throw new HttpException(415, "Unsupported Media Type: " + contentType);
    }
  }
}
