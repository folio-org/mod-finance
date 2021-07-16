package org.folio.services.ledger;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.LEDGERS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

public class LedgerService {
  private final RestClient ledgerStorageRestClient;
  private final LedgerTotalsService ledgerTotalsService;
  public static final String ID = "id";

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

  public CompletableFuture<List<Ledger>> getLedgers(Collection<String> ledgerIds, RequestContext requestContext) {
    return collectResultsOnSuccess(
      ofSubLists(new ArrayList<>(ledgerIds), MAX_IDS_FOR_GET_RQ).map(ids -> getLedgersByIds(ids, requestContext))
        .toList()).thenApply(
      lists -> lists.stream()
        .flatMap(Collection::stream)
        .collect(toList()));
  }

  private CompletableFuture<List<Ledger>> getLedgersByIds(Collection<String> ids, RequestContext requestContext) {
    String query = convertIdsToCqlQuery(ids);
    RequestEntry requestEntry = new RequestEntry(resourcesPath(LEDGERS_STORAGE)).withQuery(query)
      .withOffset(0)
      .withLimit(MAX_IDS_FOR_GET_RQ);
    return ledgerStorageRestClient.get(requestEntry, requestContext, LedgersCollection.class)
      .thenApply(LedgersCollection::getLedgers);
  }

}
