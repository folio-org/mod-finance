package org.folio.services.ledger;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLog;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLogCollection;

import java.util.concurrent.CompletableFuture;

public class LedgerRolloverLogsService {
  private final RestClient ledgerRolloverLogsStorageRestClient;

  public LedgerRolloverLogsService(RestClient ledgerRolloverProgressStorageRestClient) {
    this.ledgerRolloverLogsStorageRestClient = ledgerRolloverProgressStorageRestClient;
  }

  public CompletableFuture<LedgerFiscalYearRolloverLog> retrieveLedgerRolloverLogById(String id, RequestContext requestContext) {
    return ledgerRolloverLogsStorageRestClient.getById(id, requestContext, LedgerFiscalYearRolloverLog.class);
  }

  public CompletableFuture<LedgerFiscalYearRolloverLogCollection> retrieveLedgerRolloverLogs(String query, int offset, int limit, RequestContext requestContext) {
    return ledgerRolloverLogsStorageRestClient.get(query, offset, limit, requestContext, LedgerFiscalYearRolloverLogCollection.class);
  }
}
