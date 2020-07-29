package org.folio.services;

import static java.util.Collections.emptyList;
import static org.folio.rest.jaxrs.model.BudgetExpenseClass.Status.INACTIVE;
import static org.folio.rest.jaxrs.model.BudgetExpenseClass.Status.fromValue;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Vertx;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.jaxrs.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.impl.EventLoopContext;

public class BudgetExpenseClassServiceTest {

  @InjectMocks
  private BudgetExpenseClassService budgetExpenseClassService;

  @Mock
  private RestClient budgetExpenseClassClientMock;

  @Mock
  private TransactionService transactionServiceMock;

  @Mock
  private RequestContext requestContextMock;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void testGetBudgetExpenseClasses() {

    String budgetId = UUID.randomUUID().toString();
    List<BudgetExpenseClass> expectedBudgetExpenseClasses = Collections.singletonList(new BudgetExpenseClass()
      .withBudgetId(budgetId)
      .withId(UUID.randomUUID().toString()));

    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(expectedBudgetExpenseClasses)
      .withTotalRecords(1);

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(budgetExpenseClassCollection));

    CompletableFuture<List<BudgetExpenseClass>> resultFuture = budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContextMock);

    String expectedQuery =  String.format("budgetId==%s", budgetId);
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));

    List<BudgetExpenseClass> resultBudgetExpenseClasses = resultFuture.join();
    assertEquals(expectedBudgetExpenseClasses, resultBudgetExpenseClasses);

  }

  @Test
  void testCreateBudgetWithExpenseClasses() {
    StatusExpenseClass expenseClass1 = new StatusExpenseClass()
      .withExpenseClassId(UUID.randomUUID().toString())
      .withStatus(StatusExpenseClass.Status.ACTIVE);

    StatusExpenseClass expenseClass2 = new StatusExpenseClass()
      .withExpenseClassId(UUID.randomUUID().toString())
      .withStatus(StatusExpenseClass.Status.INACTIVE);

    SharedBudget sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withStatusExpenseClasses(Arrays.asList(expenseClass1, expenseClass2));

    when(requestContextMock.getContext()).thenReturn(mock(EventLoopContext.class));
    when(budgetExpenseClassClientMock.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(new BudgetExpenseClass()));

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

    SharedBudget sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString());

    CompletableFuture<Void> future = budgetExpenseClassService.createBudgetExpenseClasses(sharedBudget, requestContextMock);
    future.join();

    assertFalse(future.isCompletedExceptionally());
    verify(requestContextMock, never()).getContext();
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());

  }

  @Test
  void testUpdateBudgetExpenseClassesLinksWithoutExpenseClasses() {
    SharedBudget sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString());

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(new BudgetExpenseClassCollection()));

    CompletableFuture<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
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
    SharedBudget sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass3 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClassCollection));
    when(transactionServiceMock.getTransactions(anyList(), any(), any())).thenReturn(CompletableFuture.completedFuture(emptyList()));
    when(budgetExpenseClassClientMock.delete(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    CompletableFuture<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).put(any(), any(), any());
    ArgumentCaptor<List<BudgetExpenseClass>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(transactionServiceMock).getTransactions(listArgumentCaptor.capture(), eq(sharedBudget), eq(requestContextMock));
    List<BudgetExpenseClass> budgetExpenseClasses = listArgumentCaptor.getValue();
    assertThat(budgetExpenseClasses, containsInAnyOrder(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(budgetExpenseClassClientMock, times(3)).delete(idArgumentCaptor.capture(), any());
    List<String> ids = idArgumentCaptor.getAllValues();
    assertThat(ids, containsInAnyOrder(budgetExpenseClass1.getId(), budgetExpenseClass2.getId(), budgetExpenseClass3.getId()));
  }


  @Test
  void testUpdateBudgetExpenseClassesLinks_withoutStatusExpenseClasses_transactionsAssigned_budgetExpenseClassesDeletionProhibited() {
    SharedBudget sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass3 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClassCollection));
    when(transactionServiceMock.getTransactions(anyList(), any(), any())).thenReturn(CompletableFuture.completedFuture(Collections.singletonList(new Transaction())));
    when(budgetExpenseClassClientMock.delete(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    CompletableFuture<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
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
    verify(transactionServiceMock).getTransactions(listArgumentCaptor.capture(), eq(sharedBudget), eq(requestContextMock));
    List<BudgetExpenseClass> budgetExpenseClasses = listArgumentCaptor.getValue();
    assertThat(budgetExpenseClasses, containsInAnyOrder(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    verify(budgetExpenseClassClientMock, never()).delete(any(), any());

  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_withSameStatusExpenseClassesAsExistingBudgetExpenseClassesNoUpdates() {
    SharedBudget sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass3 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    StatusExpenseClass statusExpenseClass1 = new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClass1.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.fromValue(budgetExpenseClass1.getStatus().value()));
    StatusExpenseClass statusExpenseClass2 = new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClass2.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.fromValue(budgetExpenseClass2.getStatus().value()));
    StatusExpenseClass statusExpenseClass3 = new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClass3.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.fromValue(budgetExpenseClass3.getStatus().value()));
    List<StatusExpenseClass> statusExpenseClasses = Arrays.asList(statusExpenseClass1, statusExpenseClass2, statusExpenseClass3);
    sharedBudget.withStatusExpenseClasses(statusExpenseClasses);

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClassCollection));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    CompletableFuture<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).put(any(), any(), any());
    verify(transactionServiceMock, never()).getTransactions(any(), any(), any());
    verify(budgetExpenseClassClientMock, never()).delete(any(), any());

  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_withUpdatedStatusExpenseClasses_existingBudgetExpenseClassesHasToBeUpdated() {
    SharedBudget sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClass3 = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    StatusExpenseClass statusExpenseClass1 = new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClass1.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.fromValue(budgetExpenseClass1.getStatus().value()));
    StatusExpenseClass statusExpenseClass2 = new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClass2.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.INACTIVE);
    StatusExpenseClass statusExpenseClass3 = new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClass3.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.INACTIVE);
    List<StatusExpenseClass> statusExpenseClasses = Arrays.asList(statusExpenseClass1, statusExpenseClass2, statusExpenseClass3);
    sharedBudget.withStatusExpenseClasses(statusExpenseClasses);

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClassCollection));
    when(budgetExpenseClassClientMock.put(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    CompletableFuture<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock, never()).post(any(), any(), any());

    verify(transactionServiceMock, never()).getTransactions(any(), any(), any());
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
    SharedBudget sharedBudget = new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClassToBeDeleted = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());
    BudgetExpenseClass budgetExpenseClassToBeUpdated = new BudgetExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withExpenseClassId(UUID.randomUUID().toString());

    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClassToBeDeleted, budgetExpenseClassToBeUpdated));

    StatusExpenseClass updatingStatusExpenseClass = new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClassToBeUpdated.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.INACTIVE);
    StatusExpenseClass newStatusExpenseClass = new StatusExpenseClass()
      .withExpenseClassId(UUID.randomUUID().toString())
      .withStatus(StatusExpenseClass.Status.INACTIVE);
    List<StatusExpenseClass> statusExpenseClasses = Arrays.asList(updatingStatusExpenseClass, newStatusExpenseClass);
    sharedBudget.withStatusExpenseClasses(statusExpenseClasses);

    BudgetExpenseClass newBudgetExpenseClass = new BudgetExpenseClass()
      .withExpenseClassId(newStatusExpenseClass.getExpenseClassId())
      .withBudgetId(sharedBudget.getId())
      .withStatus(INACTIVE);

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClassCollection));
    when(budgetExpenseClassClientMock.put(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(budgetExpenseClassClientMock.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(newBudgetExpenseClass));
    when(budgetExpenseClassClientMock.delete(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(transactionServiceMock.getTransactions(anyList(), any(), any())).thenReturn(CompletableFuture.completedFuture(emptyList()));
    when(requestContextMock.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    CompletableFuture<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    future.join();
    assertFalse(future.isCompletedExceptionally());

    String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock), eq(BudgetExpenseClassCollection.class));
    verify(budgetExpenseClassClientMock).post(eq(newBudgetExpenseClass), eq(requestContextMock), eq(BudgetExpenseClass.class));

    List<BudgetExpenseClass> expectedToDeleteList = new ArrayList<>();
    expectedToDeleteList.add(budgetExpenseClassToBeDeleted);
    verify(transactionServiceMock).getTransactions(eq(expectedToDeleteList), eq(sharedBudget), eq(requestContextMock));
    verify(budgetExpenseClassClientMock).delete(eq(budgetExpenseClassToBeDeleted.getId()), eq(requestContextMock));


    ArgumentCaptor<BudgetExpenseClass> budgetExpenseClassArgumentCaptor = ArgumentCaptor.forClass(BudgetExpenseClass.class);
    verify(budgetExpenseClassClientMock)
      .put(eq(budgetExpenseClassToBeUpdated.getId()), budgetExpenseClassArgumentCaptor.capture(), eq(requestContextMock));

    BudgetExpenseClass budgetExpenseClass = budgetExpenseClassArgumentCaptor.getValue();

    assertEquals(budgetExpenseClassToBeUpdated, budgetExpenseClass);
    assertThat(budgetExpenseClass, hasProperty("status", is(INACTIVE)));

  }
}
