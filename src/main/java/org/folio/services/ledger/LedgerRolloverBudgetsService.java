package org.folio.services.ledger;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudget;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudgetCollection;

import java.util.concurrent.CompletableFuture;

public class LedgerRolloverBudgetsService {
  private final RestClient ledgerRolloverBudgetsStorageRestClient;

  public LedgerRolloverBudgetsService(RestClient ledgerRolloverBudgetsStorageRestClient) {
    this.ledgerRolloverBudgetsStorageRestClient = ledgerRolloverBudgetsStorageRestClient;
  }

  public CompletableFuture<LedgerFiscalYearRolloverBudget> retrieveLedgerRolloverBudgetById(String id, RequestContext requestContext) {
    return ledgerRolloverBudgetsStorageRestClient.getById(id, requestContext, LedgerFiscalYearRolloverBudget.class);
  }

  public CompletableFuture<LedgerFiscalYearRolloverBudgetCollection> retrieveLedgerRolloverBudgets(String query, int offset, int limit, RequestContext requestContext) {
    return ledgerRolloverBudgetsStorageRestClient.get(query, offset, limit, requestContext, LedgerFiscalYearRolloverBudgetCollection.class);
  }
}
