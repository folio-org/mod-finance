package org.folio.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;

public class FundDetailsService {
  private static final String ACTIVE_BUDGET_QUERY = "query=fundId==%s and budgetStatus==Active";

  private final BudgetService budgetService;
  private final BudgetExpenseClassService budgetExpenseClassService;

  public FundDetailsService(BudgetService budgetService, BudgetExpenseClassService budgetExpenseClassService) {
    this.budgetService = budgetService;
    this.budgetExpenseClassService = budgetExpenseClassService;
  }

  public CompletableFuture<Budget> retrieveActiveBudget(String fundId, RequestContext requestContext) {
    String query = String.format(ACTIVE_BUDGET_QUERY, fundId);
    return budgetService.getBudgets(query, 0, Integer.MAX_VALUE, requestContext)
                        .thenApply(this::getFirstBudget);
  }

  public CompletableFuture<List<BudgetExpenseClass>> retrieveCurrentExpenseClasses(String fundId, RequestContext requestContext) {
    return retrieveActiveBudget(fundId, requestContext)
                .thenCompose(activeBudget -> retrieveExpenseClasses(activeBudget, requestContext));
  }

  private CompletableFuture<List<BudgetExpenseClass>> retrieveExpenseClasses(Budget budget, RequestContext requestContext) {
    return Optional.ofNullable(budget)
                   .map(activeBudgetP -> budgetExpenseClassService.getBudgetExpenseClasses(activeBudgetP.getId(), requestContext))
                   .orElse(CompletableFuture.completedFuture(Collections.emptyList()));
  }

  private Budget getFirstBudget(BudgetsCollection budgetsCollection) {
    return Optional.ofNullable(budgetsCollection)
                  .filter(budgetsCol -> !CollectionUtils.isEmpty(budgetsCol.getBudgets()))
                  .map(BudgetsCollection::getBudgets)
                  .map(budgets -> budgets.get(0))
                  .orElse(null);
  }
}
