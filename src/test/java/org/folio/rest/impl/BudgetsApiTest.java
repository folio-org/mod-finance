package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.jaxrs.model.BudgetExpenseClassTotal.ExpenseClassStatus.ACTIVE;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.ApiTestSuite;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotal;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.util.RestTestUtils;
import org.folio.rest.util.TestEntities;
import org.folio.services.budget.BudgetExpenseClassTotalsService;
import org.folio.services.budget.BudgetService;
import org.folio.services.budget.CreateBudgetService;
import org.folio.services.budget.RecalculateBudgetService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.vertx.core.Future;

public class BudgetsApiTest  {

  private static final Logger logger = LogManager.getLogger(BudgetsApiTest.class);
  public static final String BUDGET_WITH_BOUNDED_TRANSACTION_ID = "34fe0c8b-2b99-4fe2-81a5-4ed6872a32e8";
  @Autowired
  public BudgetExpenseClassTotalsService budgetExpenseClassTotalsMockService;
  @Autowired
  public BudgetService budgetMockService;
  @Autowired
  public RecalculateBudgetService recalculateBudgetMockService;
  @Autowired
  public CreateBudgetService mockCreateBudgetService;

  private static boolean runningOnOwn;

  @BeforeAll
  public static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(BudgetsApiTest.ContextConfiguration.class);
  }

  @AfterAll
  public static void after() {
    clearVertxContext();
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterEach
  void resetMocks() {
    reset(budgetExpenseClassTotalsMockService);
    reset(budgetMockService);
    reset(recalculateBudgetMockService);
    reset(mockCreateBudgetService);
  }

  @Test
  void postBudget() {
    SharedBudget budget = TestEntities.BUDGET.getMockObject().mapTo(SharedBudget.class);

    when(mockCreateBudgetService.createBudget(any(), any())).thenReturn(succeededFuture(budget));

    SharedBudget resultBudget = RestTestUtils.verifyPostResponse(TestEntities.BUDGET.getEndpoint(), budget, APPLICATION_JSON, CREATED.getStatusCode()).as(SharedBudget.class);


    assertEquals(budget, resultBudget);
    verify(mockCreateBudgetService).createBudget(any(), any());
  }

  @Test
  void postBudgetWithErrorFromService() {
    Future<SharedBudget> errorFuture = Future.failedFuture(new HttpException(INTERNAL_SERVER_ERROR.getStatusCode(), GENERIC_ERROR_CODE));

    when(mockCreateBudgetService.createBudget(any(), any())).thenReturn(errorFuture);

    Errors errors = RestTestUtils.verifyPostResponse(TestEntities.BUDGET.getEndpoint(), TestEntities.BUDGET.getMockObject().mapTo(SharedBudget.class), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals(GENERIC_ERROR_CODE.toError(), errors.getErrors().get(0));
    verify(mockCreateBudgetService).createBudget(any(), any());
  }

  @Test
  void putBudgetIdMismatch() {
    String id = UUID.randomUUID().toString();
    SharedBudget budget = TestEntities.BUDGET.getMockObject().mapTo(SharedBudget.class)
      .withId(UUID.randomUUID().toString());

    Errors errors = RestTestUtils.verifyPut(TestEntities.BUDGET.getEndpointWithId(id), budget, APPLICATION_JSON, 422).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    Error expectedError = MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError();
    assertEquals(expectedError, errors.getErrors().get(0));
    verify(budgetMockService, never()).updateBudget(any(), any());
  }

  @Test
  void putBudgetWithEmptyIdInBody() {
    String id = UUID.randomUUID().toString();
    SharedBudget budget = TestEntities.BUDGET.getMockObject().mapTo(SharedBudget.class)
      .withId(null);

    when(budgetMockService.updateBudget(any(), any())).thenReturn(succeededFuture(null));

    RestTestUtils.verifyPut(TestEntities.BUDGET.getEndpointWithId(id), budget, "", NO_CONTENT.getStatusCode());

    final ArgumentCaptor<SharedBudget> budgetArgumentCaptor = ArgumentCaptor.forClass(SharedBudget.class);
    verify(budgetMockService).updateBudget(budgetArgumentCaptor.capture(), any());
    SharedBudget argumentBudget = budgetArgumentCaptor.getValue();
    assertEquals(id, argumentBudget.getId());
  }

  @Test
  void testGetBudgetById() {
    SharedBudget budget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withStatusExpenseClasses(Collections.singletonList(new StatusExpenseClass()
        .withExpenseClassId(UUID.randomUUID().toString())
        .withStatus(StatusExpenseClass.Status.ACTIVE)));

    when(budgetMockService.getBudgetById(anyString(), any())).thenReturn(succeededFuture(budget));

    SharedBudget resultBudget = RestTestUtils.verifyGet(TestEntities.BUDGET.getEndpointWithId(budget.getId()), APPLICATION_JSON, OK.getStatusCode()).as(SharedBudget.class);

    assertEquals(budget, resultBudget);

    verify(budgetMockService).getBudgetById(eq(budget.getId()), any(RequestContext.class));
  }

  @Test
  void testGetBudgetByIdWithError() {
    Future<SharedBudget> errorFuture = Future.failedFuture(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
    String budgetId = UUID.randomUUID().toString();

    when(budgetMockService.getBudgetById(anyString(), any())).thenReturn(errorFuture);

    Errors errors = RestTestUtils.verifyGet(TestEntities.BUDGET.getEndpointWithId(budgetId), APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    Error expectedError = GENERIC_ERROR_CODE.toError().withMessage(NOT_FOUND.getReasonPhrase());
    assertEquals(expectedError,  errors.getErrors().get(0));

    verify(budgetMockService).getBudgetById(eq(budgetId), any(RequestContext.class));
  }

  @Test
  void testGetBudgetCollectionApiEmptyQueryParams() {

    BudgetsCollection budgetCollection = new BudgetsCollection();
    when(budgetMockService.getBudgets(isNull(), anyInt(), anyInt(), any())).thenReturn(succeededFuture(budgetCollection));
    BudgetsCollection budgetCollectionResult = RestTestUtils.verifyGet(TestEntities.BUDGET.getEndpoint(), APPLICATION_JSON, OK.getStatusCode()).as(BudgetsCollection.class);

    assertEquals(budgetCollection, budgetCollectionResult);
    verify(budgetMockService).getBudgets(isNull(), eq(0), eq(10), any(RequestContext.class));
  }

  @Test
  void testGetBudgetCollectionApiWithQueryParams() {

    String budgetId = UUID.randomUUID().toString();
    BudgetsCollection budgetCollection = new BudgetsCollection()
      .withBudgets(Collections.singletonList(new Budget().withId(budgetId)));

    when(budgetMockService.getBudgets(anyString(), anyInt(), anyInt(), any())).thenReturn(succeededFuture(budgetCollection));

    String query = "id=" + budgetId;
    int limit = 50;
    int offset = 4;
    String url = String.format("%s?query=%s&limit=%d&offset=%d", TestEntities.BUDGET.getEndpoint(), query, limit, offset);
    BudgetsCollection budgetCollectionResult = RestTestUtils.verifyGet(url, APPLICATION_JSON, OK.getStatusCode()).as(BudgetsCollection.class);

    assertEquals(budgetCollection, budgetCollectionResult);
    verify(budgetMockService).getBudgets(eq(query), eq(offset), eq(limit), any(RequestContext.class));
  }

  @Test
  void testGetBudgetCollectionApiWithError() {

    Future<BudgetsCollection> errorFuture = Future.failedFuture(new HttpException(422, "Test error"));

    when(budgetMockService.getBudgets(isNull(), anyInt(), anyInt(), any())).thenReturn(errorFuture);

    Errors errors = RestTestUtils.verifyGet(TestEntities.BUDGET.getEndpoint(), APPLICATION_JSON, 422).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals("Test error", errors.getErrors().get(0).getMessage());
    assertEquals(GENERIC_ERROR_CODE.getCode(), errors.getErrors().get(0).getCode());
    verify(budgetMockService).getBudgets(isNull(), eq(0), eq(10), any(RequestContext.class));
  }


  @Test
  void testDeleteShouldFailIfThereAreTransactionBoundedToBudget() {
    logger.info("=== Test Delete of the budget is forbidden, if budget related transactions found ===");
    Future<Void> future = Future.failedFuture(new HttpException(400, TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR));
    when(budgetMockService.deleteBudget(anyString(), any())).thenReturn(future);

    Errors errors = RestTestUtils.verifyDeleteResponse(TestEntities.BUDGET.getEndpointWithId(BUDGET_WITH_BOUNDED_TRANSACTION_ID), APPLICATION_JSON, 400).then()
      .extract()
      .as(Errors.class);

    assertEquals(TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR.toError(), errors.getErrors().get(0));
    verify(budgetMockService).deleteBudget(eq(BUDGET_WITH_BOUNDED_TRANSACTION_ID), any(RequestContext.class));
  }

  @Test
  void testDeleteShouldSuccessIfNoTransactionBoundedToBudget() {
    logger.info("=== Test Delete of the budget, if no transactions were found. ===");
    when(budgetMockService.deleteBudget(anyString(), any())).thenReturn(succeededFuture(null));
    RestTestUtils.verifyDeleteResponse(TestEntities.BUDGET.getEndpointWithDefaultId(), EMPTY, 204);
    verify(budgetMockService).deleteBudget(eq(TestEntities.BUDGET.getMockObject().getString(ID)), any(RequestContext.class));
  }

  @Test
  void getFinanceBudgetsExpenseClassesTotalsById() {
    BudgetExpenseClassTotal expenseClassTotal =  new BudgetExpenseClassTotal();
    expenseClassTotal.withId(UUID.randomUUID().toString())
      .withExpenseClassName("test")
      .withExpenseClassStatus(ACTIVE)
      .withEncumbered(0d)
      .withAwaitingPayment(50d)
      .withExpended(100d)
      .withPercentageExpended(100d);
    BudgetExpenseClassTotalsCollection expectedExpenseClassTotalsCollection = new BudgetExpenseClassTotalsCollection();
    expectedExpenseClassTotalsCollection.withBudgetExpenseClassTotals(Collections.singletonList(expenseClassTotal))
      .withTotalRecords(1);
    when(budgetExpenseClassTotalsMockService.getExpenseClassTotals(anyString(), ArgumentMatchers.any())).thenReturn(succeededFuture(expectedExpenseClassTotalsCollection));
    String budgetId = UUID.randomUUID().toString();

    BudgetExpenseClassTotalsCollection resultExpenseClassTotal = RestTestUtils.verifyGet(String.format("/finance/budgets/%s/expense-classes-totals", budgetId), APPLICATION_JSON, 200).as(BudgetExpenseClassTotalsCollection.class);

    verify(budgetExpenseClassTotalsMockService).getExpenseClassTotals(eq(budgetId), ArgumentMatchers.any());
    assertEquals(expectedExpenseClassTotalsCollection, resultExpenseClassTotal);
  }

  @Test
  void getFinanceBudgetsExpenseClassesTotalsByIdWithError() {
    Future<BudgetExpenseClassTotalsCollection> future = Future.failedFuture(new HttpException(400, GENERIC_ERROR_CODE));
    when(budgetExpenseClassTotalsMockService.getExpenseClassTotals(anyString(), ArgumentMatchers.any())).thenReturn(future);
    String budgetId = UUID.randomUUID().toString();

    RestTestUtils.verifyGet(String.format("/finance/budgets/%s/expense-classes-totals", budgetId), APPLICATION_JSON, 400);

    verify(budgetExpenseClassTotalsMockService).getExpenseClassTotals(eq(budgetId), ArgumentMatchers.any());
  }

  @Test
  void postFinanceBudgetsRecalculateById() {
    when(recalculateBudgetMockService.recalculateBudget(anyString(), ArgumentMatchers.any())).thenReturn(succeededFuture());
    String budgetId = UUID.randomUUID().toString();

    RestTestUtils.verifyPostResponse(String.format("/finance/budgets/%s/recalculate", budgetId), null, "", 204);

    verify(recalculateBudgetMockService).recalculateBudget(eq(budgetId), ArgumentMatchers.any());
  }

  @Test
  void postFinanceBudgetsRecalculateByIdWithError() {
    Future<Void> failedFuture = Future.failedFuture(new HttpException(400, GENERIC_ERROR_CODE));
    when(recalculateBudgetMockService.recalculateBudget(anyString(), ArgumentMatchers.any())).thenReturn(failedFuture);
    String budgetId = UUID.randomUUID().toString();

    RestTestUtils.verifyPostResponse(String.format("/finance/budgets/%s/recalculate", budgetId), null, APPLICATION_JSON, 400);

    verify(recalculateBudgetMockService).recalculateBudget(eq(budgetId), ArgumentMatchers.any());
  }

  /**
   * Define unit test specific beans to override actual ones
   */
  static class ContextConfiguration {

    @Bean
    public BudgetExpenseClassTotalsService budgetExpenseClassTotalsService() {
      return mock(BudgetExpenseClassTotalsService.class);
    }

    @Bean
    public BudgetService budgetService() {
      return mock(BudgetService.class);
    }

    @Bean
    public RecalculateBudgetService recalculateBudgetService() {
      return mock(RecalculateBudgetService.class);
    }

    @Bean
    public CreateBudgetService createBudgetService() {
      return mock(CreateBudgetService.class);
    }
  }

}
