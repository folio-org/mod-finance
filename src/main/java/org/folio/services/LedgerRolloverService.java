package org.folio.services;

import java.util.concurrent.CompletableFuture;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverCollection;

public class LedgerRolloverService {
  private final RestClient ledgerRolloverStorageRestClient;

  public LedgerRolloverService(RestClient ledgerRolloverStorageRestClient) {
    this.ledgerRolloverStorageRestClient = ledgerRolloverStorageRestClient;
  }

  public CompletableFuture<LedgerFiscalYearRollover> createLedger(LedgerFiscalYearRollover ledgerFiscalYearRollover, RequestContext requestContext) {
    return ledgerRolloverStorageRestClient.post(ledgerFiscalYearRollover, requestContext, LedgerFiscalYearRollover.class);
  }

  public CompletableFuture<LedgerFiscalYearRollover> retrieveLedgerRolloverById(String id, RequestContext requestContext) {
    return ledgerRolloverStorageRestClient.getById(id, requestContext, LedgerFiscalYearRollover.class);
  }

  public CompletableFuture<LedgerFiscalYearRolloverCollection> retrieveLedgerRollovers(String query, int offset, int limit, RequestContext requestContext) {
    return ledgerRolloverStorageRestClient.get(query, offset, limit, requestContext, LedgerFiscalYearRolloverCollection.class);
  }
}
