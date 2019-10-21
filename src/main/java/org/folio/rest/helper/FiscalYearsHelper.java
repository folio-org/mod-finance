package org.folio.rest.helper;

import static me.escoffier.vertx.completablefuture.VertxCompletableFuture.supplyBlockingAsync;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.handleGetRequest;

import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;

public class FiscalYearsHelper extends AbstractHelper {

  private static final String GET_FISCAL_YEARS_BY_QUERY = resourcesPath(FISCAL_YEARS) + SEARCH_PARAMS;

  public FiscalYearsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<FiscalYear> createFiscalYear(FiscalYear fund) {
    return handleCreateRequest(resourcesPath(FISCAL_YEARS), fund).thenApply(fund::withId);
  }

  public CompletableFuture<FiscalYearsCollection> getFiscalYears(int limit, int offset, String query) {
    String endpoint = String.format(GET_FISCAL_YEARS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> supplyBlockingAsync(ctx, () -> json.mapTo(FiscalYearsCollection.class)));
  }

  public CompletableFuture<FiscalYear> getFiscalYear(String id) {
    return handleGetRequest(resourceByIdPath(FISCAL_YEARS, id, lang), httpClient, ctx, okapiHeaders, logger)
      .thenApply(json -> json.mapTo(FiscalYear.class));
  }

  public CompletableFuture<Void> updateFiscalYear(FiscalYear fund) {
    return handleUpdateRequest(resourceByIdPath(FISCAL_YEARS, fund.getId(), lang), fund);
  }

  public CompletableFuture<Void> deleteFiscalYear(String id) {
    return handleDeleteRequest(resourceByIdPath(FISCAL_YEARS, id, lang));
  }
}
