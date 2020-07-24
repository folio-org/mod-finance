package org.folio.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.dao.BudgetDAO;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotal;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.impl.EventLoopContext;

public class BudgetExpenseClassTotalsServiceTest {

  @InjectMocks
  private BudgetExpenseClassTotalsService budgetExpenseClassTotalsService;

  @Mock
  private BudgetDAO budgetDAOMock;

  @Mock
  private ExpenseClassService expenseClassServiceMock;

  @Mock
  private TransactionService transactionServiceMock;

  @Mock
  private BudgetExpenseClassService budgetExpenseClassServiceMock;

  @Mock
  private Map<String, String> okapiHeadersMock;

  @Mock
  private EventLoopContext ctxMock;

  private Budget budget;
  private ExpenseClass expenseClass1;
  private ExpenseClass expenseClass2;
  private BudgetExpenseClass budgetExpenseClass1;
  private BudgetExpenseClass budgetExpenseClass2;

  private Transaction buildTransaction(double amount, Transaction.TransactionType type, String expenseClassId) {
    return new Transaction()
      .withAmount(amount)
      .withTransactionType(type)
      .withExpenseClassId(expenseClassId)
      .withCurrency("USD");
  }

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    budget = new Budget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());

    expenseClass1 = new ExpenseClass().withName("EC1").withId(UUID.randomUUID().toString());
    expenseClass2 = new ExpenseClass().withName("EC2").withId(UUID.randomUUID().toString());

    budgetExpenseClass1 = new BudgetExpenseClass()
      .withBudgetId(budget.getId())
      .withExpenseClassId(expenseClass1.getId())
      .withStatus(BudgetExpenseClass.Status.ACTIVE);

    budgetExpenseClass2 = new BudgetExpenseClass()
    .withBudgetId(budget.getId())
    .withExpenseClassId(expenseClass2.getId())
    .withStatus(BudgetExpenseClass.Status.INACTIVE);
  }

  @Test
  void getExpenseClassTotalsComplexPositiveTest() {

    Transaction encumbrance1 = buildTransaction(7.5, Transaction.TransactionType.ENCUMBRANCE, expenseClass1.getId());
    Transaction encumbrance2 = buildTransaction(3.33, Transaction.TransactionType.ENCUMBRANCE, expenseClass1.getId());
    Transaction encumbrance3 = buildTransaction(100d, Transaction.TransactionType.ENCUMBRANCE, null);

    Transaction pendingPayment1 = buildTransaction(5, Transaction.TransactionType.PENDING_PAYMENT, expenseClass1.getId());
    Transaction pendingPayment2 = buildTransaction(4.44, Transaction.TransactionType.PENDING_PAYMENT, expenseClass1.getId());
    Transaction pendingPayment3 = buildTransaction(1500d, Transaction.TransactionType.PENDING_PAYMENT, null);

    Transaction payment1 = buildTransaction(11d, Transaction.TransactionType.PAYMENT, expenseClass2.getId());
    Transaction credit = buildTransaction(4.44, Transaction.TransactionType.CREDIT, expenseClass2.getId());
    Transaction credit2 = buildTransaction(5.56d, Transaction.TransactionType.CREDIT, expenseClass2.getId());
    Transaction payment2 = buildTransaction(15d, Transaction.TransactionType.PAYMENT, null);

    budget.withExpenditures(15d).withOverExpended(1d); // 15 + 1 = 11 + 15 - 4.44 - 5.56

    List<ExpenseClass> expenseClasses = Arrays.asList(expenseClass1, expenseClass2);
    List<BudgetExpenseClass> budgetExpenseClasses = Arrays.asList(budgetExpenseClass1, budgetExpenseClass2);
    List<Transaction> transactions = Arrays.asList(encumbrance1,
      encumbrance2, encumbrance3, pendingPayment1, pendingPayment2,
      pendingPayment3, payment1, payment2, credit, credit2);

    when(budgetDAOMock.getById(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(budget));
    when(expenseClassServiceMock.getExpenseClassesByBudgetId(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(expenseClasses));
    when(transactionServiceMock.getTransactions(any(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(transactions));
    when(budgetExpenseClassServiceMock.getBudgetExpenseClasses(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClasses));

    CompletableFuture<BudgetExpenseClassTotalsCollection> resultFuture = budgetExpenseClassTotalsService.getExpenseClassTotals(budget.getId(), ctxMock, okapiHeadersMock);

    verify(budgetDAOMock).getById(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));
    verify(expenseClassServiceMock).getExpenseClassesByBudgetId(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));
    verify(transactionServiceMock).getTransactions(eq(budget), eq(ctxMock), eq(okapiHeadersMock));
    verify(budgetExpenseClassServiceMock).getBudgetExpenseClasses(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));

    BudgetExpenseClassTotalsCollection budgetExpenseClassTotalsCollection = resultFuture.join();

    assertEquals(2, budgetExpenseClassTotalsCollection.getTotalRecords());
    assertEquals(2, budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals().size());

    BudgetExpenseClassTotal expenseClassTotal1 = budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals()
      .stream().filter(budgetExpenseClassTotal -> budgetExpenseClassTotal.getId().equals(expenseClass1.getId())).findFirst().get();

    assertNotNull(expenseClassTotal1);
    assertEquals(budgetExpenseClass1.getStatus().value(), expenseClassTotal1.getExpenseClassStatus().value());
    assertEquals(expenseClass1.getName(), expenseClassTotal1.getExpenseClassName());
    assertEquals(10.83, expenseClassTotal1.getEncumbered()); // 7.5 + 3.33
    assertEquals(9.44, expenseClassTotal1.getAwaitingPayment()); // 5 + 4.44
    assertEquals(0, expenseClassTotal1.getExpended());
    assertEquals(0, expenseClassTotal1.getPercentageExpended());

    BudgetExpenseClassTotal expenseClassTotal2 = budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals()
      .stream().filter(budgetExpenseClassTotal -> budgetExpenseClassTotal.getId().equals(expenseClass2.getId())).findFirst().get();

    assertNotNull(expenseClassTotal2);
    assertEquals(budgetExpenseClass2.getStatus().value(), expenseClassTotal2.getExpenseClassStatus().value());
    assertEquals(expenseClass2.getName(), expenseClassTotal2.getExpenseClassName());
    assertEquals(0d, expenseClassTotal2.getEncumbered());
    assertEquals(0d, expenseClassTotal2.getAwaitingPayment());
    assertEquals(1d, expenseClassTotal2.getExpended()); //11 - 4.44 - 5.56
    assertEquals(6.25, expenseClassTotal2.getPercentageExpended()); // (1 / 16) * 100

  }

  @Test
  void getExpenseClassTotalsWhenBudgetExpendedTotalIsZeroPercentageExpendedMustBeNull() {
    budget.withExpenditures(0d).withOverExpended(0d);
    Transaction payment1 = buildTransaction(11d, Transaction.TransactionType.PAYMENT, expenseClass1.getId());
    Transaction credit = buildTransaction(11d, Transaction.TransactionType.CREDIT, null);
    List<ExpenseClass> expenseClasses = Collections.singletonList(expenseClass1);
    List<BudgetExpenseClass> budgetExpenseClasses = Collections.singletonList(budgetExpenseClass1);
    List<Transaction> transactions = Arrays.asList(payment1, credit);

    when(budgetDAOMock.getById(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(budget));
    when(expenseClassServiceMock.getExpenseClassesByBudgetId(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(expenseClasses));
    when(transactionServiceMock.getTransactions(any(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(transactions));
    when(budgetExpenseClassServiceMock.getBudgetExpenseClasses(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClasses));

    CompletableFuture<BudgetExpenseClassTotalsCollection> resultFuture = budgetExpenseClassTotalsService.getExpenseClassTotals(budget.getId(), ctxMock, okapiHeadersMock);

    verify(budgetDAOMock).getById(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));
    verify(expenseClassServiceMock).getExpenseClassesByBudgetId(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));
    verify(transactionServiceMock).getTransactions(eq(budget), eq(ctxMock), eq(okapiHeadersMock));
    verify(budgetExpenseClassServiceMock).getBudgetExpenseClasses(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));

    BudgetExpenseClassTotalsCollection budgetExpenseClassTotalsCollection = resultFuture.join();

    assertEquals(1, budgetExpenseClassTotalsCollection.getTotalRecords());
    assertEquals(1, budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals().size());

    BudgetExpenseClassTotal expenseClassTotal = budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals().get(0);

    assertNotNull(expenseClassTotal);
    assertEquals(budgetExpenseClass1.getStatus().value(), expenseClassTotal.getExpenseClassStatus().value());
    assertEquals(expenseClass1.getName(), expenseClassTotal.getExpenseClassName());
    assertEquals(0, expenseClassTotal.getEncumbered());
    assertEquals(0, expenseClassTotal.getAwaitingPayment());
    assertEquals(11d, expenseClassTotal.getExpended());
    assertNull(expenseClassTotal.getPercentageExpended());
  }

  @Test
  void getExpenseClassTotalsWithoutLinkedExpenseClasses() {

    Transaction credit = buildTransaction(11d, Transaction.TransactionType.CREDIT, null);

    when(budgetDAOMock.getById(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(budget));
    when(expenseClassServiceMock.getExpenseClassesByBudgetId(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(transactionServiceMock.getTransactions(any(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(Collections.singletonList(credit)));
    when(budgetExpenseClassServiceMock.getBudgetExpenseClasses(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

    CompletableFuture<BudgetExpenseClassTotalsCollection> resultFuture = budgetExpenseClassTotalsService.getExpenseClassTotals(budget.getId(), ctxMock, okapiHeadersMock);

    verify(budgetDAOMock).getById(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));
    verify(expenseClassServiceMock).getExpenseClassesByBudgetId(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));
    verify(transactionServiceMock).getTransactions(eq(budget), eq(ctxMock), eq(okapiHeadersMock));
    verify(budgetExpenseClassServiceMock).getBudgetExpenseClasses(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));

    BudgetExpenseClassTotalsCollection budgetExpenseClassTotalsCollection = resultFuture.join();

    assertEquals(0, budgetExpenseClassTotalsCollection.getTotalRecords());
    assertEquals(0, budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals().size());

  }

  @Test
  void getExpenseClassTotalsWithoutTransactions() {

    List<ExpenseClass> expenseClasses = Collections.singletonList(expenseClass1);
    List<BudgetExpenseClass> budgetExpenseClasses = Collections.singletonList(budgetExpenseClass1);
    when(budgetDAOMock.getById(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(budget));
    when(expenseClassServiceMock.getExpenseClassesByBudgetId(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(expenseClasses));
    when(transactionServiceMock.getTransactions(any(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(budgetExpenseClassServiceMock.getBudgetExpenseClasses(anyString(), any(), anyMap())).thenReturn(CompletableFuture.completedFuture(budgetExpenseClasses));

    CompletableFuture<BudgetExpenseClassTotalsCollection> resultFuture = budgetExpenseClassTotalsService.getExpenseClassTotals(budget.getId(), ctxMock, okapiHeadersMock);

    verify(budgetDAOMock).getById(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));
    verify(expenseClassServiceMock).getExpenseClassesByBudgetId(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));
    verify(transactionServiceMock).getTransactions(eq(budget), eq(ctxMock), eq(okapiHeadersMock));
    verify(budgetExpenseClassServiceMock).getBudgetExpenseClasses(eq(budget.getId()), eq(ctxMock), eq(okapiHeadersMock));

    BudgetExpenseClassTotalsCollection budgetExpenseClassTotalsCollection = resultFuture.join();

    assertEquals(1, budgetExpenseClassTotalsCollection.getTotalRecords());
    assertEquals(1, budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals().size());

    BudgetExpenseClassTotal expenseClassTotal = budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals().get(0);

    assertNotNull(expenseClassTotal);
    assertEquals(budgetExpenseClass1.getStatus().value(), expenseClassTotal.getExpenseClassStatus().value());
    assertEquals(expenseClass1.getName(), expenseClassTotal.getExpenseClassName());
    assertEquals(0d, expenseClassTotal.getEncumbered());
    assertEquals(0d, expenseClassTotal.getAwaitingPayment());
    assertEquals(0d, expenseClassTotal.getExpended());
    assertEquals(0d, expenseClassTotal.getPercentageExpended());

  }

}
