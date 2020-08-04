package org.folio.rest.helper;

import static me.escoffier.vertx.completablefuture.VertxCompletableFuture.supplyBlockingAsync;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.HelperUtils;

public class FiscalYearsHelper extends AbstractHelper {

  private static final String CURRENCY = "currency";
  private static final String LOCALE_SETTINGS = "localeSettings";
  private static final String DEFAULT_LOCALE = "{\"locale\":\"en-US\",\"timezone\":\"America/New_York\",\"currency\":\"USD\"}";
  static final String GET_FISCAL_YEARS_BY_QUERY = resourcesPath(FISCAL_YEARS_STORAGE) + SEARCH_PARAMS;

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
        String currency = getCurrency(locale);
        fiscalYear.setCurrency(currency);
        return handleCreateRequest(resourcesPath(FISCAL_YEARS_STORAGE), fiscalYear).thenApply(fiscalYear::withId);
      });
  }

  private String getCurrency(JsonObject locale) {
    return Optional.ofNullable(locale.getString(LOCALE_SETTINGS, DEFAULT_LOCALE))
        .map(settings -> new JsonObject(settings).getString(CURRENCY))
        .orElseThrow(() -> new HttpException(500, ErrorCodes.CURRENCY_NOT_FOUND));
  }

  public CompletableFuture<FiscalYearsCollection> getFiscalYears(int limit, int offset, String query) {
    String endpoint = String.format(GET_FISCAL_YEARS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenCompose(json -> supplyBlockingAsync(ctx, () -> json.mapTo(FiscalYearsCollection.class)));
  }

  public CompletableFuture<FiscalYear> getFiscalYear(String id) {
    return handleGetRequest(resourceByIdPath(FISCAL_YEARS_STORAGE, id, lang))
      .thenApply(json -> json.mapTo(FiscalYear.class));
  }

  public CompletableFuture<Void> updateFiscalYear(FiscalYear fiscalYear) {
    return HelperUtils.getConfigurationEntries(okapiHeaders, ctx, logger)
      .thenCompose(locale -> {
        String currency = getCurrency(locale);
        fiscalYear.setCurrency(currency);
        return handleUpdateRequest(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYear.getId(), lang), fiscalYear);
      });

  }

  public CompletableFuture<Void> deleteFiscalYear(String id) {
    return handleDeleteRequest(resourceByIdPath(FISCAL_YEARS_STORAGE, id, lang));
  }
}
