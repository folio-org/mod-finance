package org.folio.services;

import org.folio.rest.acq.model.finance.LedgerFiscalYearRolloverErrorCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverError;
import java.util.concurrent.CompletableFuture;

public class LedgerRolloversErrorsService {
  private final RestClient ledgerRolloversErrorsRestClient;

  public LedgerRolloversErrorsService(RestClient ledgerRolloverErrorsRestClient) {
    this.ledgerRolloversErrorsRestClient = ledgerRolloverErrorsRestClient;
  }

  public CompletableFuture<LedgerFiscalYearRolloverError> retrieveLedgerRolloversErrorsById(String id, RequestContext requestContext) {
    return ledgerRolloversErrorsRestClient.getById(id, requestContext, LedgerFiscalYearRolloverError.class);
  }

  public CompletableFuture<LedgerFiscalYearRolloverErrorCollection> retrieveLedgersRolloverErrors(String query, int offset, int limit, RequestContext requestContext) {
    return ledgerRolloversErrorsRestClient.get(query, offset, limit, requestContext, LedgerFiscalYearRolloverErrorCollection.class);
  }

}
