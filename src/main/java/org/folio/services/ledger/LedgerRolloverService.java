package org.folio.services.ledger;

import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverCollection;

import io.vertx.core.Future;

public class LedgerRolloverService {
  private final RestClient restClient;

  public LedgerRolloverService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<LedgerFiscalYearRollover> createLedgerFyRollover(LedgerFiscalYearRollover ledgerFiscalYearRollover, RequestContext requestContext) {
    return restClient.post(resourcesPath(LEDGER_ROLLOVERS_STORAGE), ledgerFiscalYearRollover, LedgerFiscalYearRollover.class, requestContext);
  }

  public Future<LedgerFiscalYearRollover> retrieveLedgerRolloverById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(LEDGER_ROLLOVERS_STORAGE, id), LedgerFiscalYearRollover.class, requestContext);
  }

  public Future<LedgerFiscalYearRolloverCollection> retrieveLedgerRollovers(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(LEDGER_ROLLOVERS_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), LedgerFiscalYearRolloverCollection.class, requestContext);
  }
}
