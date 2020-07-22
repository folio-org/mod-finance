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

import org.folio.dao.BudgetExpenseClassDAO;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.impl.EventLoopContext;

public class BudgetExpenseClassServiceTest {

  @InjectMocks
  private BudgetExpenseClassService budgetExpenseClassService;

  @Mock
  private BudgetExpenseClassDAO budgetExpenseClassDAOMock;

  @Mock
  private Map<String, String> okapiHeadersMock;

  @Mock
  private EventLoopContext ctxMock;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void getBudgetExpenseClasses() {

    String budgetId = UUID.randomUUID().toString();
    List<BudgetExpenseClass> expectedBudgetExpenseClasses = Collections.singletonList(new BudgetExpenseClass()
      .withBudgetId(budgetId)
      .withId(UUID.randomUUID().toString()));

    BudgetExpenseClassCollection budgetExpenseClassCollection = new BudgetExpenseClassCollection()
      .withBudgetExpenseClasses(expectedBudgetExpenseClasses)
      .withTotalRecords(1);

    when(budgetExpenseClassDAOMock.get(anyString(), anyInt(), anyInt(), any(), anyMap()))
      .thenReturn(CompletableFuture.completedFuture(budgetExpenseClassCollection));

    CompletableFuture<List<BudgetExpenseClass>> resultFuture = budgetExpenseClassService.getBudgetExpenseClasses(budgetId, ctxMock, okapiHeadersMock);

    String expectedQuery =  String.format("budgetId==%s", budgetId);
    verify(budgetExpenseClassDAOMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(ctxMock), eq(okapiHeadersMock));

    List<BudgetExpenseClass> resultBudgetExpenseClasses = resultFuture.join();
    assertEquals(expectedBudgetExpenseClasses, resultBudgetExpenseClasses);

  }
}
