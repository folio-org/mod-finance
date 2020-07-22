package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotal;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.ExpenseClassStatus;
import org.folio.services.BudgetExpenseClassTotalsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BudgetsApiTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(BudgetsApiTest.class);
  public static final String BUDGET_WITH_BOUNDED_TRANSACTION_ID = "34fe0c8b-2b99-4fe2-81a5-4ed6872a32e8";
  public static BudgetExpenseClassTotalsService mockService = mock(BudgetExpenseClassTotalsService.class);

  @Test
  public void testUpdateBudgetWithExceededAllowableAmounts() {
    logger.info("=== Test Update budget with exceeded limit of allowable encumbrance and expenditures ===");

    Budget budget = verifyGet(BUDGET.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(Budget.class);

    // set budget allowable amounts to 1%
    budget.setAllowableEncumbrance(1d);
    budget.setAllowableExpenditure(1d);

    budget.setAwaitingPayment((double) Integer.MAX_VALUE);

    verifyPut(BUDGET.getEndpointWithDefaultId(), budget, APPLICATION_JSON, 422).then()
      .body(containsString(ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED.getCode()), containsString(ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED.getCode()));
  }

  @Test
  public void testDeleteShouldFailIfThereAreTransactionBoundedToBudget() {
    logger.info("=== Test Delete of the budget is forbidden, if budget related transactions found ===");

    Errors errors = verifyDeleteResponse(BUDGET.getEndpointWithId(BUDGET_WITH_BOUNDED_TRANSACTION_ID), APPLICATION_JSON, 400).then()
      .extract()
      .as(Errors.class);

    assertEquals(TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR.getDescription(), errors.getErrors().get(0).getMessage());
    assertEquals(TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR.getCode(), errors.getErrors().get(0).getCode());
  }

  @Test
  public void testDeleteShouldSuccessIfNoTransactionBoundedToBudget() {
    logger.info("=== Test Delete of the budget, if no transactions were found. ===");

    verifyDeleteResponse(BUDGET.getEndpointWithDefaultId(), EMPTY, 204);
  }

  @Test
  void getFinanceBudgetsExpenseClassesTotalsById() {
    BudgetExpenseClassTotal expenseClassTotal =  new BudgetExpenseClassTotal();
    expenseClassTotal.withId(UUID.randomUUID().toString())
      .withExpenseClassName("test")
      .withExpenseClassStatus(ExpenseClassStatus.Status.ACTIVE)
      .withEncumbered(0d)
      .withAwaitingPayment(50d)
      .withExpended(100d)
      .withPercentageExpended(100d);
    BudgetExpenseClassTotalsCollection expectedExpenseClassTotalsCollection = new BudgetExpenseClassTotalsCollection();
    expectedExpenseClassTotalsCollection.withBudgetExpenseClassTotals(Collections.singletonList(expenseClassTotal))
      .withTotalRecords(1);
    when(mockService.getExpenseClassTotals(anyString(), ArgumentMatchers.any(), anyMap())).thenReturn(CompletableFuture.completedFuture(expectedExpenseClassTotalsCollection));
    String budgetId = UUID.randomUUID().toString();

    BudgetExpenseClassTotalsCollection resultExpenseClassTotal = verifyGet(String.format("/finance/budgets/%s/expense-classes-totals", budgetId), APPLICATION_JSON, 200).as(BudgetExpenseClassTotalsCollection.class);

    verify(mockService).getExpenseClassTotals(eq(budgetId), ArgumentMatchers.any(), anyMap());
    assertEquals(expectedExpenseClassTotalsCollection, resultExpenseClassTotal);
  }

  @Test
  void getFinanceBudgetsExpenseClassesTotalsByIdWithError() {
    CompletableFuture<BudgetExpenseClassTotalsCollection> future = new CompletableFuture<>();
    future.completeExceptionally(new HttpException(400, GENERIC_ERROR_CODE));
    when(mockService.getExpenseClassTotals(anyString(), ArgumentMatchers.any(), anyMap())).thenReturn(future);
    String budgetId = UUID.randomUUID().toString();

    verifyGet(String.format("/finance/budgets/%s/expense-classes-totals", budgetId), APPLICATION_JSON, 400);

    verify(mockService).getExpenseClassTotals(eq(budgetId), ArgumentMatchers.any(), anyMap());
  }

  /**
   * Define unit test specific beans to override actual ones
   */
  @Configuration
  static class ContextConfiguration {

    @Bean("budgetExpenseClassTotalsMockService")
    @Primary
    public BudgetExpenseClassTotalsService budgetExpenseClassTotalsService() {
      return mockService;
    }
  }

}
