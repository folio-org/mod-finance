package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.Source;
import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.util.ErrorCodes;

public class BudgetsHelper extends AbstractHelper {

  private static final String GET_BUDGETS_BY_QUERY = resourcesPath(BUDGETS) + SEARCH_PARAMS;

  public BudgetsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public BudgetsHelper(HttpClientInterface httpClient, Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(httpClient, okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Budget> createBudget(Budget budget) {
    double allocatedValue = budget.getAllocated();
    budget.setAllocated(0d);
    return handleCreateRequest(resourcesPath(BUDGETS), budget).thenCompose(budgetId -> {
      if (allocatedValue > 0d) {
        return createAllocationTransaction(budget.withAllocated(allocatedValue)).thenApply(v -> budget.withId(budgetId))
          .exceptionally(e -> {
            throw new HttpException(500, ErrorCodes.ALLOCATION_TRANSFER_FAILED);
          });
      }
      return CompletableFuture.completedFuture(budget.withId(budgetId));
    });
  }

  private CompletableFuture<Void> createAllocationTransaction(Budget budget) {
    Transaction transaction = new Transaction().withAmount(budget.getAllocated())
        .withFiscalYearId(budget.getFiscalYearId()).withToFundId(budget.getFundId())
        .withTransactionType(Transaction.TransactionType.ALLOCATION).withSource(Source.USER);

    return handleGetRequest(resourceByIdPath(FISCAL_YEARS, budget.getFiscalYearId(), lang)).
        thenApply(json -> json.mapTo(FiscalYear.class)).thenAccept(fy ->
        handleCreateRequest(resourcesPath(TRANSACTIONS), transaction.withCurrency(fy.getCurrency())));

  }

  public CompletableFuture<BudgetsCollection> getBudgets(int limit, int offset, String query) {
    String endpoint = String.format(GET_BUDGETS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(BudgetsCollection.class)));
  }

  public CompletableFuture<List<Budget>> getBudgetList(int limit, int offset, String query) {
    return getBudgets(limit, offset, query).thenApply(BudgetsCollection::getBudgets);
  }

  public CompletableFuture<Budget> getSingleBudgetByQuery(String query) {
    return getBudgetList(1, 0, query).thenApply(budgets-> budgets.get(0));
  }

  public CompletableFuture<Budget> getBudget(String id) {
    return handleGetRequest(resourceByIdPath(BUDGETS, id, lang))
      .thenApply(json -> json.mapTo(Budget.class));
  }

  public CompletableFuture<Void> updateBudget(Budget budget) {
    return handleUpdateRequest(resourceByIdPath(BUDGETS, budget.getId(), lang), budget);
  }

  public CompletableFuture<Void> deleteBudget(String id) {
    return handleDeleteRequest(resourceByIdPath(BUDGETS, id, lang));
  }
}
