package org.folio.services;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.junit5.VertxExtension;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.folio.services.transactions.CommonTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.junit5.VertxTestContext;


@ExtendWith(VertxExtension.class)
public class CommonTransactionServiceTest {

  private CommonTransactionService transactionService;

  @Mock(name = "restClient")
  private RestClient restClient;

  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    transactionService = new CommonTransactionService(restClient);
  }

  @Test
  void getTransactionsByBudgetId(VertxTestContext vertxTestContext) {
    String fundId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    Budget budget = new Budget().withFundId(fundId).withFiscalYearId(fiscalYearId);

    List<Transaction> transactions = Collections.singletonList(new Transaction().withId(UUID.randomUUID().toString()));
    TransactionCollection transactionCollection = new TransactionCollection().withTransactions(transactions).withTotalRecords(1);

    when(restClient.get(anyString(), any(), eq(requestContext)))
      .thenReturn(succeededFuture(transactionCollection));

    Future<List<Transaction>> future = transactionService.retrieveTransactions(budget, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertEquals(transactions, result.result());
        String expectedQuery = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s", fundId, fundId, fiscalYearId);
        verify(restClient).get(ArgumentMatchers.contains(expectedQuery), eq(TransactionCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });

  }

  @Test
  void getTransactionsByExpenseClasses(VertxTestContext vertxTestContext) {
    String fundId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    SharedBudget budget = new SharedBudget().withFundId(fundId).withFiscalYearId(fiscalYearId);

    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withStatus(BudgetExpenseClass.Status.ACTIVE)
      .withExpenseClassId(UUID.randomUUID().toString())
      .withBudgetId(budget.getId());

    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withStatus(BudgetExpenseClass.Status.ACTIVE)
      .withExpenseClassId(UUID.randomUUID().toString())
      .withBudgetId(budget.getId());

    List<Transaction> transactions = Collections.singletonList(new Transaction().withId(UUID.randomUUID().toString()));
    TransactionCollection transactionCollection = new TransactionCollection().withTransactions(transactions).withTotalRecords(1);

    when(restClient.get(anyString(), any(), eq(requestContext)))
      .thenReturn(succeededFuture(transactionCollection));

    Future<List<Transaction>> future = transactionService.retrieveTransactions(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2), budget, requestContext);

    String expectedQuery = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s AND expenseClassId==(%s or %s)",
      fundId, fundId, fiscalYearId,
      budgetExpenseClass1.getExpenseClassId(),
      budgetExpenseClass2.getExpenseClassId());

    verify(restClient).get(ArgumentMatchers.contains(expectedQuery), eq(TransactionCollection.class), eq(requestContext));

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertEquals(transactions, result.result());
        vertxTestContext.completeNow();
      });

  }

  @Test
  void createAllocationTransaction(VertxTestContext vertxTestContext) {
    String fundId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    Budget budget = new Budget()
      .withAllocated(11.22)
      .withFundId(fundId)
      .withFiscalYearId(fiscalYearId);
    FiscalYear fiscalYear = new FiscalYear()
      .withId(fiscalYearId)
      .withCurrency("BYN");

    Transaction transaction = new Transaction()
      .withId(UUID.randomUUID().toString())
      .withToFundId(budget.getFundId())
      .withFiscalYearId(budget.getFiscalYearId())
      .withCurrency(fiscalYear.getCurrency())
      .withAmount(budget.getAllocated())
      .withTransactionType(Transaction.TransactionType.ALLOCATION)
      .withSource(Transaction.Source.USER);

    when(restClient.post(eq(resourcesPath(TRANSACTIONS)), any(), eq(Transaction.class), eq(requestContext)))
      .thenReturn(succeededFuture(transaction));
    when(restClient.get(eq(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYear.getId())), any(), any())).thenReturn(succeededFuture(fiscalYear));

    Future<Transaction> future = transactionService.createAllocationTransaction(budget, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        verify(restClient).post(eq(resourcesPath(TRANSACTIONS)), refEq(transaction, "id"), eq(Transaction.class), eq(requestContext));
        assertEquals(transaction, result.result());
        assertEquals(result.result().getCurrency(), fiscalYear.getCurrency());

        verify(restClient).get(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYearId), FiscalYear.class, requestContext);
        vertxTestContext.completeNow();
      });

  }

  @Test
  void getTransactionsByFundIdsInChunks(VertxTestContext vertxTestContext) {
    String fiscalYearId = UUID.randomUUID().toString();

    List<String> fundIds = Stream.generate(() -> UUID.randomUUID().toString())
      .limit(40)
      .collect(Collectors.toList());

    List<Transaction> transactions = Collections.singletonList(new Transaction()
      .withId(UUID.randomUUID().toString()));

    TransactionCollection transactionCollection = new TransactionCollection()
      .withTransactions(transactions)
      .withTotalRecords(1);

    when(restClient.get(anyString(), any(), any()))
      .thenReturn(succeededFuture(transactionCollection), succeededFuture(new TransactionCollection()), succeededFuture(new TransactionCollection()));

    Future<List<Transaction>> future = transactionService.retrieveTransactionsByFundIds(fundIds, fiscalYearId, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertThat(result.result(), hasSize(1));
        verify(restClient, times(3)).get(anyString(), any(), any());

        vertxTestContext.completeNow();
      });

  }
}
