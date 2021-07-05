package org.folio.services.ledger;

import one.util.streamex.StreamEx;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.acq.model.finance.LedgerCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.jaxrs.model.Parameter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.ErrorCodes.LEDGER_NOT_FOUND_FOR_TRANSACTION;

public class LedgerService {
  private final RestClient ledgerStorageRestClient;
  private final LedgerTotalsService ledgerTotalsService;
  public static final String ID = "id";
  private static final String ENDPOINT = "/finance/ledgers";

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

  public CompletableFuture<List<org.folio.rest.acq.model.finance.Ledger>> getLedgersByIds(Collection<String> ledgerIds, RequestContext requestContext) {
    String query = convertIdsToCqlQuery(ledgerIds, ID);
    RequestEntry requestEntry = new RequestEntry(ENDPOINT).withQuery(query)
      .withLimit(MAX_IDS_FOR_GET_RQ).withOffset(0);
    return ledgerStorageRestClient.get(requestEntry, requestContext, LedgerCollection.class)
      .thenApply(ledgerCollection -> {
        if (ledgerIds.size() == ledgerCollection.getLedgers()
          .size()) {
          return ledgerCollection.getLedgers();
        }
        String missingIds = String.join(", ", CollectionUtils.subtract(ledgerIds, ledgerCollection.getLedgers()
          .stream()
          .map(org.folio.rest.acq.model.finance.Ledger::getId)
          .collect(toList())));
        throw new HttpException(404, LEDGER_NOT_FOUND_FOR_TRANSACTION.toError()
          .withParameters(Collections.singletonList(new Parameter().withKey("ledgers")
            .withValue(missingIds))));
      });
  }

  public static String convertIdsToCqlQuery(Collection<String> ids, String idField) {
    return convertFieldListToCqlQuery(ids, idField, true);
  }

  public static String convertFieldListToCqlQuery(Collection<String> values, String fieldName, boolean strictMatch) {
    String prefix = fieldName + (strictMatch ? "==(" : "=(");
    return StreamEx.of(values).joining(" or ", prefix, ")");
  }
}
