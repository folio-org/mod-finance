package org.folio.services.ledger;

import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_PROGRESS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgress;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgressCollection;

import io.vertx.core.Future;

public class LedgerRolloverProgressService {
  private final RestClient restClient;

  public LedgerRolloverProgressService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<LedgerFiscalYearRolloverProgress> createLedgerRolloverProgress(LedgerFiscalYearRolloverProgress entity, RequestContext requestContext) {
    return restClient.post(resourcesPath(LEDGER_ROLLOVERS_PROGRESS_STORAGE), entity, LedgerFiscalYearRolloverProgress.class, requestContext);
  }

  public Future<Void> updateLedgerRolloverProgressById(String id, LedgerFiscalYearRolloverProgress entity, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(LEDGER_ROLLOVERS_PROGRESS_STORAGE, id), entity, requestContext);
  }

  public Future<LedgerFiscalYearRolloverProgress> retrieveLedgerRolloverProgressById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(LEDGER_ROLLOVERS_PROGRESS_STORAGE, id), LedgerFiscalYearRolloverProgress.class, requestContext);
  }

  public Future<LedgerFiscalYearRolloverProgressCollection> retrieveLedgerRolloverProgresses(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(LEDGER_ROLLOVERS_PROGRESS_STORAGE))
      .withLimit(limit)
      .withOffset(offset)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), LedgerFiscalYearRolloverProgressCollection.class, requestContext);
  }
}
