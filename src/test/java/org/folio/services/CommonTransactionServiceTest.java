package org.folio.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class CommonTransactionServiceTest {

  private CommonTransactionService transactionService;

  @Mock(name = "transactionRestClient")
  private RestClient transactionRestClient;

  @Mock(name = "fiscalYearRestClient")
  private RestClient fiscalYearRestClient;

  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    transactionService = new CommonTransactionService(transactionRestClient, fiscalYearRestClient);
  }

  @Test
  void getTransactionsByBudgetId() {
    String fundId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    Budget budget = new Budget().withFundId(fundId).withFiscalYearId(fiscalYearId);

    List<Transaction> transactions = Collections.singletonList(new Transaction().withId(UUID.randomUUID().toString()));
    TransactionCollection transactionCollection = new TransactionCollection().withTransactions(transactions).withTotalRecords(1);

    when(transactionRestClient.get(anyString(), anyInt(), anyInt(), eq(requestContext), any()))
      .thenReturn(CompletableFuture.completedFuture(transactionCollection));

    CompletableFuture<List<Transaction>> result = transactionService.retrieveTransactions(budget, requestContext);

    String expectedQuery = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s", fundId, fundId, fiscalYearId);
    verify(transactionRestClient)
      .get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContext), eq(TransactionCollection.class));

    List<Transaction> resultTransactions = result.join();
    assertEquals(transactions, resultTransactions);

  }

  @Test
  void getTransactionsByExpenseClasses() {
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

    when(transactionRestClient.get(anyString(), anyInt(), anyInt(), eq(requestContext), any()))
      .thenReturn(CompletableFuture.completedFuture(transactionCollection));

    CompletableFuture<List<Transaction>> result = transactionService.retrieveTransactions(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2), budget, requestContext);

    String expectedQuery = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s AND expenseClassId==(%s or %s)",
      fundId, fundId, fiscalYearId,
      budgetExpenseClass1.getExpenseClassId(),
      budgetExpenseClass2.getExpenseClassId());

    verify(transactionRestClient)
      .get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContext), eq(TransactionCollection.class));

    List<Transaction> resultTransactions = result.join();
    assertEquals(transactions, resultTransactions);

  }

  @Test
  void createAllocationTransaction() {
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

    when(transactionRestClient.post(any(), eq(requestContext), any()))
      .thenReturn(CompletableFuture.completedFuture(transaction));
    when(fiscalYearRestClient.getById(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(fiscalYear));

    CompletableFuture<Transaction> result = transactionService.createAllocationTransaction(budget, requestContext);

    verify(transactionRestClient)
      .post(refEq(transaction, "id"), eq(requestContext), eq(Transaction.class));


    Transaction resultTransaction = result.join();
    assertEquals(transaction, resultTransaction);
    assertEquals(resultTransaction.getCurrency(), fiscalYear.getCurrency());

    verify(fiscalYearRestClient).getById(eq(fiscalYearId), eq(requestContext), eq(FiscalYear.class));
  }

  @Test
  void getTransactionsByFundIdsInChunks() {
    String fiscalYearId = UUID.randomUUID().toString();

    List<String> fundIds = Stream.generate(() -> UUID.randomUUID().toString())
      .limit(40)
      .collect(Collectors.toList());

    List<Transaction> transactions = Collections.singletonList(new Transaction()
      .withId(UUID.randomUUID().toString()));

    TransactionCollection transactionCollection = new TransactionCollection()
      .withTransactions(transactions)
      .withTotalRecords(1);

    when(transactionRestClient.get(anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(transactionCollection), CompletableFuture.completedFuture(new TransactionCollection()), CompletableFuture.completedFuture(new TransactionCollection()));

    CompletableFuture<List<Transaction>> resultFuture = transactionService.retrieveTransactionsByFundIds(fundIds, fiscalYearId, requestContext);

    List<Transaction> resultTransactions = resultFuture.join();

    assertThat(resultTransactions, hasSize(1));

    verify(transactionRestClient, times(3)).get(anyString(), anyInt(), anyInt(), any(), any());
  }
}
