package org.folio.services.budget;

import io.vertx.core.Vertx;
import io.vertx.core.impl.EventLoopContext;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.transactions.CommonTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import io.vertx.core.Future;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.emptyList;
import static org.folio.rest.jaxrs.model.BudgetExpenseClass.Status.INACTIVE;
import static org.folio.rest.jaxrs.model.BudgetExpenseClass.Status.fromValue;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BudgetExpenseClassServiceTest {

  @InjectMocks
  private BudgetExpenseClassService budgetExpenseClassService;

  @Mock
  private RestClient budgetExpenseClassClientMock;

  @Mock
  private CommonTransactionService transactionServiceMock;

  @Mock
  private RequestContext requestContextMock;

  private SharedBudget sharedBudget;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    sharedBudget =  new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());
  }

  @Test
  void testGetBudgetExpenseClasses() {

    String budgetId = sharedBudget.getId();
    List<BudgetExpenseClass> expectedBudgetExpenseClasses = Collections.singletonList(new BudgetExpenseClass()
      .withBudgetId(budgetId)
      .withId(UUID.randomUUID().toString()));

    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(expectedBudgetExpenseClasses)
      .withTotalRecords(1);

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(succeededFuture(budgetExpenseClassCollection));

    Future<List<BudgetExpenseClass>> resultFuture = budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContextMock);

    String expectedQuery =  String.format("budgetId==%s", budgetId);
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));

    List<BudgetExpenseClass> resultBudgetExpenseClasses = resultFuture.join();
    assertEquals(expectedBudgetExpenseClasses, resultBudgetExpenseClasses);

  }

  @Test
  void testCreateBudgetWithExpenseClasses() {
    StatusExpenseClass expenseClass1 = getNewStatusExpenseClass(UUID.randomUUID().toString());

    StatusExpenseClass expenseClass2 = getNewStatusExpenseClass(UUID.randomUUID().toString())
      .withStatus(StatusExpenseClass.Status.INACTIVE);

    sharedBudget.withStatusExpenseClasses(Arrays.asList(expenseClass1, expenseClass2));

    when(requestContextMock.getContext()).thenReturn(mock(EventLoopContext.class));
    when(budgetExpenseClassClientMock.post(any(), any(), any())).thenReturn(succeededFuture(new BudgetExpenseClass()));

    budgetExpenseClassService.createBudgetExpenseClasses(sharedBudget, requestContextMock);

    verify(requestContextMock).getContext();

    ArgumentCaptor<BudgetExpenseClass> expenseClassArgumentCaptor = ArgumentCaptor.forClass(BudgetExpenseClass.class);
    verify(budgetExpenseClassClientMock, times(2)).post(expenseClassArgumentCaptor.capture(), eq(requestContextMock), eq(BudgetExpenseClass.class));

    List<BudgetExpenseClass> budgetExpenseClasses = expenseClassArgumentCaptor.getAllValues();

    BudgetExpenseClass expected1 = new BudgetExpenseClass()
      .withBudgetId(sharedBudget.getId())
      .withExpenseClassId(expenseClass1.getExpenseClassId())
      .withStatus(fromValue(expenseClass1.getStatus().toString()));

    BudgetExpenseClass expected2 = new BudgetExpenseClass()
      .withBudgetId(sharedBudget.getId())
      .withExpenseClassId(expenseClass2.getExpenseClassId())
      .withStatus(fromValue(expenseClass2.getStatus().toString()));

    assertThat(budgetExpenseClasses, contains(expected1, expected2));

  }

  @Test
  void testCreateBudgetWithoutExpenseClasses() {

    Future<Void> future = budgetExpenseClassService.createBudgetExpenseClasses(sharedBudget, requestContextMock);
    future.join();

    assertFalse(future.isCompletedExceptionally());
    verify(requestContextMock, never()).getContext();
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());

  }

  @Test
  void testUpdateBudgetExpenseClassesLinksWithoutExpenseClasses() {

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(succeededFuture(new BudgetExpenseClassCollection()));

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).put(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).delete(any(), any());
  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_withoutStatusExpenseClasses_noTransactionsAssigned_existingBudgetExpenseClassesHasToBeDeleted() {

    BudgetExpenseClass budgetExpenseClass1 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass2 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass3 = getNewBudgetExpenseClass();
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(transactionServiceMock.retrieveTransactions(anyList(), any(), any())).thenReturn(succeededFuture(emptyList()));
    when(budgetExpenseClassClientMock.delete(anyString(), any())).thenReturn(succeededFuture(null));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).put(any(), any(), any());
    ArgumentCaptor<List<BudgetExpenseClass>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(transactionServiceMock).retrieveTransactions(listArgumentCaptor.capture(), eq(sharedBudget), eq(requestContextMock));
    List<BudgetExpenseClass> budgetExpenseClasses = listArgumentCaptor.getValue();
    assertThat(budgetExpenseClasses, containsInAnyOrder(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(budgetExpenseClassClientMock, times(3)).delete(idArgumentCaptor.capture(), any());
    List<String> ids = idArgumentCaptor.getAllValues();
    assertThat(ids, containsInAnyOrder(budgetExpenseClass1.getId(), budgetExpenseClass2.getId(), budgetExpenseClass3.getId()));
  }


  @Test
  void testUpdateBudgetExpenseClassesLinks_withoutStatusExpenseClasses_transactionsAssigned_budgetExpenseClassesDeletionProhibited() {

    BudgetExpenseClass budgetExpenseClass1 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass2 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass3 = getNewBudgetExpenseClass();
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(transactionServiceMock.retrieveTransactions(anyList(), any(), any())).thenReturn(succeededFuture(Collections.singletonList(new Transaction())));
    when(budgetExpenseClassClientMock.delete(anyString(), any())).thenReturn(succeededFuture(null));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    ExecutionException executionException = assertThrows(ExecutionException.class,future::get);

    assertThat(executionException.getCause(), instanceOf(HttpException.class));

    HttpException httpException = (HttpException) executionException.getCause();

    assertThat(httpException.getErrors().getErrors(), hasSize(1));
    assertEquals(TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR.toError(), httpException.getErrors().getErrors().get(0));
    assertEquals(400, httpException.getCode());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).put(any(), any(), any());
    ArgumentCaptor<List<BudgetExpenseClass>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(transactionServiceMock).retrieveTransactions(listArgumentCaptor.capture(), eq(sharedBudget), eq(requestContextMock));
    List<BudgetExpenseClass> budgetExpenseClasses = listArgumentCaptor.getValue();
    assertThat(budgetExpenseClasses, containsInAnyOrder(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    verify(budgetExpenseClassClientMock, never()).delete(any(), any());

  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_withSameStatusExpenseClassesAsExistingBudgetExpenseClassesNoUpdates() {

    BudgetExpenseClass budgetExpenseClass1 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass2 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass3 = getNewBudgetExpenseClass();
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    StatusExpenseClass statusExpenseClass1 = getNewStatusExpenseClass(budgetExpenseClass1.getExpenseClassId());
    StatusExpenseClass statusExpenseClass2 = getNewStatusExpenseClass(budgetExpenseClass2.getExpenseClassId());
    StatusExpenseClass statusExpenseClass3 = getNewStatusExpenseClass(budgetExpenseClass3.getExpenseClassId());
    List<StatusExpenseClass> statusExpenseClasses = Arrays.asList(statusExpenseClass1, statusExpenseClass2, statusExpenseClass3);
    sharedBudget.withStatusExpenseClasses(statusExpenseClasses);

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).put(any(), any(), any());
    verify(transactionServiceMock, never()).retrieveTransactions(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).delete(any(), any());

  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_withUpdatedStatusExpenseClasses_existingBudgetExpenseClassesHasToBeUpdated() {

    BudgetExpenseClass budgetExpenseClass1 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass2 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass3 = getNewBudgetExpenseClass();
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    StatusExpenseClass statusExpenseClass1 = getNewStatusExpenseClass(budgetExpenseClass1.getExpenseClassId());
    StatusExpenseClass statusExpenseClass2 = getNewStatusExpenseClass(budgetExpenseClass2.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.INACTIVE);
    StatusExpenseClass statusExpenseClass3 = getNewStatusExpenseClass(budgetExpenseClass3.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.INACTIVE);
    List<StatusExpenseClass> statusExpenseClasses = Arrays.asList(statusExpenseClass1, statusExpenseClass2, statusExpenseClass3);
    sharedBudget.withStatusExpenseClasses(statusExpenseClasses);

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(budgetExpenseClassClientMock.put(anyString(), any(), any())).thenReturn(succeededFuture(null));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());

    verify(transactionServiceMock, never()).retrieveTransactions(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).delete(any(), any());

    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<BudgetExpenseClass> budgetExpenseClassArgumentCaptor = ArgumentCaptor.forClass(BudgetExpenseClass.class);
    verify(budgetExpenseClassClientMock, times(2))
      .put(idArgumentCaptor.capture(), budgetExpenseClassArgumentCaptor.capture(), eq(requestContextMock));
    List<String> ids = idArgumentCaptor.getAllValues();
    List<BudgetExpenseClass> budgetExpenseClasses = budgetExpenseClassArgumentCaptor.getAllValues();

    assertThat(ids, containsInAnyOrder(budgetExpenseClass2.getId(), budgetExpenseClass3.getId()));

    assertThat(budgetExpenseClasses, everyItem(hasProperty("status", is(INACTIVE))));
    assertThat(budgetExpenseClasses, containsInAnyOrder(budgetExpenseClass2, budgetExpenseClass3));

  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_complexTestWithBudgetExpenseClassDeletionUpdateCreation() {

    BudgetExpenseClass budgetExpenseClassToBeDeleted = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClassToBeUpdated = getNewBudgetExpenseClass();

    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClassToBeDeleted, budgetExpenseClassToBeUpdated));

    StatusExpenseClass updatingStatusExpenseClass = getNewStatusExpenseClass(budgetExpenseClassToBeUpdated.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.INACTIVE);
    StatusExpenseClass newStatusExpenseClass = getNewStatusExpenseClass(UUID.randomUUID().toString())
      .withStatus(StatusExpenseClass.Status.INACTIVE);
    List<StatusExpenseClass> statusExpenseClasses = Arrays.asList(updatingStatusExpenseClass, newStatusExpenseClass);
    sharedBudget.withStatusExpenseClasses(statusExpenseClasses);

    BudgetExpenseClass newBudgetExpenseClass = new BudgetExpenseClass()
      .withExpenseClassId(newStatusExpenseClass.getExpenseClassId())
      .withBudgetId(sharedBudget.getId())
      .withStatus(INACTIVE);

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(budgetExpenseClassClientMock.put(anyString(), any(), any())).thenReturn(succeededFuture(null));
    when(budgetExpenseClassClientMock.post(any(), any(), any())).thenReturn(succeededFuture(newBudgetExpenseClass));
    when(budgetExpenseClassClientMock.delete(anyString(), any())).thenReturn(succeededFuture(null));
    when(transactionServiceMock.retrieveTransactions(anyList(), any(), any())).thenReturn(succeededFuture(emptyList()));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock).post(eq(newBudgetExpenseClass), eq(requestContextMock), eq(BudgetExpenseClass.class));

    List<BudgetExpenseClass> expectedToDeleteList = new ArrayList<>();
    expectedToDeleteList.add(budgetExpenseClassToBeDeleted);
    verify(transactionServiceMock).retrieveTransactions(eq(expectedToDeleteList), eq(sharedBudget), eq(requestContextMock));
    verify(budgetExpenseClassClientMock).delete(eq(budgetExpenseClassToBeDeleted.getId()), eq(requestContextMock));


    ArgumentCaptor<BudgetExpenseClass> budgetExpenseClassArgumentCaptor = ArgumentCaptor.forClass(BudgetExpenseClass.class);
    verify(budgetExpenseClassClientMock)
      .put(eq(budgetExpenseClassToBeUpdated.getId()), budgetExpenseClassArgumentCaptor.capture(), eq(requestContextMock));

    BudgetExpenseClass budgetExpenseClass = budgetExpenseClassArgumentCaptor.getValue();

    assertEquals(budgetExpenseClassToBeUpdated, budgetExpenseClass);
    assertThat(budgetExpenseClass, hasProperty("status", is(INACTIVE)));

  }

  @Test
  public void testGetBudgetExpensesClass() {
    //Given
    List<String> budgetsIds = Arrays.asList("1", "2", "3");
    List<BudgetExpenseClass> budgetExpenseClassList = new ArrayList<>();
    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass().withId("1");
    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass().withId("2");
    BudgetExpenseClass budgetExpenseClass3 = new BudgetExpenseClass().withId("3");
    budgetExpenseClassList.add(budgetExpenseClass1);
    budgetExpenseClassList.add(budgetExpenseClass2);
    budgetExpenseClassList.add(budgetExpenseClass3);
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection();
    budgetExpenseClassCollection.setBudgetExpenseClasses(budgetExpenseClassList);
    //When
    when(budgetExpenseClassClientMock.get(any(), any(), eq(BudgetExpenseClassCollection.class))).thenReturn(succeededFuture(budgetExpenseClassCollection));
    List<BudgetExpenseClass> budgetExpenseClassListReceived = budgetExpenseClassService.getBudgetExpensesClassByIds(budgetsIds, requestContextMock).join();

    List<BudgetExpenseClass> budgetExpenseClassListFrom = budgetExpenseClassService.getBudgetExpensesClass(budgetsIds, requestContextMock).join();

    //Then
    assertEquals(budgetExpenseClass1.getId(), budgetExpenseClassListFrom.get(0).getId());
    assertEquals(budgetExpenseClass2.getId(), budgetExpenseClassListFrom.get(1).getId());
    assertEquals(budgetExpenseClass3.getId(), budgetExpenseClassListFrom.get(2).getId());
  }

  @Test
  public void testGetBudgetExpensesClassByIds() {
    //Given
    List<String> budgetsIds = Arrays.asList("1", "2", "3");
    List<BudgetExpenseClass> budgetExpenseClassList = new ArrayList<>();
    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass().withId("1");
    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass().withId("2");
    BudgetExpenseClass budgetExpenseClass3 = new BudgetExpenseClass().withId("3");
    budgetExpenseClassList.add(budgetExpenseClass1);
    budgetExpenseClassList.add(budgetExpenseClass2);
    budgetExpenseClassList.add(budgetExpenseClass3);
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection();
    budgetExpenseClassCollection.setBudgetExpenseClasses(budgetExpenseClassList);
    //When
    when(budgetExpenseClassClientMock.get(any(), any(), eq(BudgetExpenseClassCollection.class))).thenReturn(succeededFuture(budgetExpenseClassCollection));
    List<BudgetExpenseClass> budgetExpenseClassListReceived = budgetExpenseClassService.getBudgetExpensesClassByIds(budgetsIds, requestContextMock).join();
    //Then
    assertEquals(budgetExpenseClass1.getId(), budgetExpenseClassListReceived.get(0).getId());
  }

  private BudgetExpenseClass getNewBudgetExpenseClass() {
    return new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
  }

  private StatusExpenseClass getNewStatusExpenseClass(String s) {
    return new StatusExpenseClass()
      .withExpenseClassId(s);
  }
}
