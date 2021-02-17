package org.folio.services;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ALLOCATION;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.TRANSFER;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEAR_NOT_FOUND;
import static org.folio.services.ledger.LedgerTotalsService.LEDGER_ID_AND_FISCAL_YEAR_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.ledger.LedgerTotalsService;
import org.folio.services.transactions.BaseTransactionService;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LedgerTotalsServiceTest {

  @InjectMocks
  private LedgerTotalsService ledgerTotalsService;

  @Mock
  private FiscalYearService fiscalYearMockService;

  @Mock
  private BudgetService budgetMockService;

  @Mock
  private BaseTransactionService baseTransactionService;

  @Mock
  private RequestContext requestContextMock;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldCalculateFromRelatedBudgetsAndPopulateLedgerTotalsWhenCallPopulateLedgerTotals() {
    String fiscalYearId = UUID.randomUUID().toString();
    String fundId1 = UUID.randomUUID().toString();
    String fundId2 = UUID.randomUUID().toString();
    FiscalYear fiscalYear = new FiscalYear().withCurrency("BYN").withId(fiscalYearId);

    Budget budget1 = new Budget()
      .withFiscalYearId(fiscalYearId)
      .withFundId(fundId1)
      .withInitialAllocation(400.01)
      .withAllocated(100.01)
      .withAvailable(120d)
      .withUnavailable(0.01)
      .withEncumbered(0.01d)
      .withAwaitingPayment(0d)
      .withExpenditures(0d)
      .withTotalFunding(120.01)
      .withCashBalance(120.01)
      .withOverEncumbrance(0d)
      .withOverExpended(0d);

    Budget budget2 = new Budget()
      .withFiscalYearId(fiscalYearId)
      .withFundId(fundId2)
      .withAllocated(300d)
      .withAvailable(120.97)
      .withUnavailable(160d)
      .withEncumbered(40d)
      .withAwaitingPayment(20d)
      .withExpenditures(100d)
      .withTotalFunding(280.97)
      .withCashBalance(180.97)
      .withOverEncumbrance(0d)
      .withOverExpended(0d);

    Budget budget3 = new Budget()
      .withFiscalYearId(fiscalYearId)
      .withFundId(fundId1)
      .withInitialAllocation(200d)
      .withAllocated(0d)
      .withAvailable(120.55)
      .withUnavailable(0d)
      .withEncumbered(0d)
      .withAwaitingPayment(0d)
      .withExpenditures(0d)
      .withTotalFunding(120.55)
      .withCashBalance(120.55)
      .withOverEncumbrance(0d)
      .withOverExpended(0d);

    List<Budget> budgets = Arrays.asList(budget1, budget2, budget3);
    BudgetsCollection budgetsCollection = new BudgetsCollection().withBudgets(budgets).withTotalRecords(3);

    Ledger ledger = new Ledger().withId(UUID.randomUUID().toString());
    Transaction allocToTr1 = new Transaction().withMetadata(new Metadata().withCreatedDate(new Date()))
                                              .withTransactionType(ALLOCATION).withToFundId(fundId1).withAmount(200d);
    Transaction allocToTr2 = new Transaction().withMetadata(new Metadata().withCreatedDate(new Date()))
                                              .withTransactionType(ALLOCATION).withToFundId(fundId1).withAmount(200d);
    List<Transaction> allocToTrs = new ArrayList<>();
    allocToTrs.add(allocToTr1);
    allocToTrs.add(allocToTr2);
    Transaction allocFromTr = new Transaction().withTransactionType(ALLOCATION).withFromFundId(fundId1).withAmount(200d);
    Transaction tranToTr = new Transaction().withTransactionType(TRANSFER).withToFundId(fundId1).withAmount(141.52d);
    Transaction tranFromTr = new Transaction().withTransactionType(TRANSFER).withFromFundId(fundId1).withAmount(20d);


    when(fiscalYearMockService.getFiscalYearById(anyString(), any())).thenReturn(CompletableFuture.completedFuture(fiscalYear));
    when(budgetMockService.getBudgets(anyString(), anyInt(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(budgetsCollection));
    when(baseTransactionService.retrieveToTransactions(anyList(), eq(fiscalYearId), eq(ALLOCATION), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(allocToTrs));
    when(baseTransactionService.retrieveFromTransactions(anyList(), eq(fiscalYearId), eq(ALLOCATION), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Arrays.asList(allocFromTr)));
    when(baseTransactionService.retrieveToTransactions(anyList(), eq(fiscalYearId), eq(TRANSFER), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Arrays.asList(tranToTr)));
    when(baseTransactionService.retrieveFromTransactions(anyList(), eq(fiscalYearId), eq(TRANSFER), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Arrays.asList(tranFromTr)));

    Ledger resultLedger = ledgerTotalsService.populateLedgerTotals(ledger, fiscalYearId, requestContextMock).join();

    double expectedInitialAllocation = 600.01;
    double expectedAllocated = 600.01;
    double expectedAvailable = 561.52;
    double expectedUnavailable = 160.01;
    double expectedNetTransfers = 121.52;
    double expectedAllocationTo = 200d;
    double expectedAllocationFrom = 200d;
    double expectedEncumbered = 40.01;
    double expectedExpenditures = 100d;
    double expectedAwaitingPayment = 20d;
    double expectedTotalFunding = 721.53;
    double expectedCashBalance = 621.53;

    assertEquals(ledger.getId(), resultLedger.getId());
    assertEquals(expectedAllocated, resultLedger.getAllocated());
    assertEquals(expectedAvailable, resultLedger.getAvailable());
    assertEquals(expectedUnavailable, resultLedger.getUnavailable());
    assertEquals(expectedNetTransfers, resultLedger.getNetTransfers());
    assertEquals(expectedInitialAllocation, resultLedger.getInitialAllocation());
    assertEquals(expectedAllocationTo, resultLedger.getAllocationTo());
    assertEquals(expectedAllocationFrom, resultLedger.getAllocationFrom());
    assertEquals(expectedEncumbered, resultLedger.getEncumbered());
    assertEquals(expectedExpenditures, resultLedger.getExpenditures());
    assertEquals(expectedAwaitingPayment, resultLedger.getAwaitingPayment());
    assertEquals(expectedTotalFunding, resultLedger.getTotalFunding());
    assertEquals(expectedCashBalance, resultLedger.getCashBalance());
    assertEquals(0d, resultLedger.getOverEncumbrance());
    assertEquals(0d, resultLedger.getOverExpended());

    verify(fiscalYearMockService).getFiscalYearById(eq(fiscalYearId), eq(requestContextMock));
    String expectedQuery = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledger.getId(), fiscalYearId);
    verify(budgetMockService).getBudgets(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock));
  }

  @Test
  void shouldPopulateLedgerZeroTotalsIfNoBudgetsWhenCallPopulateLedgerTotals() {
    String fiscalYearId = UUID.randomUUID().toString();

    FiscalYear fiscalYear = new FiscalYear()
      .withCurrency("BYN")
      .withId(fiscalYearId);

    BudgetsCollection budgetsCollection = new BudgetsCollection()
      .withTotalRecords(0);

    Ledger ledger = new Ledger()
      .withId(UUID.randomUUID().toString());


    when(fiscalYearMockService.getFiscalYearById(anyString(), any())).thenReturn(CompletableFuture.completedFuture(fiscalYear));
    when(budgetMockService.getBudgets(anyString(), anyInt(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(budgetsCollection));
    when(fiscalYearMockService.getFiscalYearById(anyString(), any())).thenReturn(CompletableFuture.completedFuture(fiscalYear));
    when(budgetMockService.getBudgets(anyString(), anyInt(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(budgetsCollection));
    when(baseTransactionService.retrieveToTransactions(anyList(), eq(fiscalYearId), eq(ALLOCATION), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Collections.EMPTY_LIST));
    when(baseTransactionService.retrieveFromTransactions(anyList(), eq(fiscalYearId), eq(ALLOCATION), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Collections.EMPTY_LIST));
    when(baseTransactionService.retrieveToTransactions(anyList(), eq(fiscalYearId), eq(TRANSFER), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Collections.EMPTY_LIST));
    when(baseTransactionService.retrieveFromTransactions(anyList(), eq(fiscalYearId), eq(TRANSFER), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Collections.EMPTY_LIST));

    Ledger resultLedger = ledgerTotalsService.populateLedgerTotals(ledger, fiscalYearId, requestContextMock).join();

    assertEquals(ledger.getId(), resultLedger.getId());
    assertEquals(0d, resultLedger.getAllocated());
    assertEquals(0d, resultLedger.getAvailable());
    assertEquals(0d, resultLedger.getUnavailable());
    assertEquals(0d, resultLedger.getNetTransfers());

    verify(fiscalYearMockService).getFiscalYearById(eq(fiscalYearId), eq(requestContextMock));
    String expectedQuery = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledger.getId(), fiscalYearId);
    verify(budgetMockService).getBudgets(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock));
  }

  @Test
  void shouldRetrieveBudgetsForEveryLedgerWhenCallPopulateLedgersTotals() {
    String fiscalYearId = UUID.randomUUID().toString();

    FiscalYear fiscalYear = new FiscalYear()
      .withCurrency("BYN")
      .withId(fiscalYearId);

    BudgetsCollection budgetsCollection = new BudgetsCollection()
      .withTotalRecords(3);

    Ledger ledger1 = new Ledger()
      .withId(UUID.randomUUID().toString());
    Ledger ledger2 = new Ledger()
      .withId(UUID.randomUUID().toString());
    Ledger ledger3 = new Ledger()
      .withId(UUID.randomUUID().toString());
    List<Ledger> ledgers = Arrays.asList(ledger1, ledger2, ledger3);
    LedgersCollection ledgersCollection = new LedgersCollection()
      .withTotalRecords(3)
      .withLedgers(ledgers);


    when(fiscalYearMockService.getFiscalYearById(anyString(), any())).thenReturn(CompletableFuture.completedFuture(fiscalYear));
    when(budgetMockService.getBudgets(anyString(), anyInt(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(budgetsCollection));
    when(fiscalYearMockService.getFiscalYearById(anyString(), any())).thenReturn(CompletableFuture.completedFuture(fiscalYear));
    when(budgetMockService.getBudgets(anyString(), anyInt(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(budgetsCollection));
    when(baseTransactionService.retrieveToTransactions(anyList(), eq(fiscalYearId), eq(ALLOCATION), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Collections.EMPTY_LIST));
    when(baseTransactionService.retrieveFromTransactions(anyList(), eq(fiscalYearId), eq(ALLOCATION), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Collections.EMPTY_LIST));
    when(baseTransactionService.retrieveToTransactions(anyList(), eq(fiscalYearId), eq(TRANSFER), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Collections.EMPTY_LIST));
    when(baseTransactionService.retrieveFromTransactions(anyList(), eq(fiscalYearId), eq(TRANSFER), eq(requestContextMock)))
            .thenReturn(CompletableFuture.completedFuture(Collections.EMPTY_LIST));

    ledgerTotalsService.populateLedgersTotals(ledgersCollection, fiscalYearId, requestContextMock).join();
    verify(fiscalYearMockService).getFiscalYearById(eq(fiscalYearId), eq(requestContextMock));
    String expectedQuery1 = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledger1.getId(), fiscalYearId);
    String expectedQuery2 = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledger2.getId(), fiscalYearId);
    String expectedQuery3 = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledger3.getId(), fiscalYearId);
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(budgetMockService, times(3)).getBudgets(argumentCaptor.capture(), eq(0), eq(Integer.MAX_VALUE), eq(requestContextMock));

    List<String> args = argumentCaptor.getAllValues();

    assertThat(args, containsInAnyOrder(expectedQuery1, expectedQuery2, expectedQuery3));
  }

  @Test
  void shouldReturnResponseWith400StatusWhenFiscalYearNotFound() {
    String fiscalYearId = UUID.randomUUID().toString();
    CompletableFuture<FiscalYear> errorFuture = new CompletableFuture<>();
    CompletionException completionException = new CompletionException(new HttpException(404, NOT_FOUND.getReasonPhrase()));
    errorFuture.completeExceptionally(completionException);

    Ledger ledger = new Ledger()
      .withId(UUID.randomUUID().toString());

    when(fiscalYearMockService.getFiscalYearById(anyString(), any())).thenReturn(errorFuture);

    CompletableFuture<Ledger> resultFuture = ledgerTotalsService.populateLedgerTotals(ledger, fiscalYearId, requestContextMock);
    ExecutionException executionException = assertThrows(ExecutionException.class, resultFuture::get);

    assertThat(executionException.getCause(), IsInstanceOf.instanceOf(HttpException.class));

    HttpException httpException = (HttpException) executionException.getCause();
    assertEquals(400, httpException.getCode());
    assertEquals(FISCAL_YEAR_NOT_FOUND.toError(), httpException.getErrors().getErrors().get(0));

    verify(fiscalYearMockService).getFiscalYearById(eq(fiscalYearId), eq(requestContextMock));
   // verify(budgetMockService, never()).getBudgets(any(), any(), any(), any());
  }

}
