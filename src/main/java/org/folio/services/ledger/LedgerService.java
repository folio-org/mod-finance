package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.LEDGERS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.services.protection.AcqUnitsService;

import io.vertx.core.Future;

public class LedgerService {
  private final RestClient restClient;
  private final LedgerTotalsService ledgerTotalsService;
  private final AcqUnitsService acqUnitsService;
  public static final String ID = "id";

  public LedgerService(RestClient restClient, LedgerTotalsService ledgerTotalsService, AcqUnitsService acqUnitsService) {
    this.restClient = restClient;
    this.ledgerTotalsService = ledgerTotalsService;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<Ledger> createLedger(Ledger ledger, RequestContext requestContext) {
    return restClient.post(resourcesPath(LEDGERS_STORAGE), ledger, Ledger.class, requestContext);
  }

  public Future<Ledger> retrieveLedgerById(String ledgerId, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(LEDGERS_STORAGE, ledgerId), Ledger.class, requestContext);
  }

  public Future<LedgersCollection> retrieveLedgers(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(LEDGERS_STORAGE).withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry, LedgersCollection.class, requestContext);
  }

  public Future<LedgersCollection> retrieveLedgersWithTotals(String query, int offset, int limit, String fiscalYearId, RequestContext requestContext) {
    return retrieveLedgers(query, offset, limit, requestContext)
      .compose(ledgersCollection -> {
        if (isEmpty(fiscalYearId)) {
          return succeededFuture(ledgersCollection);
        }
        return ledgerTotalsService.populateLedgersTotals(ledgersCollection, fiscalYearId, requestContext);
      });
  }

  public Future<LedgersCollection> retrieveLedgersWithAcqUnitsRestrictionAndTotals(String query, int offset, int limit, String fiscalYearId, RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .compose(effectiveQuery -> retrieveLedgersWithTotals(effectiveQuery, offset, limit, fiscalYearId, requestContext));
  }

  public Future<Ledger> retrieveLedgerWithTotals(String ledgerId, String fiscalYearId, RequestContext requestContext) {
    return retrieveLedgerById(ledgerId, requestContext)
      .compose(ledger -> {
        if (isEmpty(fiscalYearId)) {
          return succeededFuture(ledger);
        } else {
          return ledgerTotalsService.populateLedgerTotals(ledger, fiscalYearId, requestContext);
        }
      });
  }

  public Future<Void> updateLedger(Ledger ledger, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(LEDGERS_STORAGE,ledger.getId()), ledger, requestContext);
  }

  public Future<Void> deleteLedger(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(LEDGERS_STORAGE, id), requestContext);
  }

  public Future<List<Ledger>> getLedgers(Collection<String> ledgerIds, RequestContext requestContext) {
    return collectResultsOnSuccess(
      ofSubLists(new ArrayList<>(ledgerIds), MAX_IDS_FOR_GET_RQ).map(ids -> getLedgersByIds(ids, requestContext))
        .toList()).map(
      lists -> lists.stream()
        .flatMap(Collection::stream)
        .toList());
  }

  public Future<List<Ledger>> getLedgersByIds(Collection<String> ids, RequestContext requestContext) {
    String query = convertIdsToCqlQuery(ids);
    RequestEntry requestEntry = new RequestEntry(resourcesPath(LEDGERS_STORAGE)).withQuery(query)
      .withOffset(0)
      .withLimit(MAX_IDS_FOR_GET_RQ);
    return restClient.get(requestEntry, LedgersCollection.class, requestContext)
      .map(LedgersCollection::getLedgers);
  }

}
