package org.folio.services.budget;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.ALLOCATION_TRANSFER_FAILED;
import static org.folio.rest.util.ErrorCodes.NEGATIVE_ALLOCATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.services.fund.FundDetailsService;
import org.folio.services.fund.FundFiscalYearService;
import org.folio.services.transactions.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class CreateBudgetServiceTest {
  @InjectMocks
  private CreateBudgetService createBudgetService;

  @Mock
  private RestClient restClient;
  @Mock
  private TransactionService transactionMockService;
  @Mock
  private BudgetExpenseClassService budgetExpenseClassMockService;
  @Mock
  private FundFiscalYearService fundFiscalYearMockService;
  @Mock
  private FundDetailsService fundDetailsMockService;


  @Mock
  private RequestContext requestContextMock;

  private AutoCloseable mockitoMocks;
  private SharedBudget currSharedBudget;
  private Budget currBudget, budgetAfterCreation, budgetWithAllocation;
  private SharedBudget plannedSharedBudget;
  private FiscalYear plannedFiscalYear;
  private List<BudgetExpenseClass> currBudgetExpenseClasses;
  private StatusExpenseClass statusExpenseClass;

  @BeforeEach
  public void initMocks() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
    String currBudgetId = UUID.randomUUID().toString();
    String currFiscalYearId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    BudgetExpenseClass expenseClass = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withBudgetId(currBudgetId)
      .withExpenseClassId(UUID.randomUUID().toString());
    statusExpenseClass = new StatusExpenseClass()
      .withStatus(StatusExpenseClass.Status.ACTIVE)
      .withExpenseClassId(UUID.randomUUID().toString());
    currBudgetExpenseClasses = Collections.singletonList(expenseClass);
    currBudget = new Budget()
      .withId(currBudgetId)
      .withBudgetStatus(Budget.BudgetStatus.ACTIVE)
      .withFiscalYearId(currFiscalYearId)
      .withFundId(fundId);
    budgetAfterCreation = new Budget()
      .withId(currBudgetId)
      .withBudgetStatus(Budget.BudgetStatus.ACTIVE)
      .withAllocated(0d)
      .withFiscalYearId(currFiscalYearId)
      .withFundId(fundId);
    budgetWithAllocation = new Budget()
      .withId(currBudgetId)
      .withBudgetStatus(Budget.BudgetStatus.ACTIVE)
      .withFiscalYearId(currFiscalYearId)
      .withFundId(fundId)
      .withInitialAllocation(100.43)
      .withAllocated(100.43);
    currSharedBudget = new SharedBudget()
      .withId(currBudgetId)
      .withBudgetStatus(SharedBudget.BudgetStatus.ACTIVE)
      .withFiscalYearId(currFiscalYearId)
      .withFundId(fundId);
    plannedFiscalYear = new FiscalYear()
      .withId(UUID.randomUUID().toString())
      .withCode("FY2021")
      .withSeries("FY");
    plannedSharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withBudgetStatus(SharedBudget.BudgetStatus.ACTIVE)
      .withFiscalYearId(plannedFiscalYear.getId())
      .withFundId(fundId);
  }

  @AfterEach
  public void afterEach() throws Exception {
    mockitoMocks.close();
  }

  @Test
  void testCurrentCreateBudgetWithAllocated(VertxTestContext vertxTestContext) {
    currSharedBudget.withAllocated(100.43);

    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any()))
      .thenReturn(succeededFuture(plannedFiscalYear));
    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetAfterCreation));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any()))
      .thenReturn(succeededFuture(null));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), any()))
      .thenReturn(succeededFuture(null));

    var future = createBudgetService.createBudget(currSharedBudget, requestContextMock);
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());

          var resultBudget = result.result();
          assertEquals(100.43, resultBudget.getAllocated());
          assertEquals(currSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
          assertEquals(currSharedBudget.getFundId(), resultBudget.getFundId());

          JsonObject json = JsonObject.mapFrom(currSharedBudget);
          json.remove("statusExpenseClasses");
          Budget expectedBudget = json.mapTo(Budget.class);

          verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));
          verify(restClient).post(anyString(), eq(expectedBudget), eq(Budget.class), eq(requestContextMock));
          verify(restClient).get(anyString(), eq(Budget.class), eq(requestContextMock));
          verify(transactionMockService).createAllocationTransaction(eq(budgetAfterCreation), eq(requestContextMock));
          verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(currSharedBudget), eq(requestContextMock));
          vertxTestContext.completeNow();
        });

  }

  @Test
  void testShouldCreatePlannedBudgetWithoutCurrExpenseClassesAndAllocatedIfCurrentBudgetIsAbsent(VertxTestContext vertxTestContext) {
    plannedSharedBudget.withAllocated(100.43);
    budgetAfterCreation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());
    budgetWithAllocation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());

    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any()))
      .thenReturn(succeededFuture(plannedFiscalYear));
    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetAfterCreation));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any()))
      .thenReturn(succeededFuture(null));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(currBudget.getId(), requestContextMock))
      .thenReturn(succeededFuture(currBudgetExpenseClasses));

    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(succeededFuture(null));
    when(fundDetailsMockService.retrieveCurrentBudget(plannedSharedBudget.getFundId(), null, true, requestContextMock)).thenReturn(succeededFuture(null));

    var future = createBudgetService.createBudget(plannedSharedBudget, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        SharedBudget resultBudget = future.result();

        assertEquals(100.43, resultBudget.getAllocated());
        assertEquals(0, plannedSharedBudget.getAllocated());
        assertEquals(plannedSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
        assertEquals(plannedSharedBudget.getFundId(), resultBudget.getFundId());
        assertEquals(0, resultBudget.getStatusExpenseClasses().size());

        JsonObject json = JsonObject.mapFrom(plannedSharedBudget);
        json.remove("statusExpenseClasses");
        Budget expectedBudget = json.mapTo(Budget.class);

        verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));
        verify(restClient).post(anyString(), eq(expectedBudget), eq(Budget.class), eq(requestContextMock));
        verify(transactionMockService).createAllocationTransaction(eq(budgetAfterCreation), eq(requestContextMock));
        verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(plannedSharedBudget), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldCreatePlannedBudgetWithoutCurrExpenseClassesAndAllocatedIfCurrentBudgetExistAndCurrExpenseClassesAbsent(VertxTestContext vertxTestContext) {
    plannedSharedBudget.withAllocated(100.43);
    budgetAfterCreation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());
    budgetWithAllocation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());

    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any()))
      .thenReturn(succeededFuture(plannedFiscalYear));
    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetAfterCreation));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any()))
      .thenReturn(succeededFuture(null));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(currBudget.getId(), requestContextMock))
      .thenReturn(succeededFuture(Collections.emptyList()));

    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(succeededFuture(null));
    when(fundDetailsMockService.retrieveCurrentBudget(plannedSharedBudget.getFundId(), null, true, requestContextMock)).thenReturn(succeededFuture(currBudget));

    Future<SharedBudget> future = createBudgetService.createBudget(plannedSharedBudget, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        SharedBudget resultBudget = result.result();
        assertEquals(100.43, resultBudget.getAllocated());
        assertEquals(0, plannedSharedBudget.getAllocated());
        assertEquals(plannedSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
        assertEquals(plannedSharedBudget.getFundId(), resultBudget.getFundId());
        assertEquals(0, resultBudget.getStatusExpenseClasses().size());

        JsonObject json = JsonObject.mapFrom(plannedSharedBudget);
        json.remove("statusExpenseClasses");
        Budget expectedBudget = json.mapTo(Budget.class);

        verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));
        verify(restClient).post(anyString(), eq(expectedBudget), eq(Budget.class), eq(requestContextMock));
        verify(transactionMockService).createAllocationTransaction(eq(budgetAfterCreation), eq(requestContextMock));
        verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(plannedSharedBudget), eq(requestContextMock));
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testShouldCreatePlannedBudgetWithExpenseClassesProvidedFromUserAndAllocatedIfCurrentBudgetExistAndCurrExpenseClassesExist(VertxTestContext vertxTestContext) {
    SharedBudget actPlannedSharedBudget = JsonObject.mapFrom(plannedSharedBudget).mapTo(SharedBudget.class);
    actPlannedSharedBudget.withStatusExpenseClasses(Collections.singletonList(statusExpenseClass));
    plannedSharedBudget.withAllocated(100.43);
    budgetAfterCreation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());
    budgetWithAllocation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());

    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any())).thenReturn(succeededFuture(plannedFiscalYear));
    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetAfterCreation));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any())).thenReturn(succeededFuture(null));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(currBudget.getId(), requestContextMock)).thenReturn(succeededFuture(currBudgetExpenseClasses));

    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(succeededFuture(null));
    when(fundDetailsMockService.retrieveCurrentBudget(plannedSharedBudget.getFundId(), null, true, requestContextMock)).thenReturn(succeededFuture(currBudget));

    var future = createBudgetService.createBudget(plannedSharedBudget, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        SharedBudget resultBudget = result.result();
        assertEquals(100.43, resultBudget.getAllocated());
        assertEquals(0, plannedSharedBudget.getAllocated());
        assertEquals(plannedSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
        assertEquals(plannedSharedBudget.getFundId(), resultBudget.getFundId());
        assertEquals(currBudgetExpenseClasses.get(0)
          .getExpenseClassId(),
            resultBudget.getStatusExpenseClasses()
              .get(0)
              .getExpenseClassId());

        JsonObject json = JsonObject.mapFrom(plannedSharedBudget);
        json.remove("statusExpenseClasses");
        Budget expectedBudget = json.mapTo(Budget.class);

        verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));
        verify(restClient).post(anyString(), eq(expectedBudget), eq(Budget.class), eq(requestContextMock));
        verify(transactionMockService).createAllocationTransaction(eq(budgetAfterCreation), eq(requestContextMock));
        verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(plannedSharedBudget), eq(requestContextMock));
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testShouldCreatePlannedBudgetWithExpenseCurrExpenseClassesAndAllocatedIfCurrentBudgetExistAndCurrExpenseClassesExist(VertxTestContext vertxTestContext) {
    plannedSharedBudget.withAllocated(100.43);
    budgetAfterCreation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());
    budgetWithAllocation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());

    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any()))
      .thenReturn(succeededFuture(plannedFiscalYear));
    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetAfterCreation));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any()))
      .thenReturn(succeededFuture(null));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(currBudget.getId(), requestContextMock))
      .thenReturn(succeededFuture(currBudgetExpenseClasses));

    when(transactionMockService.createAllocationTransaction(any(Budget.class), any())).thenReturn(succeededFuture(null));
    when(fundDetailsMockService.retrieveCurrentBudget(plannedSharedBudget.getFundId(), null, true, requestContextMock)).thenReturn(succeededFuture(currBudget));

    var future = createBudgetService.createBudget(plannedSharedBudget, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        SharedBudget resultBudget = future.result();
        assertEquals(100.43, resultBudget.getAllocated());
        assertEquals(0, plannedSharedBudget.getAllocated());
        assertEquals(plannedSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
        assertEquals(plannedSharedBudget.getFundId(), resultBudget.getFundId());
        assertEquals(currBudgetExpenseClasses.get(0)
          .getExpenseClassId(),
            resultBudget.getStatusExpenseClasses()
              .get(0)
              .getExpenseClassId());

        JsonObject json = JsonObject.mapFrom(plannedSharedBudget);
        json.remove("statusExpenseClasses");
        Budget expectedBudget = json.mapTo(Budget.class);

        verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));
        verify(restClient).post(anyString(), eq(expectedBudget), eq(Budget.class), eq(requestContextMock));
        verify(transactionMockService).createAllocationTransaction(eq(budgetAfterCreation), eq(requestContextMock));
        verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(plannedSharedBudget), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testPlannedBudgetWithAllocated(VertxTestContext vertxTestContext) {
    plannedSharedBudget.withAllocated(100.43);
    budgetAfterCreation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());
    budgetWithAllocation.setFiscalYearId(plannedSharedBudget.getFiscalYearId());

    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any()))
      .thenReturn(succeededFuture(plannedFiscalYear));
    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetAfterCreation));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any()))
      .thenReturn(succeededFuture(null));
    when(budgetExpenseClassMockService.getBudgetExpenseClasses(currBudget.getId(), requestContextMock))
      .thenReturn(succeededFuture(currBudgetExpenseClasses));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), any()))
      .thenReturn(succeededFuture(null));
    when(fundDetailsMockService.retrieveCurrentBudget(plannedSharedBudget.getFundId(), null, true, requestContextMock))
      .thenReturn(succeededFuture(currBudget));

    var future = createBudgetService.createBudget(plannedSharedBudget, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        SharedBudget resultBudget = result.result();
        assertEquals(100.43, resultBudget.getAllocated());
        assertEquals(0, plannedSharedBudget.getAllocated());
        assertEquals(plannedSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
        assertEquals(plannedSharedBudget.getFundId(), resultBudget.getFundId());

        JsonObject json = JsonObject.mapFrom(plannedSharedBudget);
        json.remove("statusExpenseClasses");
        Budget expectedBudget = json.mapTo(Budget.class);

        verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));
        verify(restClient).post(anyString(), eq(expectedBudget), eq(Budget.class), eq(requestContextMock));
        verify(transactionMockService).createAllocationTransaction(eq(budgetAfterCreation), eq(requestContextMock));
        verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(plannedSharedBudget), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testCreateBudgetWithZeroAllocated(VertxTestContext vertxTestContext) {
    currSharedBudget.withAllocated(0d);
    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any())).thenReturn(succeededFuture(plannedFiscalYear));

    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetAfterCreation));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(), any()))
      .thenReturn(succeededFuture(null));

    var future = createBudgetService.createBudget(currSharedBudget, requestContextMock);

    vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());
          SharedBudget resultBudget = result.result();

          assertEquals(currSharedBudget.getFiscalYearId(), resultBudget.getFiscalYearId());
          assertEquals(currSharedBudget.getFundId(), resultBudget.getFundId());

          verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));
          verify(restClient).post(anyString(), eq(budgetAfterCreation), eq(Budget.class), eq(requestContextMock));
          verify(transactionMockService, never()).createAllocationTransaction(eq(budgetAfterCreation), eq(requestContextMock));
          verify(budgetExpenseClassMockService).createBudgetExpenseClasses(eq(currSharedBudget), eq(requestContextMock));

          vertxTestContext.completeNow();
        });

  }

  @Test
  void testCreateBudgetWithAllocationCreationError(VertxTestContext vertxTestContext) {

    currSharedBudget.withAllocated(100.43);

    Future<Void> errorFuture = Future.failedFuture(new Exception());

    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetAfterCreation));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), any()))
      .thenReturn(errorFuture);
    when(fundFiscalYearMockService.retrievePlannedFiscalYear(any(), any()))
      .thenReturn(succeededFuture(plannedFiscalYear));
    when(fundDetailsMockService.retrieveCurrentBudget(plannedSharedBudget.getFundId(), null, true, requestContextMock))
      .thenReturn(succeededFuture(currBudget));

    var future = createBudgetService.createBudget(currSharedBudget, requestContextMock);

    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        HttpException httpException = (HttpException) result.cause();
        assertEquals(500, httpException.getCode());
        assertEquals(ALLOCATION_TRANSFER_FAILED.toError(), httpException.getErrors().getErrors().get(0));
        assertEquals(100.43, budgetAfterCreation.getAllocated());
        assertEquals(0, currSharedBudget.getAllocated());

        JsonObject json = JsonObject.mapFrom(currSharedBudget);
        json.remove("statusExpenseClasses");
        Budget expectedBudget = json.mapTo(Budget.class);

        verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));
        verify(restClient).post(anyString(), eq(expectedBudget), eq(Budget.class), eq(requestContextMock));
        verify(transactionMockService).createAllocationTransaction(eq(budgetAfterCreation), eq(requestContextMock));
        verify(budgetExpenseClassMockService, never()).createBudgetExpenseClasses(any(), any());
        vertxTestContext.completeNow();

      });
  }

  @Test
  void testCreateInactiveBudgetWithAllocated(VertxTestContext vertxTestContext) {
    currSharedBudget.setBudgetStatus(SharedBudget.BudgetStatus.INACTIVE);
    currSharedBudget.setAllocated(100.43);
    Budget budgetWithInactiveStatusAndAllocation = new Budget()
      .withId(budgetWithAllocation.getId())
      .withBudgetStatus(Budget.BudgetStatus.INACTIVE)
      .withFiscalYearId(budgetWithAllocation.getFiscalYearId())
      .withFundId(budgetWithAllocation.getFundId())
      .withInitialAllocation(100.43)
      .withAllocated(100.43);

    when(fundFiscalYearMockService.retrievePlannedFiscalYear(anyString(), eq(requestContextMock)))
      .thenReturn(succeededFuture(plannedFiscalYear));
    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(JsonObject.mapFrom(budgetAfterCreation).mapTo(Budget.class)));
    when(restClient.get(anyString(), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(budgetWithAllocation))
      .thenReturn(succeededFuture(budgetWithInactiveStatusAndAllocation));
    when(restClient.put(anyString(), any(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture());
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(SharedBudget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(null));
    when(transactionMockService.createAllocationTransaction(any(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(null));

    var future = createBudgetService.createBudget(currSharedBudget, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var resultBudget = result.result();
        assertEquals(100.43, resultBudget.getAllocated());
        assertEquals(SharedBudget.BudgetStatus.INACTIVE, resultBudget.getBudgetStatus());

        verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));

        ArgumentCaptor<Budget> postBudgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(restClient).post(anyString(), postBudgetCaptor.capture(), eq(Budget.class), eq(requestContextMock));
        List<Budget> postBudgets = postBudgetCaptor.getAllValues();
        assertThat(postBudgets, hasSize(1));
        assertEquals(budgetAfterCreation, postBudgets.getFirst());

        ArgumentCaptor<Budget> putBudgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(restClient).put(anyString(), putBudgetCaptor.capture(), eq(requestContextMock));
        List<Budget> putBudgets = putBudgetCaptor.getAllValues();
        assertThat(putBudgets, hasSize(1));
        assertEquals(budgetWithInactiveStatusAndAllocation, putBudgets.getFirst());

        verify(restClient, times(2)).get(anyString(), eq(Budget.class), eq(requestContextMock));

        verify(transactionMockService).createAllocationTransaction(any(Budget.class), eq(requestContextMock));
        verify(budgetExpenseClassMockService).createBudgetExpenseClasses(any(SharedBudget.class), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testCreateInactiveBudgetWithoutAllocated(VertxTestContext vertxTestContext) {
    currSharedBudget.setBudgetStatus(SharedBudget.BudgetStatus.INACTIVE);
    currSharedBudget.setAllocated(0d);
    budgetAfterCreation.setBudgetStatus(Budget.BudgetStatus.INACTIVE);

    when(fundFiscalYearMockService.retrievePlannedFiscalYear(anyString(), eq(requestContextMock)))
      .thenReturn(succeededFuture(plannedFiscalYear));
    when(restClient.post(anyString(), any(Budget.class), eq(Budget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(JsonObject.mapFrom(budgetAfterCreation).mapTo(Budget.class)));
    when(budgetExpenseClassMockService.createBudgetExpenseClasses(any(SharedBudget.class), eq(requestContextMock)))
      .thenReturn(succeededFuture(null));

    var future = createBudgetService.createBudget(currSharedBudget, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var resultBudget = result.result();
        assertEquals(0d, resultBudget.getAllocated());
        assertEquals(SharedBudget.BudgetStatus.INACTIVE, resultBudget.getBudgetStatus());

        verify(fundFiscalYearMockService).retrievePlannedFiscalYear(eq(currSharedBudget.getFundId()), eq(requestContextMock));

        ArgumentCaptor<Budget> postBudgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(restClient).post(anyString(), postBudgetCaptor.capture(), eq(Budget.class), eq(requestContextMock));
        List<Budget> postBudgets = postBudgetCaptor.getAllValues();
        assertThat(postBudgets, hasSize(1));
        assertEquals(budgetAfterCreation, postBudgets.getFirst());

        verify(restClient, never()).put(anyString(), any(Budget.class), any(RequestContext.class));

        verify(restClient, never()).get(anyString(), eq(Budget.class), eq(requestContextMock));

        verify(transactionMockService, never()).createAllocationTransaction(any(), any());
        verify(budgetExpenseClassMockService).createBudgetExpenseClasses(any(SharedBudget.class), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testCreateBudgetWithNegativeAllocation(VertxTestContext vertxTestContext) {
    currSharedBudget.setAllocated(-10d);

    var future = createBudgetService.createBudget(currSharedBudget, requestContextMock);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        HttpException httpException = (HttpException)result.cause();
        assertEquals(422, httpException.getCode());
        assertEquals(NEGATIVE_ALLOCATION.getCode(), httpException.getErrors().getErrors().getFirst().getCode());

        vertxTestContext.completeNow();
      });
  }

}
