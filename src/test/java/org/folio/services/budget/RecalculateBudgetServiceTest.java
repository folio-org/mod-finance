package org.folio.services.budget;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.BudgetUtils;
import org.folio.services.transactions.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.vertx.core.Future.succeededFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class RecalculateBudgetServiceTest {

  @InjectMocks
  RecalculateBudgetService recalculateBudgetService;

  @Mock
  private BudgetService budgetServiceMock;
  @Mock
  private RequestContext requestContext;
  @Mock
  private TransactionService transactionServiceMock;

  private Budget budget;

  @BeforeEach
  public void beforeEach() {
    budget = new Budget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withOverEncumbrance(0.0d)
      .withOverExpended(0.0d)
      .withFiscalYearId(UUID.randomUUID().toString());
  }

  @Test
  void recalculateBudgetComplexPositiveTest(VertxTestContext vertxTestContext) {
    String budgetFundId = budget.getFundId();
    String anotherFundId = UUID.randomUUID().toString();

    ArgumentCaptor<SharedBudget> budgetCaptor = ArgumentCaptor.forClass(SharedBudget.class);

    Transaction initialAllocation = buildTransaction(null, budgetFundId, 1500d, Transaction.TransactionType.ALLOCATION);
    Transaction increaseAllocation = buildTransaction(null, budgetFundId, 100d, Transaction.TransactionType.ALLOCATION);
    Transaction decreaseAllocation = buildTransaction(budgetFundId, null, 200d, Transaction.TransactionType.ALLOCATION);

    Transaction transferTo = buildTransaction(anotherFundId, budgetFundId, 150d, Transaction.TransactionType.TRANSFER);
    Transaction transferFrom = buildTransaction(budgetFundId, anotherFundId, 100d, Transaction.TransactionType.TRANSFER);

    Transaction rolloverTransfer = buildTransaction(null, budgetFundId, 4d, Transaction.TransactionType.ROLLOVER_TRANSFER);

    Transaction encumbrance = buildTransaction(budgetFundId, null, 120d, Transaction.TransactionType.ENCUMBRANCE);

    Transaction pendingPayment1 = buildTransaction(budgetFundId, null, -20d, Transaction.TransactionType.PENDING_PAYMENT);
    Transaction pendingPayment2 = buildTransaction(budgetFundId, null, 50d, Transaction.TransactionType.PENDING_PAYMENT);

    Transaction credit = buildTransaction(null, budgetFundId, 90d, Transaction.TransactionType.CREDIT);
    Transaction payment = buildTransaction(budgetFundId, null, 60d, Transaction.TransactionType.PAYMENT);

    List<Transaction> transactions = Arrays.asList(initialAllocation,
      increaseAllocation, decreaseAllocation, transferTo, transferFrom, rolloverTransfer,
      encumbrance, pendingPayment1, pendingPayment2, credit, payment);

    when(budgetServiceMock.getBudgetById(anyString(), any())).thenReturn(succeededFuture(BudgetUtils.convertToSharedBudget(budget)));
    when(budgetServiceMock.updateBudgetWithAmountFields(any(), any())).thenReturn(succeededFuture());
    when(transactionServiceMock.getBudgetTransactions(any(), any())).thenReturn(succeededFuture(transactions));

    Future<Void> future = recalculateBudgetService.recalculateBudget(budget.getId(), requestContext);
    vertxTestContext.assertComplete(future)
      .onSuccess(result -> {
        verify(budgetServiceMock).getBudgetById(eq(budget.getId()), eq(requestContext));
        verify(transactionServiceMock).getBudgetTransactions(eq(budget), eq(requestContext));
        verify(budgetServiceMock).updateBudgetWithAmountFields(budgetCaptor.capture(), eq(requestContext));

        SharedBudget capturedBudget = budgetCaptor.getValue();
        assertNotNull(capturedBudget);
        assertEquals(1500d, capturedBudget.getInitialAllocation()); // initialAllocation
        assertEquals(100d, capturedBudget.getAllocationTo()); // increaseAllocation
        assertEquals(200d, capturedBudget.getAllocationFrom()); // decreaseAllocation
        assertEquals(54d, capturedBudget.getNetTransfers()); // transferTo - transferFrom
        assertEquals(120d, capturedBudget.getEncumbered()); // encumbrance
        assertEquals(30d, capturedBudget.getAwaitingPayment()); // pendingPayment1 + pendingPayment2
        assertEquals(60d, capturedBudget.getExpenditures()); // payment
        assertEquals(90d, capturedBudget.getCredits()); // credit

        vertxTestContext.completeNow();
      }).onFailure(vertxTestContext::failNow);
  }

  @Test
  void recalculateBudgetWithoutTransactions(VertxTestContext vertxTestContext) {
    ArgumentCaptor<SharedBudget> budgetCaptor = ArgumentCaptor.forClass(SharedBudget.class);

    List<Transaction> transactions = Collections.emptyList();

    when(budgetServiceMock.getBudgetById(anyString(), any())).thenReturn(succeededFuture(BudgetUtils.convertToSharedBudget(budget)));
    when(budgetServiceMock.updateBudgetWithAmountFields(any(), any())).thenReturn(succeededFuture());
    when(transactionServiceMock.getBudgetTransactions(any(), any())).thenReturn(succeededFuture(transactions));

    Future<Void> future = recalculateBudgetService.recalculateBudget(budget.getId(), requestContext);
    vertxTestContext.assertComplete(future)
      .onSuccess(result -> {
        verify(budgetServiceMock).getBudgetById(eq(budget.getId()), eq(requestContext));
        verify(transactionServiceMock).getBudgetTransactions(eq(budget), eq(requestContext));
        verify(budgetServiceMock).updateBudgetWithAmountFields(budgetCaptor.capture(), eq(requestContext));

        SharedBudget capturedBudget = budgetCaptor.getValue();
        assertNotNull(capturedBudget);
        assertEquals(0d, capturedBudget.getInitialAllocation());
        assertEquals(0d, capturedBudget.getAllocationTo());
        assertEquals(0d, capturedBudget.getAllocationFrom());
        assertEquals(0d, capturedBudget.getNetTransfers());
        assertEquals(0d, capturedBudget.getEncumbered());
        assertEquals(0d, capturedBudget.getAwaitingPayment());
        assertEquals(0d, capturedBudget.getExpenditures());
        assertEquals(0d, capturedBudget.getCredits());

        vertxTestContext.completeNow();
      }).onFailure(vertxTestContext::failNow);
  }

  private Transaction buildTransaction(String fromFundId, String toFundId, double amount, Transaction.TransactionType transactionType) {
    return new Transaction()
      .withId(UUID.randomUUID().toString())
      .withAmount(amount)
      .withFromFundId(fromFundId)
      .withToFundId(toFundId)
      .withTransactionType(transactionType)
      .withCurrency("USD")
      .withMetadata(new Metadata().withCreatedDate(new Date()));
  }

}
