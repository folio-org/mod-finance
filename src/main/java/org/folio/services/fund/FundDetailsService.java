package org.folio.services.fund;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;
import static org.folio.rest.util.ErrorCodes.CURRENT_BUDGET_NOT_FOUND;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClass.Status;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;

import io.vertx.core.Future;

public class FundDetailsService {
  private static final Logger logger = LogManager.getLogger(FundDetailsService.class);
  private static final String BUDGET_QUERY_WITH_STATUS = "fundId==%s and fiscalYearId==%s and budgetStatus==%s";
  private static final String BUDGET_QUERY = "fundId==%s and fiscalYearId==%s";

  private final BudgetService budgetService;
  private final ExpenseClassService expenseClassService;
  private final BudgetExpenseClassService budgetExpenseClassService;
  private final FundFiscalYearService fundFiscalYearService;
  private final FiscalYearService fiscalYearService;

  public FundDetailsService(BudgetService budgetService, ExpenseClassService expenseClassService,
                            BudgetExpenseClassService budgetExpenseClassService, FundFiscalYearService fundFiscalYearService,
                            FiscalYearService fiscalYearService) {
    this.budgetService = budgetService;
    this.expenseClassService = expenseClassService;
    this.budgetExpenseClassService = budgetExpenseClassService;
    this.fundFiscalYearService = fundFiscalYearService;
    this.fiscalYearService = fiscalYearService;
  }

  public Future<Budget> retrieveCurrentBudget(String fundId, String budgetStatus, boolean skipThrowException,
                                                         RequestContext rqContext) {
    return retrieveCurrentBudget(fundId, budgetStatus, rqContext)
      .recover(t -> {
        logger.error("Failed to retrieve current budget for fundId: {}", fundId, t);
        if (skipThrowException) {
          return Future.succeededFuture();
        } else {
          return Future.failedFuture(t);
        }
      });
  }

  public Future<Budget> retrieveCurrentBudget(String fundId, String budgetStatus, RequestContext rqContext) {
    return retrieveBudget(fundId, null, budgetStatus, rqContext);
  }

  /**
   * Retrieve fund's budget for fiscalYearId passed,
   * or for current fiscal year if fiscalYearId is null or empty
   *
   * @param fundId       the id of fund budget belongs
   * @param fiscalYearId the fiscal year of budget to retrieve (null for current fiscal year)
   * @param budgetStatus the status of budget to search for. Null for all statuses
   * @param rqContext    the request context
   * @return budget completable future
   */
  public Future<Budget> retrieveBudget(String fundId, String fiscalYearId, String budgetStatus, RequestContext rqContext) {
    Future<FiscalYear> fiscalYear = StringUtils.isNotEmpty(fiscalYearId) ? fiscalYearService
      .getFiscalYearById(fiscalYearId, rqContext) : fundFiscalYearService.retrieveCurrentFiscalYear(fundId, rqContext);
    return fiscalYear.map(fundFY -> buildBudgetQuery(fundId, budgetStatus, fundFY.getId()))
      .compose(activeBudgetQuery -> budgetService.getBudgets(activeBudgetQuery, 0, Integer.MAX_VALUE, rqContext))
      .map(this::getFirstBudget);
  }

  /**
   * Retrieve fund's expense classes for fiscalYearId passed,
   * or for current fiscal year if fiscalYearId is null or empty
   *
   * @param fundId       the id of fund expense classes belong
   * @param fiscalYearId the fiscal year of expense classes to retrieve (null for current fiscal year)
   * @param budgetStatus the status of budget to search for (to which expense classes belong). Null for all statuses
   * @param rqContext    the request context
   * @return expense classes completable future
   */
  public Future<List<ExpenseClass>> retrieveExpenseClasses(String fundId, String fiscalYearId,
                                                           String budgetStatus, RequestContext rqContext) {

    return retrieveBudget(fundId, fiscalYearId, null, rqContext)
      .compose(budget -> {
        logger.debug("retrieveExpenseClasses:: budget id='{}' was found for fund id='{}': ",budget.getId(), fundId);
        var expenseClasses = getExpenseClasses(budget, rqContext);
        var budgetExpenseClassIds  = getBudgetExpenseClassIds(budget.getId(), budgetStatus, rqContext);

        return GenericCompositeFuture.join(List.of(expenseClasses, budgetExpenseClassIds))
          .map(cf -> expenseClasses.result().stream()
            .filter(expenseClass -> budgetExpenseClassIds.result().contains(expenseClass.getId()))
            .collect(toList()));
      })
      .onSuccess(expenseClasses -> logger.debug("retrieveExpenseClasses:: found expense classes for fund id='{}', size={} ", fundId, expenseClasses.size()))
      .onFailure(t -> logger.error("Retrieve expense classes for fund id='{}' failed", fundId, t));
  }

  private Future<List<ExpenseClass>> getExpenseClasses(Budget budget, RequestContext rqContext) {
    return Optional.ofNullable(budget)
      .map(budgetP -> expenseClassService.getExpenseClassesByBudgetId(budgetP.getId(), rqContext))
      .orElse(succeededFuture(Collections.emptyList()));
  }

  private Budget getFirstBudget(BudgetsCollection budgetsCollection) {
    return Optional.ofNullable(budgetsCollection)
      .filter(budgetsCol -> !CollectionUtils.isEmpty(budgetsCol.getBudgets()))
      .map(BudgetsCollection::getBudgets)
      .map(budgets -> budgets.get(0))
      .orElseThrow(() -> new HttpException(404, CURRENT_BUDGET_NOT_FOUND.toError()));
  }

  private Future<List<String>> getBudgetExpenseClassIds(String budgetId, String status, RequestContext rqContext) {
    return budgetExpenseClassService.getBudgetExpenseClasses(budgetId, rqContext)
      .map(expenseClasses -> expenseClasses.stream()
        .filter(budgetExpenseClass -> isBudgetExpenseClassWithStatus(budgetExpenseClass, status))
        .map(BudgetExpenseClass::getExpenseClassId)
        .collect(toList()));
  }

  private String buildBudgetQuery(String fundId, String budgetStatus, String fundFYId) {
    return StringUtils.isEmpty(budgetStatus) ? String.format(BUDGET_QUERY, fundId, fundFYId)
      : String.format(BUDGET_QUERY_WITH_STATUS, fundId, fundFYId, Budget.BudgetStatus.fromValue(budgetStatus)
      .value());
  }

  public boolean isBudgetExpenseClassWithStatus(BudgetExpenseClass budgetExpenseClass, String status) {
    if (Objects.nonNull(status)) {
      return budgetExpenseClass.getStatus()
        .equals(Status.fromValue(status));
    }
    return true;
  }


}
