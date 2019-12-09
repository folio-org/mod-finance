package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.LEDGERS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

public class LedgersHelper extends AbstractHelper {

  private static final String GET_LEDGERS_BY_QUERY = resourcesPath(LEDGERS) + SEARCH_PARAMS;

  public LedgersHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public LedgersHelper(HttpClientInterface httpClient, Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(httpClient, okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Ledger> createLedger(Ledger ledger) {
    return handleCreateRequest(resourcesPath(LEDGERS), ledger).thenApply(ledger::withId);
  }

  public CompletableFuture<LedgersCollection> getLedgers(int limit, int offset, String query) {
    String endpoint = String.format(GET_LEDGERS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(LedgersCollection.class)));
  }

  public CompletableFuture<Ledger> getLedger(String id) {
    return handleGetRequest(resourceByIdPath(LEDGERS, id, lang))
      .thenApply(json -> json.mapTo(Ledger.class));
  }

  public CompletableFuture<Void> updateLedger(Ledger ledger) {
    return handleUpdateRequest(resourceByIdPath(LEDGERS, ledger.getId(), lang), ledger);
  }

  public CompletableFuture<Void> deleteLedger(String id) {
    return handleDeleteRequest(resourceByIdPath(LEDGERS, id, lang));
  }
}
