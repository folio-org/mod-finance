package org.folio.services.ledger;

import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_BUDGETS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudget;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudgetCollection;

import io.vertx.core.Future;

public class LedgerRolloverBudgetsService {
  private final RestClient restClient;

  public LedgerRolloverBudgetsService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<LedgerFiscalYearRolloverBudget> retrieveLedgerRolloverBudgetById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(LEDGER_ROLLOVERS_BUDGETS_STORAGE, id), LedgerFiscalYearRolloverBudget.class, requestContext);
  }

  public Future<LedgerFiscalYearRolloverBudgetCollection> retrieveLedgerRolloverBudgets(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(LEDGER_ROLLOVERS_BUDGETS_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), LedgerFiscalYearRolloverBudgetCollection.class, requestContext);
  }
}
