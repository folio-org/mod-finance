package org.folio.rest.helper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.LEDGER_FY_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.LEDGERS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.LEDGER_FYS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.acq.model.finance.LedgerFY;
import org.folio.rest.acq.model.finance.LedgerFYCollection;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.util.HelperUtils;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class LedgersHelper extends AbstractHelper {

  private static final String GET_LEDGERS_BY_QUERY = resourcesPath(LEDGERS_STORAGE) + SEARCH_PARAMS;
  private static final String GET_LEDGERSFY_BY_QUERY = resourcesPath(LEDGER_FYS) + SEARCH_PARAMS;
  public static final String LEDGER_ID_AND_FISCAL_YEAR_ID = "ledgerId==%s AND fiscalYearId==%s";
  public static final String FISCAL_YEAR_ID = "fiscalYearId==%s";

  public LedgersHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public LedgersHelper(HttpClientInterface httpClient, Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(httpClient, okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Ledger> createLedger(Ledger ledger) {
    return handleCreateRequest(resourcesPath(LEDGERS_STORAGE), ledger).thenApply(ledger::withId);
  }

  public CompletableFuture<LedgersCollection> getLedgers(int limit, int offset, String query) {
    String endpoint = String.format(GET_LEDGERS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(LedgersCollection.class)));
  }

  public CompletableFuture<Ledger> getLedgerWithSummary(String ledgerId, String fiscalYearId) {
    CompletableFuture<Ledger> future = getLedger(ledgerId);
    if (isEmpty(fiscalYearId)) {
      return future;
    } else {
      return future.thenCompose(ledger -> getLedgerFY(ledgerId, fiscalYearId)
        .thenApply(ledgerFY -> getLedgerWithTotals(ledgerFY, ledger)));
    }
  }

  private CompletableFuture<LedgerFY> getLedgerFY(String ledgerId, String fiscalYearId) {
    String query = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledgerId, fiscalYearId);
    String endpoint = String.format(GET_LEDGERSFY_BY_QUERY, 1, 0, HelperUtils.buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenApply(entries -> {
        LedgerFYCollection ledgerFYs = entries.mapTo(LedgerFYCollection.class);
        if (CollectionUtils.isNotEmpty(ledgerFYs.getLedgerFY())) {
          return ledgerFYs.getLedgerFY().get(0);
        }
        throw new HttpException(BAD_REQUEST.getStatusCode(), LEDGER_FY_NOT_FOUND);
      });
  }

  public CompletableFuture<LedgerFYCollection> getLedgerFYsByFiscalYearId(String fiscalYearId) {
    String query = String.format(FISCAL_YEAR_ID, fiscalYearId);
    String endpoint = String.format(GET_LEDGERSFY_BY_QUERY, Integer.MAX_VALUE, 0, HelperUtils.buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenApply(entries -> entries.mapTo(LedgerFYCollection.class));
  }

  private Ledger getLedgerWithTotals(LedgerFY ledgerFY, Ledger ledger) {
    return ledger.withAllocated(ledgerFY.getAllocated())
      .withAvailable(ledgerFY.getAvailable())
      .withUnavailable(ledgerFY.getUnavailable());
  }

  CompletableFuture<Ledger> getLedger(String id) {
    return handleGetRequest(resourceByIdPath(LEDGERS_STORAGE, id, lang))
      .thenApply(json -> json.mapTo(Ledger.class));
  }

  public CompletableFuture<Void> updateLedger(Ledger ledger) {
    return handleUpdateRequest(resourceByIdPath(LEDGERS_STORAGE, ledger.getId(), lang), ledger);
  }

  public CompletableFuture<Void> deleteLedger(String id) {
    return handleDeleteRequest(resourceByIdPath(LEDGERS_STORAGE, id, lang));
  }
}
