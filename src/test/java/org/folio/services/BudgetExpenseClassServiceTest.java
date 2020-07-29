package org.folio.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BudgetExpenseClassServiceTest {

  @InjectMocks
  private BudgetExpenseClassService budgetExpenseClassService;

  @Mock
  private RestClient budgetExpenseClassClientMock;

  @Mock
  private Map<String, String> okapiHeadersMock;

  @Mock
  private RequestContext requestContext;

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

    when(budgetExpenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(budgetExpenseClassCollection));

    CompletableFuture<List<BudgetExpenseClass>> resultFuture = budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContext);

    String expectedQuery =  String.format("budgetId==%s", budgetId);
    verify(budgetExpenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContext), eq(BudgetExpenseClassCollection.class));

    List<BudgetExpenseClass> resultBudgetExpenseClasses = resultFuture.join();
    assertEquals(expectedBudgetExpenseClasses, resultBudgetExpenseClasses);

  }
}
