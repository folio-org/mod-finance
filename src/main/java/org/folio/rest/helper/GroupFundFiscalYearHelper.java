package org.folio.rest.helper;

import static me.escoffier.vertx.completablefuture.VertxCompletableFuture.supplyBlockingAsync;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.handleGetRequest;
import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;

import io.vertx.core.Context;

public class GroupFundFiscalYearHelper extends AbstractHelper {

  private static final String GET_GROUP_FUND_FISCAL_YEARS_BY_QUERY = resourcesPath(GROUP_FUND_FISCAL_YEARS) + SEARCH_PARAMS;

  public GroupFundFiscalYearHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<GroupFundFiscalYear> createGroupFundFiscalYear(GroupFundFiscalYear GroupFundFiscalYear) {
    return handleCreateRequest(resourcesPath(GROUP_FUND_FISCAL_YEARS), GroupFundFiscalYear).thenApply(GroupFundFiscalYear::withId);
  }

  public CompletableFuture<GroupFundFiscalYearCollection> getGroupFundFiscalYears(int limit, int offset, String query) {
    String endpoint = String.format(GET_GROUP_FUND_FISCAL_YEARS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> supplyBlockingAsync(ctx, () -> json.mapTo(GroupFundFiscalYearCollection.class)));
  }

  public CompletableFuture<Void> deleteGroupFundFiscalYear(String id) {
    return handleDeleteRequest(resourceByIdPath(GROUP_FUND_FISCAL_YEARS, id, lang));
  }
}
