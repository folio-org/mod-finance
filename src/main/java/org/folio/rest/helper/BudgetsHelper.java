package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.handleGetRequest;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class BudgetsHelper extends AbstractHelper {

  private static final String GET_BUDGETS_BY_QUERY = resourcesPath(BUDGETS) + SEARCH_PARAMS;

  public BudgetsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Budget> createBudget(Budget budget) {
    return handleCreateRequest(resourcesPath(BUDGETS), budget).thenApply(budget::withId);
  }

  public CompletableFuture<BudgetsCollection> getBudgets(int limit, int offset, String query) {
    String endpoint = String.format(GET_BUDGETS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(BudgetsCollection.class)));
  }

  public CompletableFuture<Budget> getBudget(String id) {
    return handleGetRequest(resourceByIdPath(BUDGETS, id, lang), httpClient, ctx, okapiHeaders, logger)
      .thenApply(json -> json.mapTo(Budget.class));
  }

  public CompletableFuture<Void> updateBudget(Budget budget) {
    return handleUpdateRequest(resourceByIdPath(BUDGETS, budget.getId(), lang), budget);
  }

  public CompletableFuture<Void> deleteBudget(String id) {
    return handleDeleteRequest(resourceByIdPath(BUDGETS, id, lang));
  }
}
