package org.folio.services;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.folio.ApiTestSuite.mockPort;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.impl.ApiTestBase.X_OKAPI_TENANT;
import static org.folio.rest.impl.ApiTestBase.X_OKAPI_TOKEN;
import static org.folio.rest.impl.ApiTestBase.X_OKAPI_USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.Times;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class FundDetailsServiceTest {
  private static final String X_ACTIVE_BUDGET_QUERY = "query=fundId==%s and budgetStatus==Active";

  private RequestContext requestContext;

  @InjectMocks
  private FundDetailsService fundDetailsService;

  @Mock
  private BudgetService budgetService;

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

//  @Test
//  public void testShouldReturnActiveExistingBudget() {
//    String fundId = UUID.randomUUID().toString();
//    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId);
//    //Given
//    Budget expBudget = new Budget().withId(UUID.randomUUID().toString()).withFundId(fundId);
//    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget));
//
//    doReturn(completedFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
//    //When
//    Budget actBudget = fundDetailsService.retrieveActiveBudget(fundId, requestContext).join();
//    //Then
//    assertThat(actBudget, equalTo(expBudget));
//    verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
//  }
//
//  @Test
//  public void testShouldReturnNullIfNoActiveBudget() {
//    String fundId = UUID.randomUUID().toString();
//    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId);
//    //Given
//    doReturn(completedFuture(null)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
//    //When
//    Budget actBudget = fundDetailsService.retrieveActiveBudget(fundId, requestContext).join();
//    //Then
//    assertNull(actBudget);
//    verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
//  }
//
//  @Test
//  public void testShouldReturnNullIfBudgetCollectionWithoutActiveBudget() {
//    String fundId = UUID.randomUUID().toString();
//    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId);
//    //Given
//    doReturn(completedFuture(new BudgetsCollection())).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
//    //When
//    Budget actBudget = fundDetailsService.retrieveActiveBudget(fundId, requestContext).join();
//    //Then
//    assertNull(actBudget);
//    verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
//  }

  @Test
  public void testShouldReturnExpenseClassesForActiveBudget() {
    //Given
    String fundId = UUID.randomUUID().toString();
    String budgetId = UUID.randomUUID().toString();
    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId);
    Budget expBudget = new Budget().withId(budgetId).withFundId(fundId);
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(singletonList(expBudget));
    String expenseClassId = UUID.randomUUID().toString();
    BudgetExpenseClass expClass = new BudgetExpenseClass().withId(UUID.randomUUID().toString())
      .withExpenseClassId(expenseClassId).withBudgetId(budgetId);
    doReturn(completedFuture(budgetsCollection)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    doReturn(completedFuture(singletonList(expClass))).when(budgetExpenseClassService).getBudgetExpenseClasses(budgetId, requestContext);
    //When
    List<BudgetExpenseClass> actClasses = fundDetailsService.retrieveCurrentExpenseClasses(fundId, requestContext).join();
    //Then
    assertEquals(expClass, actClasses.get(0));
    verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    verify(budgetExpenseClassService).getBudgetExpenseClasses(budgetId, requestContext);
  }

  @Test
  public void testShouldReturnEmptyListIfNoActiveBudget() {
    //Given
    String fundId = UUID.randomUUID().toString();
    String budgetId = UUID.randomUUID().toString();
    String query = String.format(X_ACTIVE_BUDGET_QUERY, fundId);

    doReturn(completedFuture(null)).when(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    //When
    List<BudgetExpenseClass> actClasses = fundDetailsService.retrieveCurrentExpenseClasses(fundId, requestContext).join();
    //Then
    assertEquals(emptyList(), actClasses);
    verify(budgetService).getBudgets(query, 0, Integer.MAX_VALUE, requestContext);
    verify(budgetExpenseClassService, new Times(0)).getBudgetExpenseClasses(budgetId, requestContext);
  }
}
