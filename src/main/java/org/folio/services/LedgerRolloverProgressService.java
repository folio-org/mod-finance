package org.folio.services;

import java.util.concurrent.CompletableFuture;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgress;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgressCollection;

public class LedgerRolloverProgressService {
  private final RestClient ledgerRolloverProgressStorageRestClient;

  public LedgerRolloverProgressService(RestClient ledgerRolloverProgressRestClient) {
    this.ledgerRolloverProgressStorageRestClient = ledgerRolloverProgressRestClient;
  }

  public CompletableFuture<LedgerFiscalYearRolloverProgress> createLedgerRolloverProgress(LedgerFiscalYearRolloverProgress entity, RequestContext requestContext) {
    return ledgerRolloverProgressStorageRestClient.post(entity, requestContext, LedgerFiscalYearRolloverProgress.class);
  }

  public CompletableFuture<Void> updateLedgerRolloverProgressById(String id, LedgerFiscalYearRolloverProgress entity, RequestContext requestContext) {
    return ledgerRolloverProgressStorageRestClient.put(id, entity, requestContext);
  }

  public CompletableFuture<LedgerFiscalYearRolloverProgress> retrieveLedgerRolloverProgressById(String id, RequestContext requestContext) {
    return ledgerRolloverProgressStorageRestClient.getById(id, requestContext, LedgerFiscalYearRolloverProgress.class);
  }

  public CompletableFuture<LedgerFiscalYearRolloverProgressCollection> retrieveLedgerRolloverProgresses(String query, int offset, int limit, RequestContext requestContext) {
    return ledgerRolloverProgressStorageRestClient.get(query, offset, limit, requestContext, LedgerFiscalYearRolloverProgressCollection.class);
  }
}
