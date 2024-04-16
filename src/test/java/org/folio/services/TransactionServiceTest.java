package org.folio.services;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ResourcePathResolver.BATCH_TRANSACTIONS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.transactions.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;


@ExtendWith(VertxExtension.class)
public class TransactionServiceTest {

  private TransactionService transactionService;
  private AutoCloseable mockitoMocks;

  @Mock(name = "restClient")
  private RestClient restClient;

  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
    FiscalYearService fiscalYearService = new FiscalYearService(restClient);
    transactionService = new TransactionService(restClient, fiscalYearService);
  }

  @AfterEach
  public void afterEach() throws Exception {
    mockitoMocks.close();
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

    Future<List<Transaction>> future = transactionService.getBudgetTransactions(budget, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertEquals(transactions, result.result());
        String expectedQuery = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s", fundId, fundId, fiscalYearId);
        verify(restClient).get(assertQueryContains(expectedQuery), eq(TransactionCollection.class), eq(requestContext));
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

    Future<List<Transaction>> future = transactionService.getBudgetTransactionsWithExpenseClasses(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2), budget, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        String expectedQuery = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s AND expenseClassId==(%s or %s)",
          fundId, fundId, fiscalYearId,
          budgetExpenseClass1.getExpenseClassId(),
          budgetExpenseClass2.getExpenseClassId());

        verify(restClient).get(assertQueryContains(expectedQuery), eq(TransactionCollection.class), eq(requestContext));
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

    when(restClient.postEmptyResponse(eq(resourcesPath(BATCH_TRANSACTIONS_STORAGE)), any(Batch.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture());
    when(restClient.get(eq(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYear.getId())), any(), any()))
      .thenReturn(succeededFuture(fiscalYear));

    Future<Void> future = transactionService.createAllocationTransaction(budget, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        ArgumentCaptor<Batch> batchCaptor = ArgumentCaptor.forClass(Batch.class);
        verify(restClient).postEmptyResponse(eq(resourcesPath(BATCH_TRANSACTIONS_STORAGE)), batchCaptor.capture(), eq(requestContext));
        Batch batch = batchCaptor.getValue();
        Transaction transaction = batch.getTransactionsToCreate().get(0);
        assertEquals(budget.getFundId(), transaction.getToFundId());
        assertEquals(budget.getFiscalYearId(), transaction.getFiscalYearId());
        assertEquals(fiscalYear.getCurrency(), transaction.getCurrency());
        assertEquals(budget.getAllocated(), transaction.getAmount());
        assertEquals(budget.getAllocated(), transaction.getAmount());
        assertEquals(Transaction.Source.USER, transaction.getSource());

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

    when(restClient.get(anyString(), eq(TransactionCollection.class), any()))
      .thenReturn(succeededFuture(transactionCollection))
      .thenReturn(succeededFuture(new TransactionCollection()))
      .thenReturn(succeededFuture(new TransactionCollection()));

    Future<List<Transaction>> future = transactionService.getTransactionsByFundIds(fundIds, fiscalYearId, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertThat(result.result(), hasSize(1));
        verify(restClient, times(3)).get(anyString(), any(), any());

        vertxTestContext.completeNow();
      });

  }
}
