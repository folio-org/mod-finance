package org.folio.services;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.fund.FundDetailsService;
import org.folio.services.fund.FundService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

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
  private LedgerDetailsService fiscalYearService;
  @Mock
  private BudgetExpenseClassService budgetExpenseClassService;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @Test
  void testShouldReturnCurrentExistingBudget() {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Budget expBudget = new Budget().withId(UUID.randomUUID().toString()).withFundId(fundId).withFiscalYearId(fiscalId);
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget));
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);

    doReturn(completedFuture(fund)).when(fundService).retrieveFundById(fundId, requestContext);
    doReturn(completedFuture(fiscalYear)).when(fiscalYearService).getCurrentFiscalYear(ledgerId, requestContext);
    doReturn(completedFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    //When
    Budget actBudget = fundDetailsService.retrieveCurrentBudget(fundId, null, requestContext).join();
    //Then
    assertThat(actBudget, equalTo(expBudget));
    verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
  }

  @Test
  void testShouldReturnNullIfNoActiveBudget() {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);

    doReturn(completedFuture(fund)).when(fundService).retrieveFundById(fundId, requestContext);
    doReturn(completedFuture(fiscalYear)).when(fiscalYearService).getCurrentFiscalYear(ledgerId, requestContext);
    doReturn(completedFuture(null)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    //When
    Assertions.assertThrows(CompletionException.class, () -> fundDetailsService.retrieveCurrentBudget(fundId, null, requestContext).join());
  }

  @Test
  void testShouldReturnExpenseClassesIfCurrentBudgetExistForFund() {
    //Given
    String fiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String budgetId = UUID.randomUUID().toString();
    String expenseClassId = UUID.randomUUID().toString();

    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId, fiscalId);
    Budget expBudget = new Budget().withId(budgetId).withFundId(fundId).withFiscalYearId(fiscalId);
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget));
    BudgetExpenseClass budgetExpenseClass = new BudgetExpenseClass().withBudgetId(budgetId).withExpenseClassId(expenseClassId);
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalId);
    ExpenseClass expClasses = new ExpenseClass().withId(expenseClassId).withCode("El");

    doReturn(completedFuture(fund)).when(fundService).retrieveFundById(fundId, requestContext);
    doReturn(completedFuture(fiscalYear)).when(fiscalYearService).getCurrentFiscalYear(ledgerId, requestContext);
    doReturn(completedFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    doReturn(completedFuture(singletonList(expClasses))).when(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);
    doReturn(completedFuture(singletonList(budgetExpenseClass))).when(budgetExpenseClassService).getBudgetExpenseClasses(budgetId, requestContext);
    //When

    List<ExpenseClass> actClasses = fundDetailsService.retrieveCurrentExpenseClasses(fundId, null,requestContext).join();
    //Then
    assertEquals(expClasses.getId(), actClasses.get(0).getId());
    verify(fundService).retrieveFundById(fundId, requestContext);
    verify(fiscalYearService).getCurrentFiscalYear(ledgerId, requestContext);
    verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    verify(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);
  }

  @Test
  void testShouldThrowExceptionIfNoFundFoundById() {
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

    doReturn(completedFuture(null)).when(fundService).retrieveFundById(fundId, requestContext);
    doReturn(completedFuture(fiscalYear)).when(fiscalYearService).getCurrentFiscalYear(ledgerId, requestContext);
    doReturn(completedFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    doReturn(completedFuture(singletonList(expClasses))).when(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);
    //When
    Assertions.assertThrows(CompletionException.class, () -> fundDetailsService.retrieveCurrentExpenseClasses(fundId, null, requestContext).join());
  }

  @Test
  void testShouldThrowExceptionIfNoCurrentFiscalYear() {
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

    doReturn(completedFuture(fund)).when(fundService).retrieveFundById(fundId, requestContext);
    doReturn(completedFuture(null)).when(fiscalYearService).getCurrentFiscalYear(ledgerId, requestContext);
    doReturn(completedFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    doReturn(completedFuture(singletonList(expClasses))).when(expenseClassService).getExpenseClassesByBudgetId(budgetId, requestContext);
    //When
    Assertions.assertThrows(CompletionException.class, () -> fundDetailsService.retrieveCurrentExpenseClasses(fundId, null, requestContext).join());
  }
}
