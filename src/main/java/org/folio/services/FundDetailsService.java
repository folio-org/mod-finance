package org.folio.services;

import static org.folio.rest.util.ErrorCodes.CURRENT_BUDGET_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.CURRENT_FISCAL_YEAR_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
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
  private final ExpenseClassService expenseClassService;

  public FundDetailsService(FiscalYearService fiscalYearService, FundService fundService
        , BudgetService budgetService, ExpenseClassService expenseClassService) {
    this.fiscalYearService = fiscalYearService;
    this.fundService = fundService;
    this.budgetService = budgetService;
    this.expenseClassService = expenseClassService;
  }

  public CompletableFuture<Budget> retrieveCurrentBudget(String fundId, RequestContext requestContext) {
    return fundService.retrieveFundById(fundId, requestContext)
                      .thenApply(Fund::getLedgerId)
                      .thenCompose(budgetLedgerId -> getCurrentFiscalYear(budgetLedgerId, requestContext))
                      .thenApply(fundCurrFY -> buildActiveBudgetQuery(fundId, fundCurrFY.getId()))
                      .thenCompose(activeBudgetQuery -> budgetService.getBudgets(activeBudgetQuery, 0, Integer.MAX_VALUE, requestContext))
                      .thenApply(this::getFirstBudget);
  }

  public CompletableFuture<List<ExpenseClass>> retrieveCurrentExpenseClasses(String fundId, RequestContext requestContext) {
    CompletableFuture<List<ExpenseClass>> future = new VertxCompletableFuture<>(requestContext.getContext());
    retrieveCurrentBudget(fundId, requestContext)
                      .thenCompose(currentBudget -> {
                        logger.debug("Is Current budget for fund found : " + currentBudget.getId());
                        return retrieveBudgetExpenseClasses(currentBudget, requestContext);
                      })
                      .thenAccept(expenseClasses -> {
                        logger.debug("Expense classes for fund size : " + expenseClasses.size());
                        future.complete(expenseClasses);
                      })
                      .exceptionally(t -> {
                        logger.error(GENERIC_ERROR_CODE.getDescription(), t.getCause());
                        future.completeExceptionally(t);
                        return null;
                      });
    return future;
  }

  private CompletableFuture<List<ExpenseClass>> retrieveBudgetExpenseClasses(Budget budget, RequestContext requestContext) {
    return Optional.ofNullable(budget)
                  .map(budgetP -> expenseClassService.getExpenseClassesByBudgetId(budgetP.getId(), requestContext))
                  .orElse(CompletableFuture.completedFuture(Collections.emptyList()));
  }

  private Budget getFirstBudget(BudgetsCollection budgetsCollection) {
    return Optional.ofNullable(budgetsCollection)
                  .filter(budgetsCol -> !CollectionUtils.isEmpty(budgetsCol.getBudgets()))
                  .map(BudgetsCollection::getBudgets)
                  .map(budgets -> budgets.get(0))
                  .orElseThrow(() -> new HttpException(404, CURRENT_BUDGET_NOT_FOUND.toError()));
  }

  private CompletableFuture<FiscalYear> getCurrentFiscalYear(String budgetLedgerId, RequestContext requestContext) {
    return fiscalYearService.getCurrentFiscalYear(budgetLedgerId, requestContext)
                            .thenApply(fiscalYear ->
                              Optional.ofNullable(fiscalYear)
                                      .orElseThrow(() -> new HttpException(404, CURRENT_FISCAL_YEAR_NOT_FOUND.toError()))
                            );
  }

  private String buildActiveBudgetQuery(String fundId, String fundCurrFYId) {
    return String.format(ACTIVE_BUDGET_QUERY, fundId, fundCurrFYId);
  }
}
