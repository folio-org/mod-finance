package org.folio.services;

import static org.folio.rest.util.ErrorCodes.CURRENT_BUDGET_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.CURRENT_FISCAL_YEAR_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.PLANNED_FISCAL_YEAR_NOT_FOUND;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClass.Status;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class FundDetailsService {
  private static final Logger logger = LoggerFactory.getLogger(FundDetailsService.class);
  private static final String CURRENT_BUDGET_QUERY_WITH_STATUS = "query=fundId==%s and fiscalYearId==%s and budgetStatus==%s";
  private static final String CURRENT_BUDGET_QUERY = "query=fundId==%s and fiscalYearId==%s";

  private final LedgerDetailsService ledgerDetailsService;
  private final FundService fundService;
  private final BudgetService budgetService;
  private final ExpenseClassService expenseClassService;
  private final BudgetExpenseClassService budgetExpenseClassService;

  public FundDetailsService(LedgerDetailsService ledgerDetailsService, FundService fundService
    , BudgetService budgetService, ExpenseClassService expenseClassService, BudgetExpenseClassService budgetExpenseClassService) {
    this.ledgerDetailsService = ledgerDetailsService;
    this.fundService = fundService;
    this.budgetService = budgetService;
    this.expenseClassService = expenseClassService;
    this.budgetExpenseClassService = budgetExpenseClassService;
  }

  public CompletableFuture<FiscalYear> retrieveFundCurrentFiscalYear(String fundId, RequestContext rqContext) {
    return fundService.retrieveFundById(fundId, rqContext)
                      .thenApply(Fund::getLedgerId)
                      .thenCompose(budgetLedgerId -> getCurrentFiscalYear(budgetLedgerId, rqContext));
  }

  public CompletableFuture<FiscalYear> retrieveFundPlannedFiscalYear(String fundId, RequestContext rqContext) {
    return fundService.retrieveFundById(fundId, rqContext)
      .thenApply(Fund::getLedgerId)
      .thenCompose(budgetLedgerId -> getPlannedFiscalYear(budgetLedgerId, rqContext));
  }


  public CompletableFuture<Budget> retrieveCurrentBudget(String fundId, String budgetStatus, RequestContext rqContext) {
    return retrieveFundCurrentFiscalYear(fundId, rqContext)
      .thenApply(fundCurrFY -> buildCurrentBudgetQuery(fundId, budgetStatus, fundCurrFY.getId()))
      .thenCompose(activeBudgetQuery -> budgetService.getBudgets(activeBudgetQuery, 0, Integer.MAX_VALUE, rqContext))
      .thenApply(this::getFirstBudget);
  }

  public CompletableFuture<List<ExpenseClass>> retrieveCurrentExpenseClasses(String fundId, String status, RequestContext rqContext) {
    CompletableFuture<List<ExpenseClass>> future = new VertxCompletableFuture<>(rqContext.getContext());
    retrieveCurrentBudget(fundId, null, rqContext)
      .thenCompose(currentBudget -> {
        logger.debug("Is Current budget for fund found : " + currentBudget.getId());
        return retrieveBudgetExpenseClasses(currentBudget, rqContext)
          .thenCombine(getBudgetExpenseClassIds(currentBudget.getId(), status, rqContext), (expenseClasses, budgetExpenseClassIds) ->
            expenseClasses.stream()
              .filter(expenseClass -> budgetExpenseClassIds.contains(expenseClass.getId()))
              .collect(Collectors.toList()));

      })
      .thenAccept(expenseClasses -> {
        logger.debug("Expense classes for fund size : " + expenseClasses.size());
        future.complete(expenseClasses);
      })
      .exceptionally(t -> {
        logger.error("Retrieve current expense classes for fund failed", t.getCause());
        future.completeExceptionally(t);
        return null;
      });
    return future;
  }

  private CompletableFuture<List<ExpenseClass>> retrieveBudgetExpenseClasses(Budget budget, RequestContext rqContext) {
    return Optional.ofNullable(budget)
      .map(budgetP -> expenseClassService.getExpenseClassesByBudgetId(budgetP.getId(), rqContext))
      .orElse(CompletableFuture.completedFuture(Collections.emptyList()));
  }

  private Budget getFirstBudget(BudgetsCollection budgetsCollection) {
    return Optional.ofNullable(budgetsCollection)
      .filter(budgetsCol -> !CollectionUtils.isEmpty(budgetsCol.getBudgets()))
      .map(BudgetsCollection::getBudgets)
      .map(budgets -> budgets.get(0))
      .orElseThrow(() -> new HttpException(404, CURRENT_BUDGET_NOT_FOUND.toError()));
  }

  private CompletableFuture<FiscalYear> getCurrentFiscalYear(String budgetLedgerId, RequestContext rqContext) {
    return ledgerDetailsService.getLedgerCurrentFiscalYear(budgetLedgerId, rqContext)
      .thenApply(fiscalYear ->
        Optional.ofNullable(fiscalYear)
          .orElseThrow(() -> new HttpException(404, CURRENT_FISCAL_YEAR_NOT_FOUND.toError()))
      );
  }

  private CompletableFuture<FiscalYear> getPlannedFiscalYear(String budgetLedgerId, RequestContext rqContext) {
    return ledgerDetailsService.getLedgerPlannedFiscalYear(budgetLedgerId, rqContext)
      .thenApply(fiscalYear ->
        Optional.ofNullable(fiscalYear)
          .orElseThrow(() -> new HttpException(404, PLANNED_FISCAL_YEAR_NOT_FOUND.toError()))
      );
  }

  private CompletableFuture<List<String>> getBudgetExpenseClassIds(String budgetId, String status, RequestContext rqContext){
    return budgetExpenseClassService.getBudgetExpenseClasses(budgetId, rqContext)
      .thenApply(expenseClasses ->
        expenseClasses.stream()
          .filter(budgetExpenseClass -> isBudgetExpenseClassWithStatus(budgetExpenseClass, status))
          .map(BudgetExpenseClass::getExpenseClassId)
          .collect(Collectors.toList())
      );
  }

  private String buildCurrentBudgetQuery(String fundId, String budgetStatus, String fundCurrFYId) {
    return StringUtils.isEmpty(budgetStatus) ? String.format(CURRENT_BUDGET_QUERY, fundId, fundCurrFYId)
      : String.format(CURRENT_BUDGET_QUERY_WITH_STATUS, fundId, fundCurrFYId, Budget.BudgetStatus.fromValue(budgetStatus).value());
  }

  private boolean isBudgetExpenseClassWithStatus(BudgetExpenseClass budgetExpenseClass, String status) {
    if ( Objects.nonNull(status)) {
      return budgetExpenseClass.getStatus().equals(Status.fromValue(status));
    }
    return true;
  }
}
