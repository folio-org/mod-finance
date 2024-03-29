package org.folio.services.fund;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Collections.singletonList;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class FundDetailsServiceTest {
  private static final String X_ACTIVE_BUDGET_QUERY = "fundId==%s and fiscalYearId==%s";

  private RequestContext requestContext;

  @InjectMocks
  private FundDetailsService fundDetailsService;
  @Mock
  private BudgetService budgetService;
  @Mock
  private ExpenseClassService expenseClassService;
  @Mock
  private FundService fundService;
  @Mock
  private BudgetExpenseClassService budgetExpenseClassService;
  @Mock
  private FundFiscalYearService fundFiscalYearService;
  @Mock
  private FiscalYearService fiscalYearService;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @Test
  void testShouldReturnNullIfNoActiveBudget(VertxTestContext vertxTestContext) {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);

    doReturn(succeededFuture(fund)).when(fundService).getFundById(fundId, requestContext);
    doReturn(succeededFuture(fiscalYear)).when(fundFiscalYearService).retrieveCurrentFiscalYear(fundId, requestContext);
    doReturn(succeededFuture(null)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    //When
    var future = fundDetailsService.retrieveCurrentBudget(fundId, null, requestContext);

    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        assertEquals(HttpException.class, result.cause().getClass());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldReturnExpenseClassesIfCurrentBudgetExistForFund(VertxTestContext vertxTestContext) {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String budgetId = UUID.randomUUID().toString();
    String expenseClassId = UUID.randomUUID().toString();
    String status = "Active";
    boolean statusBoolean = true;

    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Budget expBudget = new Budget().withId(budgetId).withFundId(fundId).withFiscalYearId(fiscalId);
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget));
    BudgetExpenseClass budgetExpenseClass = new BudgetExpenseClass().withBudgetId(budgetId).withExpenseClassId(expenseClassId).withStatus(BudgetExpenseClass.Status.ACTIVE);
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);
    ExpenseClass expClasses = new ExpenseClass().withId(expenseClassId).withCode("El");

    boolean answer = fundDetailsService.isBudgetExpenseClassWithStatus(budgetExpenseClass, status);
    doReturn(succeededFuture(fund)).when(fundService).getFundById(fundId, requestContext);
    doReturn(succeededFuture(fiscalYear)).when(fundFiscalYearService).retrieveCurrentFiscalYear(fundId, requestContext);
    doReturn(succeededFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    doReturn(succeededFuture(singletonList(expClasses))).when(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);
    doReturn(succeededFuture(singletonList(budgetExpenseClass))).when(budgetExpenseClassService).getBudgetExpenseClasses(budgetId, requestContext);

    var future = fundDetailsService.retrieveExpenseClasses(fundId, null, null, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actClasses = result.result();
        assertEquals(statusBoolean, answer);
        assertEquals(expClasses.getId(), actClasses.get(0).getId());
        verify(fundFiscalYearService).retrieveCurrentFiscalYear(fundId, requestContext);
        verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
        verify(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);

        vertxTestContext.completeNow();
      });

  }

  @Test
  void testShouldReturnFundExpenseClassesForFiscalYear(VertxTestContext vertxTestContext) {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String budgetId = UUID.randomUUID().toString();
    String expenseClassId = UUID.randomUUID().toString();
    String status = "Active";
    boolean statusBoolean = true;

    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Budget expBudget = new Budget().withId(budgetId).withFundId(fundId).withFiscalYearId(fiscalId);
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget));
    BudgetExpenseClass budgetExpenseClass = new BudgetExpenseClass().withBudgetId(budgetId).withExpenseClassId(expenseClassId).withStatus(BudgetExpenseClass.Status.ACTIVE);
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);
    ExpenseClass expClasses = new ExpenseClass().withId(expenseClassId).withCode("El");

    boolean answer = fundDetailsService.isBudgetExpenseClassWithStatus(budgetExpenseClass, status);
    doReturn(succeededFuture(fiscalYear)).when(fiscalYearService).getFiscalYearById(fiscalId, requestContext);
    doReturn(succeededFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    doReturn(succeededFuture(singletonList(expClasses))).when(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);
    doReturn(succeededFuture(singletonList(budgetExpenseClass))).when(budgetExpenseClassService).getBudgetExpenseClasses(budgetId, requestContext);

    //When
    var future = fundDetailsService.retrieveExpenseClasses(fundId, fiscalId, null, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertEquals(statusBoolean, answer);
        assertEquals(expClasses.getId(), result.result().get(0).getId());
        verify(fiscalYearService).getFiscalYearById(fiscalId, requestContext);
        verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
        verify(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);

        vertxTestContext.completeNow();
      });

  }

  @Test
  void testShouldThrowExceptionIfNoFundFoundById(VertxTestContext vertxTestContext) {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String budgetId = UUID.randomUUID().toString();
    String expenseClassId = UUID.randomUUID().toString();

    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Optional<Budget> expBudget = Optional.of(new Budget().withId(budgetId).withFundId(fundId).withFiscalYearId(fiscalId));
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget.get()));
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);
    ExpenseClass expClasses = new ExpenseClass().withId(expenseClassId).withCode("El");

    doReturn(succeededFuture(null)).when(fundService).getFundById(fundId, requestContext);
    doReturn(succeededFuture(fiscalYear)).when(fundFiscalYearService).retrieveCurrentFiscalYear(fundId, requestContext);
    doReturn(succeededFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    doReturn(succeededFuture(singletonList(expClasses))).when(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);
    //When
    var future = fundDetailsService.retrieveExpenseClasses(fundId, null, null, requestContext);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        Assertions.assertNotNull(result.cause());
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testShouldThrowExceptionIfNoCurrentFiscalYear(VertxTestContext vertxTestContext) {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String budgetId = UUID.randomUUID().toString();
    String expenseClassId = UUID.randomUUID().toString();

    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Optional<Budget> expBudget = Optional.of(new Budget().withId(budgetId).withFundId(fundId).withFiscalYearId(fiscalId));
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget.get()));
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    ExpenseClass expClasses = new ExpenseClass().withId(expenseClassId).withCode("El");

    doReturn(succeededFuture(fund)).when(fundService).getFundById(fundId, requestContext);
    doReturn(succeededFuture(null)).when(fundFiscalYearService).retrieveCurrentFiscalYear(fundId, requestContext);
    doReturn(succeededFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    doReturn(succeededFuture(singletonList(expClasses))).when(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);

    var future = fundDetailsService.retrieveExpenseClasses(fundId, null, null, requestContext);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        Assertions.assertNotNull(result.cause());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testRetrieveCurrentBudget(VertxTestContext vertxTestContext) {
    String budgetStatus = "Active";
    boolean skipThrowException = true;

    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Budget expBudget = new Budget().withId(UUID.randomUUID().toString()).withFundId(fundId).withFiscalYearId(fiscalId);
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget));
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);

    doReturn(succeededFuture(fund)).when(fundService).getFundById(fundId, requestContext);
    doReturn(succeededFuture(fiscalYear)).when(fundFiscalYearService).retrieveCurrentFiscalYear(fundId, requestContext);
    doReturn(succeededFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    //When
    var future = fundDetailsService.retrieveCurrentBudget(fundId, null, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actBudget = result.result();
        assertThat(actBudget, equalTo(expBudget));
        verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);

        vertxTestContext.completeNow();
      });
    //Budget budget = fundDetailsService.retrieveCurrentBudget(fundId, budgetStatus, skipThrowException, requestContext).join();

  }

  @Test
  void testRetrieveBudgetForFiscalYear(VertxTestContext vertxTestContext) {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Budget expBudget = new Budget().withId(UUID.randomUUID().toString()).withFundId(fundId).withFiscalYearId(fiscalId);
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget));
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);

    doReturn(succeededFuture(fund)).when(fundService).getFundById(fundId, requestContext);
    doReturn(succeededFuture(fiscalYear)).when(fiscalYearService).getFiscalYearById(fiscalId, requestContext);
    doReturn(succeededFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    //When
    var future = fundDetailsService.retrieveBudget(fundId, fiscalId, null, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actBudget = result.result();
        assertThat(actBudget, equalTo(expBudget));
        verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);

        vertxTestContext.completeNow();
      });

  }
}
