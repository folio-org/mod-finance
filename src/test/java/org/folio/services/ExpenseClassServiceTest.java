package org.folio.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.dao.ExpenseClassDAO;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.impl.EventLoopContext;

public class ExpenseClassServiceTest {

  @InjectMocks
  private ExpenseClassService expenseClassService;

  @Mock
  private ExpenseClassDAO expenseClassDAOMock;

  @Mock
  private Map<String, String> okapiHeadersMock;

  @Mock
  private EventLoopContext ctxMock;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void getExpenseClassesByBudgetId() {

    String budgetId = UUID.randomUUID().toString();
    List<ExpenseClass> expectedExpenseClasses = Collections.singletonList(new ExpenseClass()
      .withName("Test name")
      .withId(UUID.randomUUID().toString()));

    ExpenseClassCollection expenseClassCollection = new ExpenseClassCollection()
      .withExpenseClasses(expectedExpenseClasses)
      .withTotalRecords(1);

    when(expenseClassDAOMock.get(anyString(), anyInt(), anyInt(), any(), anyMap()))
      .thenReturn(CompletableFuture.completedFuture(expenseClassCollection));

    CompletableFuture<List<ExpenseClass>> resultFuture = expenseClassService.getExpenseClassesByBudgetId(budgetId, ctxMock, okapiHeadersMock);

    String expectedQuery =  String.format("budgetExpenseClass.budgetId==%s", budgetId);
    verify(expenseClassDAOMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(ctxMock), eq(okapiHeadersMock));

    List<ExpenseClass> resultBudgetExpenseClasses = resultFuture.join();
    assertEquals(expectedExpenseClasses, resultBudgetExpenseClasses);

  }

}
