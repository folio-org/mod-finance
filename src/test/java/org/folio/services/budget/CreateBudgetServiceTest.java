package org.folio.services.budget;

import static org.folio.rest.util.ErrorCodes.ALLOCATION_TRANSFER_FAILED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.ExpenseClassConverterUtils;
import org.folio.services.GroupFundFiscalYearService;
import org.folio.services.fund.FundDetailsService;
import org.folio.services.fund.FundFiscalYearService;
import org.folio.services.transactions.CommonTransactionService;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.json.JsonObject;

public class CreateBudgetServiceTest {
  @InjectMocks
  private CreateBudgetService createBudgetService;

  @Mock
  private RestClient budgetMockRestClient;
//  @Mock
//  private RestClient budgetExpenseClassRestClient;
  @Mock
  private CommonTransactionService transactionMockService;
  @Mock
  private BudgetExpenseClassService budgetExpenseClassMockService;
  @Mock
  private GroupFundFiscalYearService groupFundFiscalYearMockService;
  @Mock
  private FundFiscalYearService fundFiscalYearMockService;
  @Mock
  private FundDetailsService fundDetailsMockService;


  @Mock
  private RequestContext requestContextMock;

  private SharedBudget currSharedBudget;
  private Budget currBudget;
  private SharedBudget plannedSharedBudget;
  private FiscalYear plannedFiscalYear;
  private List<BudgetExpenseClass> currBudgetExpenseClasses;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    String currBudgetId = UUID.randomUUID().toString();
    String currFiscalYearId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    BudgetExpenseClass expenseClass = new BudgetExpenseClass().withId(UUID.randomUUID().toString())
                                                              .withBudgetId(currBudgetId)
                                                              .withExpenseClassId(UUID.randomUUID().toString());
    List<StatusExpenseClass> statusExpenseClasses = Collections.singletonList(ExpenseClassConverterUtils.buildStatusExpenseClass(expenseClass));

    currBudgetExpenseClasses = Collections.singletonList(expenseClass);
    currBudget = new Budget().withId(currBudgetId).withFiscalYearId(currFiscalYearId).withFundId(fundId);
    currSharedBudget = new SharedBudget().withId(currBudgetId).withFiscalYearId(currFiscalYearId).withFundId(fundId);
    plannedFiscalYear = new FiscalYear().withId(UUID.randomUUID().toString()).withCode("FY2021").withSeries("FY");
    plannedSharedBudget = new SharedBudget().withId(UUID.randomUUID().toString()).withFiscalYearId(plannedFiscalYear.getId()).withFundId(fundId);
  }

  @Test
 // @Disabled
  void testCurrentCreateBudgetWithAllocated() throws ExecutionException, InterruptedException {

    currSharedBudget.withAllocated(100.43);

    Budget budgetFromStorage = new Budget().withId(currSharedBudget.getId())
      .withAllocated(0d)
      .withFiscalYearId(currSharedBudget.getFiscalYearId())
      .withFundId(currSharedBudget.getFundId());
    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any())).thenReturn(CompletableFuture.completedFuture(plannedFiscalYear));
    when(groupFundFiscalYearMockService.updateBudgetIdForGroupFundFiscalYears(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currSharedBudget));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(CompletableFuture.completedFuture(new Transaction()));

    CompletableFuture<SharedBudget> resultFuture = createBudgetService.createBudget(currSharedBudget, requestContextMock);
    SharedBudget resultBudget = resultFuture.get();
    assertEquals(100.43, resultBudget.getAllocated());
  //  assertEquals(0, currSharedBudget.getAllocated());
   // assertEquals(plannedSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
    assertEquals(plannedSharedBudget.getFundId(), resultBudget.getFundId());

    JsonObject json = JsonObject.mapFrom(currSharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

//    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
//    verify(transactionMockService).createAllocationTransaction(eq(budgetFromStorage), eq(requestContextMock));
//    verify(groupFundFiscalYearMockService).updateBudgetIdForGroupFundFiscalYears(eq(budgetFromStorage), eq(requestContextMock));
//    verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(resultBudget), eq(requestContextMock));
  }

  @Test
 // @Disabled
  void testPlannedBudgetWithAllocated() {

    plannedSharedBudget.withAllocated(100.43);

    Budget budgetFromStorage = new Budget().withId(plannedSharedBudget.getId())
      .withAllocated(0d)
      .withFiscalYearId(plannedSharedBudget.getFiscalYearId())
      .withFundId(plannedSharedBudget.getFundId());
    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any())).thenReturn(CompletableFuture.completedFuture(plannedFiscalYear));
    when(groupFundFiscalYearMockService.updateBudgetIdForGroupFundFiscalYears(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currSharedBudget));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(currBudget.getId(), requestContextMock)).thenReturn(CompletableFuture.completedFuture(currBudgetExpenseClasses));

    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(CompletableFuture.completedFuture(new Transaction()));
    when(fundDetailsMockService.retrieveCurrentBudget(plannedSharedBudget.getFundId(), null, requestContextMock)).thenReturn(CompletableFuture.completedFuture(currBudget));

    CompletableFuture<SharedBudget> resultFuture = createBudgetService.createBudget(plannedSharedBudget, requestContextMock);
    SharedBudget resultBudget = resultFuture.join();
    assertEquals(100.43, resultBudget.getAllocated());
    assertEquals(0, plannedSharedBudget.getAllocated());
   // assertEquals(plannedSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
    assertEquals(plannedSharedBudget.getFundId(), resultBudget.getFundId());

    JsonObject json = JsonObject.mapFrom(currSharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

  //  verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
//    verify(transactionMockService).createAllocationTransaction(eq(budgetFromStorage), eq(requestContextMock));
//    verify(groupFundFiscalYearMockService).updateBudgetIdForGroupFundFiscalYears(eq(budgetFromStorage), eq(requestContextMock));
 //   verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(resultBudget), eq(requestContextMock));
  }

  @Test
  void testCreateBudgetWithZeroAllocated() {
    currSharedBudget.withAllocated(0d);
    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any())).thenReturn(CompletableFuture.completedFuture(plannedFiscalYear));
    when(groupFundFiscalYearMockService.updateBudgetIdForGroupFundFiscalYears(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currSharedBudget));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    CompletableFuture<SharedBudget> resultFuture = createBudgetService.createBudget(currSharedBudget, requestContextMock);
    Budget resultBudget = resultFuture.join();

    JsonObject json = JsonObject.mapFrom(currSharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    assertEquals(currSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
    assertEquals(currSharedBudget.getFundId(), resultBudget.getFundId());
    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
    verify(transactionMockService, never()).createAllocationTransaction(eq(currSharedBudget), eq(requestContextMock));
    verify(groupFundFiscalYearMockService).updateBudgetIdForGroupFundFiscalYears(eq(currSharedBudget), eq(requestContextMock));
//    verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(currSharedBudget), eq(requestContextMock));
  }

  @Test
 // @Disabled
  void testCreateBudgetWithAllocationCreationError() {

    currSharedBudget.withAllocated(100.43);
    Budget budgetFromStorage = new Budget().withId(currSharedBudget.getId())
      .withAllocated(0d)
      .withFiscalYearId(currSharedBudget.getFiscalYearId())
      .withFundId(currSharedBudget.getFundId());

    CompletableFuture<Transaction> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new Exception());

    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetFromStorage));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(errorFuture);
    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any())).thenReturn(CompletableFuture.completedFuture(plannedFiscalYear));
    when(fundDetailsMockService.retrieveCurrentBudget(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currBudget));

    CompletableFuture<SharedBudget> resultFuture = createBudgetService.createBudget(currSharedBudget, requestContextMock);
    ExecutionException executionException = assertThrows(ExecutionException.class, resultFuture::get);

    assertThat(executionException.getCause(), IsInstanceOf.instanceOf(HttpException.class));

    HttpException httpException = (HttpException) executionException.getCause();
    assertEquals(500, httpException.getCode());
    assertEquals(ALLOCATION_TRANSFER_FAILED.toError(), httpException.getErrors().getErrors().get(0));
    assertEquals(100.43, budgetFromStorage.getAllocated());
    assertEquals(0, currSharedBudget.getAllocated());

    JsonObject json = JsonObject.mapFrom(currSharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
    verify(transactionMockService).createAllocationTransaction(eq(budgetFromStorage), eq(requestContextMock));
    verify(budgetExpenseClassMockService, never()).createBudgetExpenseClasses(any(), any());
  }

}
