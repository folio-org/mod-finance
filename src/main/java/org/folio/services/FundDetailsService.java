package org.folio.services;

import static org.folio.rest.util.HelperUtils.EXCEPTION_CALLING_ENDPOINT_MSG;
import static org.folio.rest.util.HelperUtils.emptyListFuture;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.Fund;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class FundDetailsService {
  private static final Logger logger = LoggerFactory.getLogger(FundDetailsService.class);
  private static final String ACTIVE_BUDGET_QUERY = "query=fundId==%s and fiscalYearId==%s";

  private final FiscalYearService fiscalYearService;
  private final FundService fundService;
  private final BudgetService budgetService;
  private final BudgetExpenseClassService budgetExpenseClassService;

  public FundDetailsService(FiscalYearService fiscalYearService, FundService fundService
        , BudgetService budgetService, BudgetExpenseClassService budgetExpenseClassService) {
    this.fiscalYearService = fiscalYearService;
    this.fundService = fundService;
    this.budgetService = budgetService;
    this.budgetExpenseClassService = budgetExpenseClassService;
  }

  public CompletableFuture<Optional<Budget>> retrieveCurrentBudget(String fundId, RequestContext requestContext) {
    return fundService.retrieveFundById(fundId, requestContext)
                      .thenApply(Fund::getLedgerId)
                      .thenCompose(budgetLedgerId -> fiscalYearService.getCurrentFiscalYear(budgetLedgerId, requestContext))
                      .thenApply(fundCurrFY -> buildActiveBudgetQuery(fundId, fundCurrFY.getId()))
                      .thenCompose(activeBudgetQuery -> budgetService.getBudgets(activeBudgetQuery, 0, Integer.MAX_VALUE, requestContext))
                      .thenApply(this::getFirstBudget);
  }

  public CompletableFuture<List<BudgetExpenseClass>> retrieveCurrentExpenseClasses(String fundId, RequestContext requestContext) {
    CompletableFuture<List<BudgetExpenseClass>> future = new VertxCompletableFuture<>(requestContext.getContext());
    retrieveCurrentBudget(fundId, requestContext)
                      .thenCompose(activeBudget -> activeBudget.map(budget -> retrieveExpenseClasses(budget, requestContext))
                                                               .orElse(emptyListFuture()))
                      .thenAccept(future::complete)
                      .exceptionally(t -> {
                        logger.error(EXCEPTION_CALLING_ENDPOINT_MSG, t);
                        future.completeExceptionally(t);
                        return null;
                      });
    return future;
  }

  private CompletableFuture<List<BudgetExpenseClass>> retrieveExpenseClasses(Budget budget, RequestContext requestContext) {
    return Optional.ofNullable(budget)
      .map(activeBudgetP -> budgetExpenseClassService.getBudgetExpenseClasses(activeBudgetP.getId(), requestContext))
      .orElse(CompletableFuture.completedFuture(Collections.emptyList()));
  }

  private Optional<Budget> getFirstBudget(BudgetsCollection budgetsCollection) {
    return Optional.ofNullable(budgetsCollection)
                  .filter(budgetsCol -> !CollectionUtils.isEmpty(budgetsCol.getBudgets()))
                  .map(BudgetsCollection::getBudgets)
                  .map(budgets -> budgets.get(0));
  }

  private String buildActiveBudgetQuery(String fundId, String fundCurrFYId) {
    return String.format(ACTIVE_BUDGET_QUERY, fundId, fundCurrFYId);
  }
}
