package org.folio.services.budget;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Collections.emptyList;
import static org.folio.rest.jaxrs.model.BudgetExpenseClass.Status.INACTIVE;
import static org.folio.rest.jaxrs.model.BudgetExpenseClass.Status.fromValue;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR;
import static org.folio.rest.util.ResourcePathResolver.BUDGET_EXPENSE_CLASSES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import io.vertx.core.Context;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.transactions.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class BudgetExpenseClassServiceTest {

  @InjectMocks
  private BudgetExpenseClassService budgetExpenseClassService;

  @Mock
  private RestClient restClient;

  @Mock
  private TransactionService transactionServiceMock;

  @Mock
  private RequestContext requestContextMock;

  private SharedBudget sharedBudget;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    sharedBudget =  new SharedBudget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());
  }

  @Test
  void testGetBudgetExpenseClasses(VertxTestContext vertxTestContext) {

    String budgetId = sharedBudget.getId();
    List<BudgetExpenseClass> expectedBudgetExpenseClasses = Collections.singletonList(new BudgetExpenseClass()
      .withBudgetId(budgetId)
      .withId(UUID.randomUUID().toString()));

    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(expectedBudgetExpenseClasses)
      .withTotalRecords(1);

    when(restClient.get(anyString(), any(), any()))
      .thenReturn(succeededFuture(budgetExpenseClassCollection));

    var future = budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        String expectedQuery =  String.format("budgetId==%s", budgetId);
        verify(restClient).get(assertQueryContains(expectedQuery), eq(BudgetExpenseClassCollection.class), eq(requestContextMock));
        assertEquals(expectedBudgetExpenseClasses, result.result());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testCreateBudgetWithExpenseClasses(VertxTestContext vertxTestContext) {
    StatusExpenseClass expenseClass1 = getNewStatusExpenseClass(UUID.randomUUID().toString());

    StatusExpenseClass expenseClass2 = getNewStatusExpenseClass(UUID.randomUUID().toString())
      .withStatus(StatusExpenseClass.Status.INACTIVE);

    sharedBudget.withStatusExpenseClasses(Arrays.asList(expenseClass1, expenseClass2));

    when(requestContextMock.context()).thenReturn(mock(Context.class));
    when(restClient.post(anyString(), any(), eq(BudgetExpenseClass.class), eq(requestContextMock))).thenReturn(succeededFuture(new BudgetExpenseClass()));

    var future = budgetExpenseClassService.createBudgetExpenseClasses(sharedBudget, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        ArgumentCaptor<BudgetExpenseClass> expenseClassArgumentCaptor = ArgumentCaptor.forClass(BudgetExpenseClass.class);
        verify(restClient, times(2))
          .post(eq(resourcesPath(BUDGET_EXPENSE_CLASSES)), expenseClassArgumentCaptor.capture(), eq(BudgetExpenseClass.class), eq(requestContextMock));

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
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testCreateBudgetWithoutExpenseClasses(VertxTestContext vertxTestContext) {

    Future<Void> future = budgetExpenseClassService.createBudgetExpenseClasses(sharedBudget, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(future.succeeded());
        verify(requestContextMock, never()).context();
        verify(restClient, never()).post(anyString(), any(), any(), any());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testUpdateBudgetExpenseClassesLinksWithoutExpenseClasses(VertxTestContext vertxTestContext) {
    when(restClient.get(anyString(), eq(BudgetExpenseClassCollection.class), any()))
      .thenReturn(succeededFuture(new BudgetExpenseClassCollection()));

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(future.succeeded());

        String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(BudgetExpenseClassCollection.class), eq(requestContextMock));
        verify(restClient, never()).post(anyString(), any(), any(), any());
        verify(restClient, never()).put(anyString(), any(), any());
        verify(restClient, never()).delete(anyString(), any());
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_withoutStatusExpenseClasses_noTransactionsAssigned_existingBudgetExpenseClassesHasToBeDeleted(VertxTestContext vertxTestContext) {

    BudgetExpenseClass budgetExpenseClass1 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass2 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass3 = getNewBudgetExpenseClass();
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(transactionServiceMock.getBudgetTransactionsWithExpenseClasses(anyList(), any(), any())).thenReturn(succeededFuture(emptyList()));
    when(restClient.delete(anyString(), any())).thenReturn(succeededFuture(null));
    when(requestContextMock.context()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(future.succeeded());

        String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(BudgetExpenseClassCollection.class), eq(requestContextMock));
        verify(restClient, never()).post(anyString(), any(), any(), any());
        verify(restClient, never()).put(anyString(), any(), any());
        ArgumentCaptor<List<BudgetExpenseClass>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(transactionServiceMock).getBudgetTransactionsWithExpenseClasses(listArgumentCaptor.capture(), eq(sharedBudget), eq(requestContextMock));
        List<BudgetExpenseClass> budgetExpenseClasses = listArgumentCaptor.getValue();
        assertThat(budgetExpenseClasses, containsInAnyOrder(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

        verify(restClient, times(3)).delete(anyString(), any());

        vertxTestContext.completeNow();
      });
  }


  @Test
  void testUpdateBudgetExpenseClassesLinks_withoutStatusExpenseClasses_transactionsAssigned_budgetExpenseClassesDeletionProhibited(VertxTestContext vertxTestContext) {

    BudgetExpenseClass budgetExpenseClass1 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass2 = getNewBudgetExpenseClass();
    BudgetExpenseClass budgetExpenseClass3 = getNewBudgetExpenseClass();
    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(transactionServiceMock.getBudgetTransactionsWithExpenseClasses(anyList(), any(), any())).thenReturn(succeededFuture(Collections.singletonList(new Transaction())));
    when(restClient.delete(anyString(), any())).thenReturn(succeededFuture(null));
    when(requestContextMock.context()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);

    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        HttpException httpException = (HttpException) result.cause();

        assertThat(httpException.getErrors().getErrors(), hasSize(1));
        assertEquals(TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR.toError(), httpException.getErrors().getErrors().get(0));
        assertEquals(400, httpException.getCode());

        String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(BudgetExpenseClassCollection.class), eq(requestContextMock));
        verify(restClient, never()).post(anyString(), any(), any(), any());
        verify(restClient, never()).put(anyString(), any(), any());
        ArgumentCaptor<List<BudgetExpenseClass>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(transactionServiceMock).getBudgetTransactionsWithExpenseClasses(listArgumentCaptor.capture(), eq(sharedBudget), eq(requestContextMock));
        List<BudgetExpenseClass> budgetExpenseClasses = listArgumentCaptor.getValue();
        assertThat(budgetExpenseClasses, containsInAnyOrder(budgetExpenseClass1, budgetExpenseClass2, budgetExpenseClass3));

        verify(restClient, never()).delete(anyString(), any());

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_withSameStatusExpenseClassesAsExistingBudgetExpenseClassesNoUpdates(VertxTestContext vertxTestContext) {

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

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(requestContextMock.context()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(future.succeeded());

        String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(BudgetExpenseClassCollection.class), eq(requestContextMock));
        verify(restClient, never()).post(anyString(), any(), any(), any());
        verify(restClient, never()).put(anyString(), any(), any());
        verify(transactionServiceMock, never()).getBudgetTransactionsWithExpenseClasses(any(), any(), any());
        verify(restClient, never()).delete(anyString(), any());

        vertxTestContext.completeNow();
      });

  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_withUpdatedStatusExpenseClasses_existingBudgetExpenseClassesHasToBeUpdated(VertxTestContext vertxTestContext) {

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

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(restClient.put(anyString(), any(), any())).thenReturn(succeededFuture(null));
    when(requestContextMock.context()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(future.succeeded());

        String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(BudgetExpenseClassCollection.class), eq(requestContextMock));
        verify(restClient, never()).post(anyString(), any(), any(), any());

        verify(transactionServiceMock, never()).getBudgetTransactionsWithExpenseClasses(any(), any(), any());
        verify(restClient, never()).delete(anyString(), any());

        ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BudgetExpenseClass> budgetExpenseClassArgumentCaptor = ArgumentCaptor.forClass(BudgetExpenseClass.class);
        verify(restClient, times(2))
          .put(idArgumentCaptor.capture(), budgetExpenseClassArgumentCaptor.capture(), eq(requestContextMock));
        List<String> ids = idArgumentCaptor.getAllValues();
        List<BudgetExpenseClass> budgetExpenseClasses = budgetExpenseClassArgumentCaptor.getAllValues();

        assertThat(ids, containsInAnyOrder(
          resourceByIdPath(BUDGET_EXPENSE_CLASSES, budgetExpenseClass2.getId()),
          resourceByIdPath(BUDGET_EXPENSE_CLASSES, budgetExpenseClass3.getId())));

        assertThat(budgetExpenseClasses, everyItem(hasProperty("status", is(INACTIVE))));
        assertThat(budgetExpenseClasses, containsInAnyOrder(budgetExpenseClass2, budgetExpenseClass3));

        vertxTestContext.completeNow();
      });

  }

  @Test
  void testUpdateBudgetExpenseClassesLinks_complexTestWithBudgetExpenseClassDeletionUpdateCreation(VertxTestContext vertxTestContext) {

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

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    when(restClient.put(anyString(), any(), any())).thenReturn(succeededFuture(null));
    when(restClient.post(anyString(), any(), any(), any())).thenReturn(succeededFuture(newBudgetExpenseClass));
    when(restClient.delete(anyString(), any())).thenReturn(succeededFuture(null));
    when(transactionServiceMock.getBudgetTransactionsWithExpenseClasses(anyList(), any(), any())).thenReturn(succeededFuture(emptyList()));
    when(requestContextMock.context()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(future.succeeded());

        String expectedQuery = String.format("budgetId==%s", sharedBudget.getId());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(BudgetExpenseClassCollection.class), eq(requestContextMock));
        verify(restClient).post(ArgumentMatchers.contains(resourcesPath(BUDGET_EXPENSE_CLASSES)), eq(newBudgetExpenseClass), eq(BudgetExpenseClass.class), eq(requestContextMock));

        List<BudgetExpenseClass> expectedToDeleteList = new ArrayList<>();
        expectedToDeleteList.add(budgetExpenseClassToBeDeleted);
        verify(transactionServiceMock).getBudgetTransactionsWithExpenseClasses(eq(expectedToDeleteList), eq(sharedBudget), eq(requestContextMock));
        verify(restClient).delete(assertQueryContains(budgetExpenseClassToBeDeleted.getId()), eq(requestContextMock));


        ArgumentCaptor<BudgetExpenseClass> budgetExpenseClassArgumentCaptor = ArgumentCaptor.forClass(BudgetExpenseClass.class);
        verify(restClient)
          .put(assertQueryContains(budgetExpenseClassToBeUpdated.getId()), budgetExpenseClassArgumentCaptor.capture(), eq(requestContextMock));

        BudgetExpenseClass budgetExpenseClass = budgetExpenseClassArgumentCaptor.getValue();

        assertEquals(budgetExpenseClassToBeUpdated, budgetExpenseClass);
        assertThat(budgetExpenseClass, hasProperty("status", is(INACTIVE)));

        vertxTestContext.completeNow();
      });

  }

  @Test
  public void testGetBudgetExpensesClass(VertxTestContext vertxTestContext) {
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
    when(restClient.get(anyString(), eq(BudgetExpenseClassCollection.class), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));

    var future = budgetExpenseClassService.getBudgetExpenseClasses(budgetsIds, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var budgetExpenseClassListFrom = result.result();
        assertEquals(budgetExpenseClass1.getId(), budgetExpenseClassListFrom.get(0).getId());
        assertEquals(budgetExpenseClass2.getId(), budgetExpenseClassListFrom.get(1).getId());
        assertEquals(budgetExpenseClass3.getId(), budgetExpenseClassListFrom.get(2).getId());

        vertxTestContext.completeNow();
      });
  }

  @Test
  public void testGetBudgetExpensesClassByIds(VertxTestContext vertxTestContext) {
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
    when(restClient.get(anyString(), eq(BudgetExpenseClassCollection.class), any())).thenReturn(succeededFuture(budgetExpenseClassCollection));
    var future = budgetExpenseClassService.getBudgetExpenseClassesByIds(budgetsIds, requestContextMock);
    //Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var budgetExpenseClassListReceived = result.result();
        assertEquals(budgetExpenseClass1.getId(), budgetExpenseClassListReceived.get(0).getId());
        vertxTestContext.completeNow();
      });
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
