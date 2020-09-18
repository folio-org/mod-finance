package org.folio.services.budget;

import static org.folio.rest.util.ErrorCodes.ALLOCATION_TRANSFER_FAILED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.json.JsonObject;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.GroupFundFiscalYearService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.transactions.CommonTransactionService;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BudgetServiceTest {

  @InjectMocks
  private BudgetService budgetService;

  @Mock
  private RestClient budgetMockRestClient;

  @Mock
  private CommonTransactionService transactionMockService;

  @Mock
  private BudgetExpenseClassService budgetExpenseClassMockService;

  @Mock
  private GroupFundFiscalYearService groupFundFiscalYearService;

  @Mock
  private RequestContext requestContextMock;

  private SharedBudget sharedBudget;


  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString());;
  }

  @Test
  void testGetBudgetByIdWithExpenseClasses() {
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
    when(budgetMockRestClient.getById(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(budget));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(anyString(), any())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClasses));

    CompletableFuture<SharedBudget> resultFuture = budgetService.getBudgetById(budget.getId(), requestContextMock);

    SharedBudget resultBudget = resultFuture.join();

    StatusExpenseClass expectedStatusExpenseClass1 = new StatusExpenseClass()
      .withStatus(StatusExpenseClass.Status.ACTIVE)
      .withExpenseClassId(budgetExpenseClass1.getExpenseClassId());

    StatusExpenseClass expectedStatusExpenseClass2 = new StatusExpenseClass()
      .withStatus(StatusExpenseClass.Status.INACTIVE)
      .withExpenseClassId(budgetExpenseClass2.getExpenseClassId());

    assertThat(resultBudget.getStatusExpenseClasses(), hasSize(2));
    assertThat(resultBudget.getStatusExpenseClasses(), containsInAnyOrder(expectedStatusExpenseClass1, expectedStatusExpenseClass2));

    verify(budgetMockRestClient).getById(eq(budget.getId()), eq(requestContextMock), any());
    verify(budgetExpenseClassMockService).getBudgetExpenseClasses(eq(budget.getId()), eq(requestContextMock));
  }

  @Test
  void testGetBudgetByIdWithoutExpenseClasses() {
    Budget budget = new Budget().withId(UUID.randomUUID().toString());

    when(budgetMockRestClient.getById(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(budget));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(anyString(), any())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

    CompletableFuture<SharedBudget> resultFuture = budgetService.getBudgetById(budget.getId(), requestContextMock);

    SharedBudget resultBudget = resultFuture.join();

    assertThat(resultBudget.getStatusExpenseClasses(), hasSize(0));

    verify(budgetMockRestClient).getById(eq(budget.getId()), eq(requestContextMock), any());
    verify(budgetExpenseClassMockService).getBudgetExpenseClasses(eq(budget.getId()), eq(requestContextMock));
  }

  @Test
  void testUpdateBudgetWithExceededAllowableEncumbered() {
    sharedBudget
      .withAllowableExpenditure(110d)
      .withAllowableEncumbrance(1d);

    Budget budgetFromStorage = new Budget()
      .withAllocated(25000d)
      .withAvailable(15313.45)
      .withUnavailable(9686.55)
      .withAwaitingPayment(150.60)
      .withEncumbered(7307.4)
      .withExpenditures(2228.55);

    when(budgetMockRestClient.getById(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetFromStorage));

    CompletableFuture<Void> resultFuture = budgetService.updateBudget(sharedBudget, requestContextMock);
    ExecutionException exception = assertThrows(ExecutionException.class, resultFuture::get);

    assertThat(exception.getCause(), IsInstanceOf.instanceOf(HttpException.class));
    HttpException cause = (HttpException) exception.getCause();
    Errors errors = new Errors().withErrors(Collections.singletonList(ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED.toError())).withTotalRecords(1);
    assertEquals(422, cause.getCode());
    assertEquals(errors, cause.getErrors());

    verify(budgetMockRestClient).getById(eq(sharedBudget.getId()), eq(requestContextMock), eq(Budget.class));
    verify(budgetMockRestClient, never()).put(anyString(), any(), any());
  }

  @Test
  void testUpdateBudgetWithoutStatusExpenseClassesTotalValuesShouldBeIgnored() {
    Budget budgetFromStorage = new Budget();
    budgetFromStorage.setAllocated(200.12);
    budgetFromStorage.setAvailable(101.12);
    budgetFromStorage.setUnavailable(100d);
    budgetFromStorage.setExpenditures(55d);
    budgetFromStorage.setAwaitingPayment(12.5);
    budgetFromStorage.setEncumbered(42.5);
    budgetFromStorage.setOverEncumbrance(5d);
    budgetFromStorage.setOverExpended(5d);
    budgetFromStorage.setNetTransfers(1d);

    when(budgetMockRestClient.put(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetExpenseClassMockService.updateBudgetExpenseClassesLinks(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetMockRestClient.getById(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetFromStorage));

    CompletableFuture<Void> resultFuture = budgetService.updateBudget(sharedBudget, requestContextMock);
    resultFuture.join();
    assertFalse(resultFuture.isCompletedExceptionally());

    JsonObject json = JsonObject.mapFrom(sharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    verify(budgetExpenseClassMockService).updateBudgetExpenseClassesLinks(eq(sharedBudget), eq(requestContextMock));
    verify(budgetMockRestClient).put(eq(sharedBudget.getId()), eq(expectedBudget), eq(requestContextMock));

    assertEquals(budgetFromStorage.getAllocated(), sharedBudget.getAllocated());
    assertEquals(budgetFromStorage.getAvailable(), sharedBudget.getAvailable());
    assertEquals(budgetFromStorage.getUnavailable(), sharedBudget.getUnavailable());
    assertEquals(budgetFromStorage.getEncumbered(), sharedBudget.getEncumbered());
    assertEquals(budgetFromStorage.getExpenditures(), sharedBudget.getExpenditures());
    assertEquals(budgetFromStorage.getAwaitingPayment(), sharedBudget.getAwaitingPayment());
    assertEquals(budgetFromStorage.getNetTransfers(), sharedBudget.getNetTransfers());
    assertEquals(budgetFromStorage.getOverExpended(), sharedBudget.getOverExpended());
    assertEquals(budgetFromStorage.getOverEncumbrance(), sharedBudget.getOverEncumbrance());
  }

  @Test
  void testUpdateBudgetWithExceededAllowableExpenditure() {
    sharedBudget
      .withAllowableEncumbrance(110d)
      .withAllowableExpenditure(1d);

    Budget budgetFromStorage = new Budget()
      .withAllocated(25000d)
      .withAvailable(15313.45)
      .withUnavailable(9686.55)
      .withAwaitingPayment(150.60)
      .withEncumbered(7307.4)
      .withExpenditures(2228.55);

    when(budgetMockRestClient.getById(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetFromStorage));

    CompletableFuture<Void> resultFuture = budgetService.updateBudget(sharedBudget, requestContextMock);
    ExecutionException exception = assertThrows(ExecutionException.class, resultFuture::get);

    assertThat(exception.getCause(), IsInstanceOf.instanceOf(HttpException.class));
    HttpException cause = (HttpException) exception.getCause();
    Errors errors = new Errors().withErrors(Collections.singletonList(ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED.toError())).withTotalRecords(1);
    assertEquals(422, cause.getCode());
    assertEquals(errors, cause.getErrors());

    verify(budgetMockRestClient).getById(eq(sharedBudget.getId()), eq(requestContextMock), eq(Budget.class));
    verify(budgetMockRestClient, never()).put(anyString(), any(), any());
  }

  @Test
  void testCreateBudgetWithAllocated() {

    sharedBudget.withAllocated(100.43);

    Budget budgetFromStorage = new Budget().withId(sharedBudget.getId())
      .withAllocated(0d)
      .withFiscalYearId(sharedBudget.getFiscalYearId())
      .withFundId(sharedBudget.getFundId());
    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetFromStorage));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(CompletableFuture.completedFuture(new Transaction()));
    when(groupFundFiscalYearService.updateBudgetIdForGroupFundFiscalYears(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    CompletableFuture<SharedBudget> resultFuture = budgetService.createBudget(sharedBudget, requestContextMock);
    SharedBudget resultBudget = resultFuture.join();
    assertEquals(100.43, resultBudget.getAllocated());
    assertEquals(0, sharedBudget.getAllocated());
    assertEquals(budgetFromStorage, resultBudget);

    JsonObject json = JsonObject.mapFrom(sharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
    verify(transactionMockService).createAllocationTransaction(eq(budgetFromStorage), eq(requestContextMock));
    verify(groupFundFiscalYearService).updateBudgetIdForGroupFundFiscalYears(eq(budgetFromStorage), eq(requestContextMock));
    verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(resultBudget), eq(requestContextMock));
  }

  @Test
  void testCreateBudgetWithZeroAllocated() {

    sharedBudget.withAllocated(0d);
    when(groupFundFiscalYearService.updateBudgetIdForGroupFundFiscalYears(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(sharedBudget));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    CompletableFuture<SharedBudget> resultFuture = budgetService.createBudget(sharedBudget, requestContextMock);
    Budget resultBudget = resultFuture.join();

    JsonObject json = JsonObject.mapFrom(sharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    assertEquals(sharedBudget, resultBudget);
    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
    verify(transactionMockService, never()).createAllocationTransaction(eq(sharedBudget), eq(requestContextMock));
    verify(groupFundFiscalYearService).updateBudgetIdForGroupFundFiscalYears(eq(sharedBudget), eq(requestContextMock));
    verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(sharedBudget), eq(requestContextMock));
  }

  @Test
  void testCreateBudgetWithAllocationCreationError() {

    sharedBudget.withAllocated(100.43);
    Budget budgetFromStorage = new Budget().withId(sharedBudget.getId())
      .withAllocated(0d)
      .withFiscalYearId(sharedBudget.getFiscalYearId())
      .withFundId(sharedBudget.getFundId());

    CompletableFuture<Transaction> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new Exception());

    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetFromStorage));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(errorFuture);

    CompletableFuture<SharedBudget> resultFuture = budgetService.createBudget(sharedBudget, requestContextMock);
    ExecutionException executionException = assertThrows(ExecutionException.class, resultFuture::get);

    assertThat(executionException.getCause(), IsInstanceOf.instanceOf(HttpException.class));

    HttpException httpException = (HttpException) executionException.getCause();
    assertEquals(500, httpException.getCode());
    assertEquals(ALLOCATION_TRANSFER_FAILED.toError(), httpException.getErrors().getErrors().get(0));
    assertEquals(100.43, budgetFromStorage.getAllocated());
    assertEquals(0, sharedBudget.getAllocated());

    JsonObject json = JsonObject.mapFrom(sharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
    verify(transactionMockService).createAllocationTransaction(eq(budgetFromStorage), eq(requestContextMock));
    verify(budgetExpenseClassMockService, never()).createBudgetExpenseClasses(any(), any());
  }

  @Test
  void testGetBudgets() {
    Budget budget = new Budget().withId(UUID.randomUUID().toString());

    BudgetsCollection budgetCollection = new BudgetsCollection()
      .withBudgets(Collections.singletonList(budget))
      .withTotalRecords(1);
    when(budgetMockRestClient.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetCollection));
    String query = "id=" + budget.getId();
    int offset = 2;
    int limit = 100;
    CompletableFuture<BudgetsCollection> resultFuture = budgetService.getBudgets(query, offset, limit, requestContextMock);

    BudgetsCollection resultBudgetsCollection = resultFuture.join();

    assertEquals(budgetCollection, resultBudgetsCollection);
    verify(budgetMockRestClient).get(eq(query), eq(offset), eq(limit), eq(requestContextMock), any());

  }

  @Test
  void testDeleteBudget() {
    String id = UUID.randomUUID().toString();
    when(budgetMockRestClient.delete(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
    CompletableFuture<Void> futureResult = budgetService.deleteBudget(id, requestContextMock);
    futureResult.join();
    assertTrue(futureResult.isDone());

    verify(budgetMockRestClient).delete(eq(id), eq(requestContextMock));
  }

}
