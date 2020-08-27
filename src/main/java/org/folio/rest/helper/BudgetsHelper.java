package org.folio.rest.helper;

import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.Source;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.util.ErrorCodes;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

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

  public CompletableFuture<Budget> getBudget(String id) {
    return handleGetRequest(resourceByIdPath(BUDGETS, id, lang))
      .thenApply(json -> json.mapTo(Budget.class));
  }

  public CompletableFuture<Void> updateBudget(Budget budget) {
    return getBudget(budget.getId())
      .thenApply(budgetFromStorage -> mergeBudgets(budget, budgetFromStorage))
      .thenCompose(mergedBudget -> handleUpdateRequest(resourceByIdPath(BUDGETS, mergedBudget.getId(), lang), mergedBudget));
  }

  public CompletableFuture<Void> deleteBudget(String id) {
    return handleDeleteRequest(resourceByIdPath(BUDGETS, id, lang));
  }

  public boolean newAllowableAmountsExceeded(Budget budget) {
    BigDecimal allocated = BigDecimal.valueOf(budget.getAllocated());
    BigDecimal encumbered = BigDecimal.valueOf(budget.getEncumbered());
    BigDecimal expenditures = BigDecimal.valueOf(budget.getExpenditures());
    BigDecimal awaitingPayment = BigDecimal.valueOf(budget.getAwaitingPayment());
    BigDecimal available = BigDecimal.valueOf(budget.getAvailable());
    BigDecimal unavailable = BigDecimal.valueOf(budget.getUnavailable());

    //[remaining amount we can encumber] = (allocated * allowableEncumbered) - (encumbered + awaitingPayment + expended)
    if (budget.getAllowableEncumbrance() != null) {
      BigDecimal newAllowableEncumbrance = BigDecimal.valueOf(budget.getAllowableEncumbrance()).movePointLeft(2);
      if (allocated.multiply(newAllowableEncumbrance).compareTo(encumbered.add(awaitingPayment).add(expenditures)) < 0) {
        this.addProcessingError(ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED.toError());
      }
    }
    //[amount we can expend] = (allocated * allowableExpenditure) - (allocated - (unavailable + available)) - (awaitingPayment + expended)
    if (budget.getAllowableExpenditure() != null) {
      BigDecimal newAllowableExpenditure = BigDecimal.valueOf(budget.getAllowableExpenditure())
        .movePointLeft(2);
      if (allocated.multiply(newAllowableExpenditure)
        .subtract(allocated.subtract(available.add(unavailable)))
        .subtract(expenditures.add(awaitingPayment))
        .compareTo(BigDecimal.ZERO) < 0) {
        this.addProcessingError(ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED.toError());
      }
    }
    return !getProcessingErrors().getErrors().isEmpty();
  }

  private Budget mergeBudgets(Budget newBudget, Budget budgetFromStorage) {
    return newBudget
      .withAllocated(budgetFromStorage.getAllocated())
      .withAvailable(budgetFromStorage.getAvailable())
      .withUnavailable(budgetFromStorage.getUnavailable())
      .withAwaitingPayment(budgetFromStorage.getAwaitingPayment())
      .withExpenditures(budgetFromStorage.getExpenditures())
      .withEncumbered(budgetFromStorage.getEncumbered())
      .withOverEncumbrance(budgetFromStorage.getOverEncumbrance())
      .withOverExpended(budgetFromStorage.getOverExpended());
   }
}
