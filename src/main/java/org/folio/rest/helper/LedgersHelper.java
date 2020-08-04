package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.LEDGERS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.LEDGER_FYS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.acq.model.finance.LedgerFYCollection;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.util.HelperUtils;
import org.folio.services.LedgerService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class LedgersHelper extends AbstractHelper {

  private static final String GET_LEDGERS_BY_QUERY = resourcesPath(LEDGERS_STORAGE) + SEARCH_PARAMS;
  private static final String GET_LEDGERSFY_BY_QUERY = resourcesPath(LEDGER_FYS_STORAGE) + SEARCH_PARAMS;
  public static final String LEDGER_ID_AND_FISCAL_YEAR_ID = "ledgerId==%s AND fiscalYearId==%s";
  public static final String FISCAL_YEAR_ID = "fiscalYearId==%s";

  @Autowired
  private LedgerService ledgerService;

  public LedgersHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
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

  public CompletableFuture<LedgerFYCollection> getLedgerFYsByFiscalYearId(String fiscalYearId) {
    String query = String.format(FISCAL_YEAR_ID, fiscalYearId);
    String endpoint = String.format(GET_LEDGERSFY_BY_QUERY, Integer.MAX_VALUE, 0, HelperUtils.buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenApply(entries -> entries.mapTo(LedgerFYCollection.class));
  }

  public CompletableFuture<Void> updateLedger(Ledger ledger) {
    return handleUpdateRequest(resourceByIdPath(LEDGERS_STORAGE, ledger.getId(), lang), ledger);
  }

  public CompletableFuture<Void> deleteLedger(String id) {
    return handleDeleteRequest(resourceByIdPath(LEDGERS_STORAGE, id, lang));
  }
}
