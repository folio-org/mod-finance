package org.folio.services.ledger;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;

public class LedgerService {
  private final RestClient ledgerStorageRestClient;
  private final LedgerTotalsService ledgerTotalsService;

  public LedgerService(RestClient ledgerStorageRestClient, LedgerTotalsService ledgerTotalsService) {
    this.ledgerStorageRestClient = ledgerStorageRestClient;
    this.ledgerTotalsService = ledgerTotalsService;
  }

  public CompletableFuture<Ledger> createLedger(Ledger ledger, RequestContext requestContext) {
    return ledgerStorageRestClient.post(ledger, requestContext, Ledger.class);
  }

  public CompletableFuture<Ledger> retrieveLedgerById(String ledgerId, RequestContext requestContext) {
    return ledgerStorageRestClient.getById(ledgerId, requestContext, Ledger.class);
  }

  public CompletableFuture<LedgersCollection> retrieveLedgers(String query, int offset, int limit, RequestContext requestContext) {
    return ledgerStorageRestClient.get(query, offset, limit, requestContext, LedgersCollection.class);
  }

  public CompletableFuture<LedgersCollection> retrieveLedgersWithTotals(String query, int offset, int limit, String fiscalYearId, RequestContext requestContext) {
    return retrieveLedgers(query, offset, limit, requestContext)
      .thenCompose(ledgersCollection -> {
        if (isEmpty(fiscalYearId)) {
          return CompletableFuture.completedFuture(ledgersCollection);
        }
        return ledgerTotalsService.populateLedgersTotals(ledgersCollection, fiscalYearId, requestContext);
      });
  }

  public CompletableFuture<Ledger> retrieveLedgerWithTotals(String ledgerId, String fiscalYearId, RequestContext requestContext) {
    return retrieveLedgerById(ledgerId, requestContext)
      .thenCompose(ledger -> {
        if (isEmpty(fiscalYearId)) {
          return CompletableFuture.completedFuture(ledger);
        } else {
          return ledgerTotalsService.populateLedgerTotals(ledger, fiscalYearId, requestContext);
        }
      });
  }

  public CompletableFuture<Void> updateLedger(Ledger ledger, RequestContext requestContext) {
    return ledgerStorageRestClient.put(ledger.getId(), ledger, requestContext);
  }

  public CompletableFuture<Void> deleteLedger(String id, RequestContext requestContext) {
    return ledgerStorageRestClient.delete(id, requestContext);
  }
}
