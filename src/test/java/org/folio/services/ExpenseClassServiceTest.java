package org.folio.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import io.vertx.core.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ExpenseClassServiceTest {

  @InjectMocks
  private ExpenseClassService expenseClassService;

  @Mock
  private RestClient expenseClassClientMock;

  @Mock
  private RequestContext requestContext;

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

    when(expenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(succeededFuture(expenseClassCollection));

    Future<List<ExpenseClass>> resultFuture = expenseClassService.getExpenseClassesByBudgetId(budgetId, requestContext);

    String expectedQuery =  String.format("budgetExpenseClass.budgetId==%s", budgetId);
    verify(expenseClassClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContext), eq(ExpenseClassCollection.class));

    List<ExpenseClass> resultBudgetExpenseClasses = resultFuture.join();
    assertEquals(expectedExpenseClasses, resultBudgetExpenseClasses);

  }

  @Test
  void getExpenseClassesByBudgetIdsInChunks() {

    List<String> budgetIds = Stream.generate(() -> UUID.randomUUID().toString())
      .limit(40)
      .collect(Collectors.toList());

    List<ExpenseClass> expectedExpenseClasses = Collections.singletonList(new ExpenseClass()
      .withName("Test name")
      .withId(UUID.randomUUID().toString()));

    ExpenseClassCollection expenseClassCollection = new ExpenseClassCollection()
      .withExpenseClasses(expectedExpenseClasses)
      .withTotalRecords(1);

    when(expenseClassClientMock.get(anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(succeededFuture(expenseClassCollection));

    Future<List<ExpenseClass>> resultFuture = expenseClassService.getExpenseClassesByBudgetIds(budgetIds, requestContext);

    List<ExpenseClass> expenseClasses = resultFuture.join();

    assertThat(expenseClasses, hasSize(1));

    verify(expenseClassClientMock, times(3)).get(anyString(), anyInt(), anyInt(), any(), any());
  }

}
