package org.folio.services.ledger;

import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_LOGS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLog;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLogCollection;

import io.vertx.core.Future;

public class LedgerRolloverLogsService {
  private final RestClient restClient;

  public LedgerRolloverLogsService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<LedgerFiscalYearRolloverLog> retrieveLedgerRolloverLogById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(LEDGER_ROLLOVERS_LOGS_STORAGE, id), LedgerFiscalYearRolloverLog.class, requestContext);
  }

  public Future<LedgerFiscalYearRolloverLogCollection> retrieveLedgerRolloverLogs(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(LEDGER_ROLLOVERS_LOGS_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), LedgerFiscalYearRolloverLogCollection.class, requestContext);
  }
}
