package org.folio.rest.impl;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.folio.rest.util.TestEntities;
import org.folio.services.BudgetExpenseClassTotalsService;
import org.folio.services.BudgetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BudgetsApiTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(BudgetsApiTest.class);
  public static final String BUDGET_WITH_BOUNDED_TRANSACTION_ID = "34fe0c8b-2b99-4fe2-81a5-4ed6872a32e8";
  public static BudgetExpenseClassTotalsService budgetExpenseClassTotalsMockService = mock(BudgetExpenseClassTotalsService.class);
  public static BudgetService budgetMockService = mock(BudgetService.class);

  @AfterEach
  void clearMocks() {
    Mockito.reset(budgetMockService);
    Mockito.reset(budgetExpenseClassTotalsMockService);
  }

  @Test
  void postBudget() {
    SharedBudget budget = TestEntities.BUDGET.getMockObject().mapTo(SharedBudget.class);

    when(budgetMockService.createBudget(any(), any())).thenReturn(CompletableFuture.completedFuture(budget));

    SharedBudget resultBudget = verifyPostResponse(TestEntities.BUDGET.getEndpoint(), budget, APPLICATION_JSON, CREATED.getStatusCode()).as(SharedBudget.class);


    assertEquals(budget, resultBudget);
    verify(budgetMockService).createBudget(refEq(budget, "metadata"), any());
  }

  @Test
  void postBudgetWithErrorFromService() {
    CompletableFuture<SharedBudget> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new HttpException(INTERNAL_SERVER_ERROR.getStatusCode(), GENERIC_ERROR_CODE));

    when(budgetMockService.createBudget(any(), any())).thenReturn(errorFuture);

    Errors errors = verifyPostResponse(TestEntities.BUDGET.getEndpoint(), TestEntities.BUDGET.getMockObject().mapTo(SharedBudget.class), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals(GENERIC_ERROR_CODE.toError(), errors.getErrors().get(0));
    verify(budgetMockService).createBudget(any(), any());
  }

  @Test
  void putBudgetIdMismatch() {
    String id = UUID.randomUUID().toString();
    SharedBudget budget = TestEntities.BUDGET.getMockObject().mapTo(SharedBudget.class)
      .withId(UUID.randomUUID().toString());

    Errors errors = verifyPut(TestEntities.BUDGET.getEndpointWithId(id), budget, APPLICATION_JSON, 422).as(Errors.class);

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

    when(budgetMockService.updateBudget(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    verifyPut(TestEntities.BUDGET.getEndpointWithId(id), budget, "", NO_CONTENT.getStatusCode());

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

    when(budgetMockService.getBudgetById(anyString(), any())).thenReturn(CompletableFuture.completedFuture(budget));

    SharedBudget resultBudget = verifyGet(TestEntities.BUDGET.getEndpointWithId(budget.getId()), APPLICATION_JSON, OK.getStatusCode()).as(SharedBudget.class);

    assertEquals(budget, resultBudget);

    verify(budgetMockService).getBudgetById(eq(budget.getId()), any(RequestContext.class));
  }

  @Test
  void testGetBudgetByIdWithError() {
    CompletableFuture<SharedBudget> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
    String budgetId = UUID.randomUUID().toString();

    when(budgetMockService.getBudgetById(anyString(), any())).thenReturn(errorFuture);

    Errors errors = verifyGet(TestEntities.BUDGET.getEndpointWithId(budgetId), APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    Error expectedError = GENERIC_ERROR_CODE.toError().withMessage(NOT_FOUND.getReasonPhrase());
    assertEquals(expectedError,  errors.getErrors().get(0));

    verify(budgetMockService).getBudgetById(eq(budgetId), any(RequestContext.class));
  }

  @Test
  void testGetBudgetCollectionApiEmptyQueryParams() {

    BudgetsCollection budgetCollection = new BudgetsCollection();
    when(budgetMockService.getBudgets(isNull(), anyInt(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(budgetCollection));
    BudgetsCollection budgetCollectionResult = verifyGet(TestEntities.BUDGET.getEndpoint(), APPLICATION_JSON, OK.getStatusCode()).as(BudgetsCollection.class);

    assertEquals(budgetCollection, budgetCollectionResult);
    verify(budgetMockService).getBudgets(isNull(), eq(0), eq(10), any(RequestContext.class));
  }

  @Test
  void testGetBudgetCollectionApiWithQueryParams() {

    String budgetId = UUID.randomUUID().toString();
    BudgetsCollection budgetCollection = new BudgetsCollection()
      .withBudgets(Collections.singletonList(new Budget().withId(budgetId)));

    when(budgetMockService.getBudgets(anyString(), anyInt(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(budgetCollection));

    String query = "id=" + budgetId;
    int limit = 50;
    int offset = 4;
    String url = String.format("%s?query=%s&limit=%d&offset=%d", TestEntities.BUDGET.getEndpoint(), query, limit, offset);
    BudgetsCollection budgetCollectionResult = verifyGet(url, APPLICATION_JSON, OK.getStatusCode()).as(BudgetsCollection.class);

    assertEquals(budgetCollection, budgetCollectionResult);
    verify(budgetMockService).getBudgets(eq(query), eq(offset), eq(limit), any(RequestContext.class));
  }

  @Test
  void testGetBudgetCollectionApiWithError() {

    CompletableFuture<BudgetsCollection> errorFuture = new CompletableFuture<>();

    errorFuture.completeExceptionally(new HttpException(422, "Test error"));

    when(budgetMockService.getBudgets(isNull(), anyInt(), anyInt(), any())).thenReturn(errorFuture);

    Errors errors = verifyGet(TestEntities.BUDGET.getEndpoint(), APPLICATION_JSON, 422).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals("Test error", errors.getErrors().get(0).getMessage());
    assertEquals(GENERIC_ERROR_CODE.getCode(), errors.getErrors().get(0).getCode());
    verify(budgetMockService).getBudgets(isNull(), eq(0), eq(10), any(RequestContext.class));
  }


  @Test
  void testDeleteShouldFailIfThereAreTransactionBoundedToBudget() {
    logger.info("=== Test Delete of the budget is forbidden, if budget related transactions found ===");
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new HttpException(400, TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR));
    when(budgetMockService.deleteBudget(anyString(), any())).thenReturn(future);

    Errors errors = verifyDeleteResponse(TestEntities.BUDGET.getEndpointWithId(BUDGET_WITH_BOUNDED_TRANSACTION_ID), APPLICATION_JSON, 400).then()
      .extract()
      .as(Errors.class);

    assertEquals(TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR.toError(), errors.getErrors().get(0));
    verify(budgetMockService).deleteBudget(eq(BUDGET_WITH_BOUNDED_TRANSACTION_ID), any(RequestContext.class));
  }

  @Test
  void testDeleteShouldSuccessIfNoTransactionBoundedToBudget() {
    logger.info("=== Test Delete of the budget, if no transactions were found. ===");
    when(budgetMockService.deleteBudget(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
    verifyDeleteResponse(TestEntities.BUDGET.getEndpointWithDefaultId(), EMPTY, 204);
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
    when(budgetExpenseClassTotalsMockService.getExpenseClassTotals(anyString(), ArgumentMatchers.any())).thenReturn(CompletableFuture.completedFuture(expectedExpenseClassTotalsCollection));
    String budgetId = UUID.randomUUID().toString();

    BudgetExpenseClassTotalsCollection resultExpenseClassTotal = verifyGet(String.format("/finance/budgets/%s/expense-classes-totals", budgetId), APPLICATION_JSON, 200).as(BudgetExpenseClassTotalsCollection.class);

    verify(budgetExpenseClassTotalsMockService).getExpenseClassTotals(eq(budgetId), ArgumentMatchers.any());
    assertEquals(expectedExpenseClassTotalsCollection, resultExpenseClassTotal);
  }

  @Test
  void getFinanceBudgetsExpenseClassesTotalsByIdWithError() {
    CompletableFuture<BudgetExpenseClassTotalsCollection> future = new CompletableFuture<>();
    future.completeExceptionally(new HttpException(400, GENERIC_ERROR_CODE));
    when(budgetExpenseClassTotalsMockService.getExpenseClassTotals(anyString(), ArgumentMatchers.any())).thenReturn(future);
    String budgetId = UUID.randomUUID().toString();

    verifyGet(String.format("/finance/budgets/%s/expense-classes-totals", budgetId), APPLICATION_JSON, 400);

    verify(budgetExpenseClassTotalsMockService).getExpenseClassTotals(eq(budgetId), ArgumentMatchers.any());
  }

  /**
   * Define unit test specific beans to override actual ones
   */
  @Configuration
  static class ContextConfiguration {

    @Bean("budgetExpenseClassTotalsMockService")
    @Primary
    public BudgetExpenseClassTotalsService budgetExpenseClassTotalsService() {
      return budgetExpenseClassTotalsMockService;
    }

    @Bean("budgetMockService")
    @Primary
    public BudgetService budgetService() {
      return budgetMockService;
    }
  }

}
