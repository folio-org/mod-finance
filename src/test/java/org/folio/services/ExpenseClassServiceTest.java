package org.folio.services;

import static io.vertx.core.Future.succeededFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;
import org.folio.rest.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class ExpenseClassServiceTest {

  @InjectMocks
  private ExpenseClassService expenseClassService;

  @Mock
  private RestClient restClient;

  @Mock
  private RequestContext requestContext;

  private AutoCloseable closeable;

  @BeforeEach
  public void initMocks() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void closeMocks() throws Exception {
    closeable.close();
  }

  @Test
  void getExpenseClassesByBudgetId(VertxTestContext vertxTestContext) {
    String budgetId = UUID.randomUUID().toString();
    List<ExpenseClass> expectedExpenseClasses = Collections.singletonList(new ExpenseClass()
      .withName("Test name")
      .withId(UUID.randomUUID().toString()));

    ExpenseClassCollection expenseClassCollection = new ExpenseClassCollection()
      .withExpenseClasses(expectedExpenseClasses)
      .withTotalRecords(1);

    when(restClient.get(anyString(), any(), any()))
      .thenReturn(succeededFuture(expenseClassCollection));

    Future<List<ExpenseClass>> future = expenseClassService.getExpenseClassesByBudgetId(budgetId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        String expectedQuery = String.format("budgetExpenseClass.budgetId==%s", budgetId);
        verify(restClient).get(TestUtils.assertQueryContains(expectedQuery), eq(ExpenseClassCollection.class), eq(requestContext));
        assertEquals(expectedExpenseClasses, result.result());

        vertxTestContext.completeNow();
      });
  }

  @Test
  void getExpenseClassesByBudgetIdsInChunks(VertxTestContext vertxTestContext) {
    List<String> budgetIds = Stream.generate(() -> UUID.randomUUID().toString())
      .limit(40)
      .collect(Collectors.toList());

    List<ExpenseClass> expectedExpenseClasses = Collections.singletonList(new ExpenseClass()
      .withName("Test name")
      .withId(UUID.randomUUID().toString()));

    ExpenseClassCollection expenseClassCollection = new ExpenseClassCollection()
      .withExpenseClasses(expectedExpenseClasses)
      .withTotalRecords(1);

    when(restClient.get(anyString(), any(), any()))
      .thenReturn(succeededFuture(expenseClassCollection));

    Future<List<ExpenseClass>> future = expenseClassService.getExpenseClassesByBudgetIds(budgetIds, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertThat(result.result(), hasSize(1));

        verify(restClient, times(3)).get(anyString(), any(), any());

        vertxTestContext.completeNow();
      });
  }
}
