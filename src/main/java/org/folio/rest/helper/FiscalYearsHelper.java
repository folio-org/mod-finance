package org.folio.rest.helper;

import static me.escoffier.vertx.completablefuture.VertxCompletableFuture.supplyBlockingAsync;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.handleGetRequest;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.util.HelperUtils;

public class FiscalYearsHelper extends AbstractHelper {

  private static final String CURRENCY = "currency";
  private static final String LOCALE_SETTINGS = "localeSettings";
  private static final String DEFAULT_LOCALE = "{\"locale\":\"en-US\",\"timezone\":\"America/New_York\",\"currency\":\"USD\"}";
  static final String GET_FISCAL_YEARS_BY_QUERY = resourcesPath(FISCAL_YEARS) + SEARCH_PARAMS;

  public FiscalYearsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public FiscalYearsHelper(HttpClientInterface httpClient, Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(httpClient, okapiHeaders, ctx, lang);
  }

  public CompletableFuture<FiscalYear> createFiscalYear(FiscalYear fiscalYear) {
    return HelperUtils.getConfigurationEntries(okapiHeaders, ctx, logger)
      .thenCompose(locale -> {
        // Initially the config will not have any locale, the same default values used here are hardcoded in tenant Settings
        fiscalYear.setCurrency(new JsonObject(
            locale.getString(LOCALE_SETTINGS, DEFAULT_LOCALE))
              .getString(CURRENCY));
        return handleCreateRequest(resourcesPath(FISCAL_YEARS), fiscalYear).thenApply(fiscalYear::withId);
      });
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

  public CompletableFuture<Void> updateFiscalYear(FiscalYear fiscalYear) {
    return HelperUtils.getConfigurationEntries(okapiHeaders, ctx, logger)
      .thenCompose(locale -> {
        fiscalYear.setCurrency(new JsonObject(
            locale.getString(LOCALE_SETTINGS, DEFAULT_LOCALE))
              .getString(CURRENCY));
        return handleUpdateRequest(resourceByIdPath(FISCAL_YEARS, fiscalYear.getId(), lang), fiscalYear);
      });

  }

  public CompletableFuture<Void> deleteFiscalYear(String id) {
    return handleDeleteRequest(resourceByIdPath(FISCAL_YEARS, id, lang));
  }
}
