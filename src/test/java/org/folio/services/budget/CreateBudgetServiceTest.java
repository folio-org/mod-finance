package org.folio.services.budget;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import io.vertx.core.json.JsonObject;
import static org.folio.rest.util.ErrorCodes.ALLOCATION_TRANSFER_FAILED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  private GroupFundFiscalYearService groupFundFiscalYearService;
  @Mock
  private FundFiscalYearService fundFiscalYearService;
  @Mock
  private FundDetailsService fundDetailsService;
  @Mock
  private BudgetExpenseClassService budgetExpenseClassService;

  @Mock
  private RequestContext requestContextMock;

  private SharedBudget currSharedBudget;
  private Budget currBudget;
  private SharedBudget plannedSharedBudget;
  private FiscalYear plannedFiscalYear;
  private List<BudgetExpenseClass> currExpenseClasses;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    String currBudgetId = UUID.randomUUID().toString();
    String currFiscalYearId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    BudgetExpenseClass expenseClass = new BudgetExpenseClass().withId(UUID.randomUUID().toString())
                                                              .withBudgetId(currBudgetId)
                                                              .withExpenseClassId(UUID.randomUUID().toString());
    currExpenseClasses = Collections.singletonList(expenseClass);
    currBudget = new Budget().withId(currBudgetId).withFiscalYearId(currFiscalYearId).withFundId(fundId);
    currSharedBudget = new SharedBudget().withId(currBudgetId).withFiscalYearId(currFiscalYearId).withFundId(fundId);
    plannedFiscalYear = new FiscalYear().withId(UUID.randomUUID().toString()).withCode("FY2021").withSeries("FY");
    plannedSharedBudget = new SharedBudget().withId(UUID.randomUUID().toString()).withFiscalYearId(plannedFiscalYear.getId()).withFundId(fundId);
  }

  @Test
  void testCurrentCreateBudgetWithAllocated() throws ExecutionException, InterruptedException {

    currSharedBudget.withAllocated(100.43);

    Budget budgetFromStorage = new Budget().withId(currSharedBudget.getId())
      .withAllocated(0d)
      .withFiscalYearId(currSharedBudget.getFiscalYearId())
      .withFundId(currSharedBudget.getFundId());
      when(fundFiscalYearService.retrievePlannedFiscalYear(any(), any())).thenReturn(CompletableFuture.completedFuture(plannedFiscalYear));
      when(budgetMockRestClient.post(eq(currSharedBudget), any(RequestContext.class), eq(Budget.class))).thenReturn(CompletableFuture.completedFuture(budgetFromStorage));
      when(transactionMockService.createAllocationTransaction(any(Budget.class), any(RequestContext.class))).thenReturn(CompletableFuture.completedFuture(new Transaction()));
//    when(groupFundFiscalYearService.updateBudgetIdForGroupFundFiscalYears(any(), any(RequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
//    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
//    when(fundDetailsService.retrieveCurrentBudget(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currBudget));

    CompletableFuture<SharedBudget> resultFuture = createBudgetService.createBudget(currSharedBudget, requestContextMock);
    SharedBudget resultBudget = resultFuture.get();
    assertEquals(100.43, resultBudget.getAllocated());
    assertEquals(0, currSharedBudget.getAllocated());
    assertEquals(budgetFromStorage, resultBudget);

    JsonObject json = JsonObject.mapFrom(currSharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
    verify(transactionMockService).createAllocationTransaction(eq(budgetFromStorage), eq(requestContextMock));
    verify(groupFundFiscalYearService).updateBudgetIdForGroupFundFiscalYears(eq(budgetFromStorage), eq(requestContextMock));
    verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(resultBudget), eq(requestContextMock));
  }

  @Test
  void testPlannedBudgetWithAllocated() {

    plannedSharedBudget.withAllocated(100.43);

    Budget budgetFromStorage = new Budget().withId(plannedSharedBudget.getId())
      .withAllocated(0d)
      .withFiscalYearId(plannedSharedBudget.getFiscalYearId())
      .withFundId(plannedSharedBudget.getFundId());
    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetFromStorage));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(CompletableFuture.completedFuture(new Transaction()));
    when(groupFundFiscalYearService.updateBudgetIdForGroupFundFiscalYears(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
   // when(budgetExpenseClassRestClient.post(any(), any(), eq(BudgetExpenseClass.class))).thenReturn(CompletableFuture.completedFuture(null));
    when(fundFiscalYearService.retrievePlannedFiscalYear(any(), any())).thenReturn(CompletableFuture.completedFuture(plannedFiscalYear));
    when(fundDetailsService.retrieveCurrentBudget(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currBudget));
    when(budgetExpenseClassService.getBudgetExpenseClasses(any(), any())).thenReturn(CompletableFuture.completedFuture(currExpenseClasses));

    CompletableFuture<SharedBudget> resultFuture = createBudgetService.createBudget(plannedSharedBudget, requestContextMock);
    SharedBudget resultBudget = resultFuture.join();
    assertEquals(100.43, resultBudget.getAllocated());
    assertEquals(0, plannedSharedBudget.getAllocated());
    assertEquals(budgetFromStorage, resultBudget);

    JsonObject json = JsonObject.mapFrom(currSharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
    verify(transactionMockService).createAllocationTransaction(eq(budgetFromStorage), eq(requestContextMock));
    verify(groupFundFiscalYearService).updateBudgetIdForGroupFundFiscalYears(eq(budgetFromStorage), eq(requestContextMock));
    verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(resultBudget), eq(requestContextMock));
  }

  @Test
  void testCreateBudgetWithZeroAllocated() {

    currSharedBudget.withAllocated(0d);
    when(groupFundFiscalYearService.updateBudgetIdForGroupFundFiscalYears(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetMockRestClient.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currSharedBudget));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(fundFiscalYearService.retrievePlannedFiscalYear(any(), any())).thenReturn(CompletableFuture.completedFuture(plannedFiscalYear));
    when(fundDetailsService.retrieveCurrentBudget(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currBudget));
    CompletableFuture<SharedBudget> resultFuture = createBudgetService.createBudget(currSharedBudget, requestContextMock);
    Budget resultBudget = resultFuture.join();

    JsonObject json = JsonObject.mapFrom(currSharedBudget);
    json.remove("statusExpenseClasses");
    Budget expectedBudget = json.mapTo(Budget.class);

    assertEquals(currSharedBudget, resultBudget);
    verify(budgetMockRestClient).post(eq(expectedBudget), eq(requestContextMock), eq(Budget.class));
    verify(transactionMockService, never()).createAllocationTransaction(eq(currSharedBudget), eq(requestContextMock));
    verify(groupFundFiscalYearService).updateBudgetIdForGroupFundFiscalYears(eq(currSharedBudget), eq(requestContextMock));
    verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(currSharedBudget), eq(requestContextMock));
  }

  @Test
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
    when(fundFiscalYearService.retrievePlannedFiscalYear(any(), any())).thenReturn(CompletableFuture.completedFuture(plannedFiscalYear));
    when(fundDetailsService.retrieveCurrentBudget(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(currBudget));

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
