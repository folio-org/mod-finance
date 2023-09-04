package org.folio.services.group;

import static io.vertx.core.Future.succeededFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotal;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.ExpenseClassService;
import org.folio.services.transactions.CommonTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;


@ExtendWith(VertxExtension.class)
public class GroupExpenseClassTotalsServiceTest {

  public static final String USD_CURRENCY = "USD";
  @InjectMocks
  private GroupExpenseClassTotalsService groupExpenseClassTotalsService;

  @Mock
  private GroupFundFiscalYearService groupFundFiscalYearServiceMock;

  @Mock
  private CommonTransactionService transactionServiceMock;

  @Mock
  private ExpenseClassService expenseClassServiceMock;

  @Mock
  private RequestContext requestContext;

  private String groupId;
  private String fiscalYearId;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    groupId = UUID.randomUUID().toString();
    fiscalYearId = UUID.randomUUID().toString();
  }

  @Test
  void getExpenseClassTotalsEmptyGroupFundFiscalYearResponse(VertxTestContext vertxTestContext) {

    when(groupFundFiscalYearServiceMock.getGroupFundFiscalYearsWithBudgetId(anyString(), anyString(), any()))
      .thenReturn(succeededFuture(Collections.emptyList()));

    var future = groupExpenseClassTotalsService.getExpenseClassTotals(groupId, fiscalYearId, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var groupExpenseClassTotalsCollection = result.result();
        assertEquals(new GroupExpenseClassTotalsCollection().withTotalRecords(0), groupExpenseClassTotalsCollection);
        verify(groupFundFiscalYearServiceMock).getGroupFundFiscalYearsWithBudgetId(eq(groupId), eq(fiscalYearId), eq(requestContext));
        verify(transactionServiceMock, never()).retrieveTransactionsByFundIds(anyList(), anyString(), any());
        verify(expenseClassServiceMock, never()).getExpenseClassesByBudgetIds(anyList(), any());

        vertxTestContext.completeNow();
      });

  }

  @Test
  void getExpenseClassTotalsEmptyExpenseClassesResponse(VertxTestContext vertxTestContext) {

    GroupFundFiscalYear groupFundFiscalYear = new GroupFundFiscalYear()
      .withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withFundId(UUID.randomUUID().toString())
      .withBudgetId(UUID.randomUUID().toString());

    Transaction transaction = new Transaction()
      .withTransactionType(Transaction.TransactionType.PAYMENT)
      .withAmount(100d)
      .withFiscalYearId(fiscalYearId)
      .withFromFundId(groupFundFiscalYear.getFundId())
      .withCurrency(USD_CURRENCY);

    when(groupFundFiscalYearServiceMock.getGroupFundFiscalYearsWithBudgetId(anyString(), anyString(), any()))
      .thenReturn(succeededFuture(Collections.singletonList(groupFundFiscalYear)));
    when(transactionServiceMock.retrieveTransactionsByFundIds(anyList(), anyString(), any()))
      .thenReturn(succeededFuture(Collections.singletonList(transaction)));
    when(expenseClassServiceMock.getExpenseClassesByBudgetIds(anyList(), any()))
      .thenReturn(succeededFuture(Collections.emptyList()));

    var future = groupExpenseClassTotalsService.getExpenseClassTotals(groupId, fiscalYearId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var groupExpenseClassTotalsCollection = result.result();
        assertEquals(new GroupExpenseClassTotalsCollection().withTotalRecords(0), groupExpenseClassTotalsCollection);
        verify(groupFundFiscalYearServiceMock).getGroupFundFiscalYearsWithBudgetId(eq(groupId), eq(fiscalYearId), eq(requestContext));
        verify(transactionServiceMock).retrieveTransactionsByFundIds(eq(Collections.singletonList(groupFundFiscalYear.getFundId())), eq(fiscalYearId), eq(requestContext));
        verify(expenseClassServiceMock).getExpenseClassesByBudgetIds(eq(Collections.singletonList(groupFundFiscalYear.getBudgetId())), eq(requestContext));

        vertxTestContext.completeNow();
      });
  }

  @Test
  void getExpenseClassTotalsEmptyTransactionsResponse(VertxTestContext vertxTestContext) {

    GroupFundFiscalYear groupFundFiscalYear = new GroupFundFiscalYear()
      .withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withFundId(UUID.randomUUID().toString())
      .withBudgetId(UUID.randomUUID().toString());

    ExpenseClass expenseClass = new ExpenseClass()
      .withId(UUID.randomUUID().toString())
      .withName("Test");

    when(groupFundFiscalYearServiceMock.getGroupFundFiscalYearsWithBudgetId(anyString(), anyString(), any()))
      .thenReturn(succeededFuture(Collections.singletonList(groupFundFiscalYear)));
    when(transactionServiceMock.retrieveTransactionsByFundIds(anyList(), anyString(), any()))
      .thenReturn(succeededFuture(Collections.emptyList()));
    when(expenseClassServiceMock.getExpenseClassesByBudgetIds(anyList(), any()))
      .thenReturn(succeededFuture(Collections.singletonList(expenseClass)));

    var future = groupExpenseClassTotalsService.getExpenseClassTotals(groupId, fiscalYearId, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var groupExpenseClassTotalsCollection = result.result();
        assertThat(groupExpenseClassTotalsCollection.getGroupExpenseClassTotals(), hasSize(1));
        GroupExpenseClassTotal groupExpenseClassTotal = groupExpenseClassTotalsCollection.getGroupExpenseClassTotals().get(0);
        assertEquals(expenseClass.getName(), groupExpenseClassTotal.getExpenseClassName());
        assertEquals(0d, groupExpenseClassTotal.getExpended());
        assertEquals(0d, groupExpenseClassTotal.getPercentageExpended());

        verify(groupFundFiscalYearServiceMock).getGroupFundFiscalYearsWithBudgetId(eq(groupId), eq(fiscalYearId), eq(requestContext));
        verify(transactionServiceMock).retrieveTransactionsByFundIds(eq(Collections.singletonList(groupFundFiscalYear.getFundId())), eq(fiscalYearId), eq(requestContext));
        verify(expenseClassServiceMock).getExpenseClassesByBudgetIds(eq(Collections.singletonList(groupFundFiscalYear.getBudgetId())), eq(requestContext));

        vertxTestContext.completeNow();
      });
  }

  @Test
  void getExpenseClassTotalsMultipleRecords(VertxTestContext vertxTestContext) {

    String fundId1 = UUID.randomUUID().toString();
    String fundId2 = UUID.randomUUID().toString();
    String expenseClassId1 = UUID.randomUUID().toString();
    String expenseClassId2 = UUID.randomUUID().toString();
    String budgetId1 = UUID.randomUUID().toString();
    String budgetId2 = UUID.randomUUID().toString();

    GroupFundFiscalYear groupFundFiscalYear1 = new GroupFundFiscalYear()
      .withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withFundId(fundId1)
      .withBudgetId(budgetId1);

    GroupFundFiscalYear groupFundFiscalYear2 = new GroupFundFiscalYear()
      .withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withFundId(fundId2)
      .withBudgetId(budgetId2);

    ExpenseClass expenseClass1 = new ExpenseClass()
      .withId(expenseClassId1)
      .withName("Test");
    ExpenseClass expenseClass2 = new ExpenseClass()
      .withId(expenseClassId2)
      .withName("Test2");

    Transaction payment = new Transaction()
      .withTransactionType(Transaction.TransactionType.PAYMENT)
      .withAmount(100d)
      .withFromFundId(fundId1)
      .withFiscalYearId(fiscalYearId)
      .withExpenseClassId(expenseClassId1)
      .withCurrency(USD_CURRENCY);

    Transaction credit = new Transaction()
      .withTransactionType(Transaction.TransactionType.CREDIT)
      .withAmount(5d)
      .withToFundId(fundId1)
      .withFiscalYearId(fiscalYearId)
      .withExpenseClassId(expenseClassId1)
      .withCurrency(USD_CURRENCY);

    Transaction payment2 = new Transaction()
      .withTransactionType(Transaction.TransactionType.PAYMENT)
      .withAmount(905d)
      .withFromFundId(fundId1)
      .withFiscalYearId(fiscalYearId)
      .withExpenseClassId(expenseClassId2)
      .withCurrency(USD_CURRENCY);

    Transaction encumbranceNoExpenseClass = new Transaction()
      .withTransactionType(Transaction.TransactionType.ENCUMBRANCE)
      .withAmount(131.31)
      .withFromFundId(fundId1)
      .withCurrency(USD_CURRENCY);

    Transaction encumbrance1 = new Transaction()
      .withTransactionType(Transaction.TransactionType.ENCUMBRANCE)
      .withAmount(11.31)
      .withFromFundId(fundId1)
      .withExpenseClassId(expenseClassId1)
      .withCurrency(USD_CURRENCY);

    Transaction encumbrance2 = new Transaction()
      .withTransactionType(Transaction.TransactionType.ENCUMBRANCE)
      .withAmount(41.32)
      .withFromFundId(fundId2)
      .withExpenseClassId(expenseClassId2)
      .withCurrency(USD_CURRENCY);

    Transaction pendingPayment1 = new Transaction()
      .withTransactionType(Transaction.TransactionType.PENDING_PAYMENT)
      .withAmount(1.23)
      .withFromFundId(fundId1)
      .withCurrency(USD_CURRENCY)
      .withExpenseClassId(expenseClassId1);

    Transaction pendingPayment2 = new Transaction()
      .withTransactionType(Transaction.TransactionType.PENDING_PAYMENT)
      .withAmount(1.77)
      .withFromFundId(fundId2)
      .withCurrency(USD_CURRENCY)
      .withExpenseClassId(expenseClassId1);

    List<Transaction> transactions = Arrays.asList(payment, credit, payment2, encumbranceNoExpenseClass, encumbrance1, encumbrance2, pendingPayment1, pendingPayment2);

    when(groupFundFiscalYearServiceMock.getGroupFundFiscalYearsWithBudgetId(anyString(), anyString(), any()))
      .thenReturn(succeededFuture(Arrays.asList(groupFundFiscalYear1, groupFundFiscalYear2)));
    when(transactionServiceMock.retrieveTransactionsByFundIds(anyList(), anyString(), any()))
      .thenReturn(succeededFuture(transactions));
    when(expenseClassServiceMock.getExpenseClassesByBudgetIds(anyList(), any()))
      .thenReturn(succeededFuture(Arrays.asList(expenseClass1, expenseClass2)));

    var future = groupExpenseClassTotalsService.getExpenseClassTotals(groupId, fiscalYearId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var groupExpenseClassTotalsCollection = result.result();
        assertThat(groupExpenseClassTotalsCollection.getGroupExpenseClassTotals(), hasSize(2));
        GroupExpenseClassTotal expected1 = new GroupExpenseClassTotal()
          .withExpenseClassName(expenseClass1.getName())
          .withId(expenseClassId1)
          .withEncumbered(11.31)
          .withAwaitingPayment(3d)
          .withExpended(95d)
          .withPercentageExpended(9.5);

        GroupExpenseClassTotal expected2 = new GroupExpenseClassTotal()
          .withExpenseClassName(expenseClass2.getName())
          .withId(expenseClassId2)
          .withEncumbered(41.32)
          .withAwaitingPayment(0d)
          .withExpended(905d)
          .withPercentageExpended(90.5);

        assertThat(groupExpenseClassTotalsCollection.getGroupExpenseClassTotals(), containsInAnyOrder(expected1, expected2));

        verify(groupFundFiscalYearServiceMock).getGroupFundFiscalYearsWithBudgetId(eq(groupId), eq(fiscalYearId), eq(requestContext));

        List<String> expectedFundIds = new ArrayList<>();
        expectedFundIds.add(fundId1);
        expectedFundIds.add(fundId2);
        verify(transactionServiceMock).retrieveTransactionsByFundIds(eq(expectedFundIds), eq(fiscalYearId), eq(requestContext));

        List<String> expectedBudgetIds = new ArrayList<>();
        expectedBudgetIds.add(budgetId1);
        expectedBudgetIds.add(budgetId2);
        verify(expenseClassServiceMock).getExpenseClassesByBudgetIds(eq(expectedBudgetIds), eq(requestContext));

        vertxTestContext.completeNow();
      });

  }

}
