package org.folio.services.budget;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.vertx.core.AsyncResult;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.util.ErrorCodes;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class BudgetServiceTest {

  @InjectMocks
  private BudgetService budgetService;

  @Mock
  private RestClient restClient;

  @Mock
  private BudgetExpenseClassService budgetExpenseClassMockService;

  @Mock
  private RequestContext requestContextMock;

  private SharedBudget sharedBudget;


  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString());
  }

  @Test
  void testGetBudgetByIdWithExpenseClasses(VertxTestContext vertxTestContext) {
    Budget budget = new Budget().withId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withBudgetId(budget.getId())
      .withExpenseClassId(UUID.randomUUID().toString())
      .withStatus(BudgetExpenseClass.Status.ACTIVE)
      .withId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withBudgetId(budget.getId())
      .withExpenseClassId(UUID.randomUUID().toString())
      .withStatus(BudgetExpenseClass.Status.INACTIVE)
      .withId(UUID.randomUUID().toString());
    List<BudgetExpenseClass> budgetExpenseClasses = Arrays.asList(budgetExpenseClass1, budgetExpenseClass2);
    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(budget));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(anyString(), any())).thenReturn(succeededFuture(budgetExpenseClasses));

    Future<SharedBudget> future = budgetService.getBudgetById(budget.getId(), requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        StatusExpenseClass expectedStatusExpenseClass1 = new StatusExpenseClass()
          .withStatus(StatusExpenseClass.Status.ACTIVE)
          .withExpenseClassId(budgetExpenseClass1.getExpenseClassId());

        StatusExpenseClass expectedStatusExpenseClass2 = new StatusExpenseClass()
          .withStatus(StatusExpenseClass.Status.INACTIVE)
          .withExpenseClassId(budgetExpenseClass2.getExpenseClassId());

        assertThat(result.result().getStatusExpenseClasses(), hasSize(2));
        assertThat(result.result().getStatusExpenseClasses(), containsInAnyOrder(expectedStatusExpenseClass1, expectedStatusExpenseClass2));

        verify(restClient).get(eq(resourceByIdPath(BUDGETS_STORAGE,budget.getId())), any(), eq(requestContextMock));
        verify(budgetExpenseClassMockService).getBudgetExpenseClasses(eq(budget.getId()), eq(requestContextMock));
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testGetBudgetByIdWithoutExpenseClasses(VertxTestContext vertxTestContext) {
    Budget budget = new Budget().withId(UUID.randomUUID().toString());

    when(restClient.get(eq(resourceByIdPath(BUDGETS_STORAGE, budget.getId())), any(), any()))
      .thenReturn(succeededFuture(budget));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(anyString(), any())).thenReturn(succeededFuture(Collections.emptyList()));

    Future<SharedBudget> future = budgetService.getBudgetById(budget.getId(), requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertThat(result.result().getStatusExpenseClasses(), hasSize(0));
        verify(restClient).get(eq(resourceByIdPath(BUDGETS_STORAGE, budget.getId())), any(), eq(requestContextMock));
        verify(budgetExpenseClassMockService).getBudgetExpenseClasses(eq(budget.getId()), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testUpdateBudgetWithExceededAllowableEncumbered(VertxTestContext vertxTestContext) {
    sharedBudget
      .withAllowableExpenditure(110d)
      .withAllowableEncumbrance(1d);

    Budget budgetFromStorage = new Budget()
      .withAllocated(25000d)
      .withAvailable(15313.45)
      .withUnavailable(9686.55)
      .withInitialAllocation(10000d)
      .withAllocationTo(1000d)
      .withAllocationFrom(1000d)
      .withAwaitingPayment(150.60)
      .withEncumbered(7307.4)
      .withExpenditures(2228.55);

    when(restClient.get(anyString(), eq(Budget.class), any()))
      .thenReturn(succeededFuture(budgetFromStorage));

    Future<Void> future = budgetService.updateBudget(sharedBudget, requestContextMock);

    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        verifyLimitExceeded(result, ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED);
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testUpdateBudgetWithExceededAllowableEncumberedSimplifiedCase(VertxTestContext vertxTestContext) {
    sharedBudget
      .withAllowableEncumbrance(90d);

    Budget budgetFromStorage = new Budget()
      .withAllocated(10d)
      .withEncumbered(10d);

    when(restClient.get(anyString(), eq(Budget.class), any()))
      .thenReturn(succeededFuture(budgetFromStorage));

    Future<Void> future = budgetService.updateBudget(sharedBudget, requestContextMock);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        verifyLimitExceeded(result, ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED);
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testUpdateBudgetWithoutStatusExpenseClassesTotalValuesShouldBeIgnored(VertxTestContext vertxTestContext) {
    Budget budgetFromStorage = new Budget();
    budgetFromStorage.setAllocated(200.12);
    budgetFromStorage.setAvailable(101.12);
    budgetFromStorage.setUnavailable(100d);
    budgetFromStorage.setExpenditures(65d);
    budgetFromStorage.setCredits(10d);
    budgetFromStorage.setAwaitingPayment(12.5);
    budgetFromStorage.setEncumbered(42.5);
    budgetFromStorage.setOverEncumbrance(5d);
    budgetFromStorage.setOverExpended(5d);
    budgetFromStorage.setNetTransfers(1d);

    when(restClient.get(any(), eq(Budget.class), any(RequestContext.class))).thenReturn(succeededFuture(budgetFromStorage));
    when(restClient.put(anyString(), any(Budget.class), any())).thenReturn(succeededFuture(null));
    when(budgetExpenseClassMockService.updateBudgetExpenseClassesLinks(any(), any())).thenReturn(succeededFuture(null));

    Future<Void> future = budgetService.updateBudget(sharedBudget, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        JsonObject json = JsonObject.mapFrom(sharedBudget);
        json.remove("statusExpenseClasses");
        Budget expectedBudget = json.mapTo(Budget.class);

        verify(budgetExpenseClassMockService).updateBudgetExpenseClassesLinks(eq(sharedBudget), eq(requestContextMock));
        verify(restClient).put(assertQueryContains(sharedBudget.getId()), eq(expectedBudget), eq(requestContextMock));

        assertEquals(budgetFromStorage.getAllocated(), sharedBudget.getAllocated());
        assertEquals(budgetFromStorage.getAvailable(), sharedBudget.getAvailable());
        assertEquals(budgetFromStorage.getUnavailable(), sharedBudget.getUnavailable());
        assertEquals(budgetFromStorage.getEncumbered(), sharedBudget.getEncumbered());
        assertEquals(budgetFromStorage.getExpenditures(), sharedBudget.getExpenditures());
        assertEquals(budgetFromStorage.getCredits(), sharedBudget.getCredits());
        assertEquals(budgetFromStorage.getAwaitingPayment(), sharedBudget.getAwaitingPayment());
        assertEquals(budgetFromStorage.getNetTransfers(), sharedBudget.getNetTransfers());
        assertEquals(budgetFromStorage.getOverExpended(), sharedBudget.getOverExpended());
        assertEquals(budgetFromStorage.getOverEncumbrance(), sharedBudget.getOverEncumbrance());
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testUpdateBudgetWithExceededAllowableExpenditure(VertxTestContext vertxTestContext) {
    sharedBudget
      .withAllowableEncumbrance(110d)
      .withAllowableExpenditure(1d);

    Budget budgetFromStorage = new Budget()
      .withAllocated(25000d)
      .withAvailable(15313.45)
      .withUnavailable(9686.55)
      .withAwaitingPayment(150.60)
      .withEncumbered(7307.4)
      .withExpenditures(2228.55)
      .withCredits(80.05);

    when(restClient.get(anyString(), eq(Budget.class), any())).thenReturn(succeededFuture(budgetFromStorage));

    Future<Void> future = budgetService.updateBudget(sharedBudget, requestContextMock);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        verifyLimitExceeded(result, ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED);
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testUpdateBudgetWithExceededAllowableExpenditureSimplifiedCase(VertxTestContext vertxTestContext) {
    sharedBudget
      .withAllowableExpenditure(90d);

    Budget budgetFromStorage = new Budget()
      .withAllocated(10d)
      .withAwaitingPayment(15d);

    when(restClient.get(anyString(), eq(Budget.class), any())).thenReturn(succeededFuture(budgetFromStorage));

    Future<Void> future = budgetService.updateBudget(sharedBudget, requestContextMock);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        verifyLimitExceeded(result, ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED);
        vertxTestContext.completeNow();
      });

  }


  @Test
  void testGetBudgets(VertxTestContext vertxTestContext) {
    Budget budget = new Budget().withId(UUID.randomUUID().toString());

    BudgetsCollection budgetCollection = new BudgetsCollection()
      .withBudgets(Collections.singletonList(budget))
      .withTotalRecords(1);
    when(restClient.get(anyString(), any(), any()))
      .thenReturn(succeededFuture(budgetCollection));
    String query = "id=" + budget.getId();
    int offset = 2;
    int limit = 100;
    Future<BudgetsCollection> future = budgetService.getBudgets(query, offset, limit, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertEquals(budgetCollection, result.result());
        verify(restClient).get(assertQueryContains(budget.getId()), any(), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testDeleteBudget(VertxTestContext vertxTestContext) {
    String id = UUID.randomUUID().toString();
    when(restClient.delete(anyString(), any())).thenReturn(succeededFuture(null));
    Future<Void> future = budgetService.deleteBudget(id, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(future.succeeded());
        verify(restClient).delete(contains(id), eq(requestContextMock));
        vertxTestContext.completeNow();
      });

  }

  private void verifyLimitExceeded(AsyncResult<Void> result, ErrorCodes errorCode) {
    assertThat(result.cause(), IsInstanceOf.instanceOf(HttpException.class));
    HttpException cause = (HttpException) result.cause();
    Errors errors = new Errors().withErrors(Collections.singletonList(errorCode.toError())).withTotalRecords(1);
    assertEquals(422, cause.getCode());
    assertEquals(errors, cause.getErrors());

    verify(restClient).get(anyString(), eq(Budget.class), eq(requestContextMock));
    verify(restClient, never()).put(anyString(), any(), any());
  }
}
