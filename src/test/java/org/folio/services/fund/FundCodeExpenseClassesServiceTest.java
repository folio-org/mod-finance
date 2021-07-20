package org.folio.services.fund;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.*;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class FundCodeExpenseClassesServiceTest {

  @InjectMocks
  private FundCodeExpenseClassesService fundCodeExpenseClassesService;

  @Mock
  private BudgetService budgetService;

  @Mock
  private BudgetExpenseClassService budgetExpenseClassService;

  @Mock
  private FundService fundService;

  @Mock
  private LedgerService ledgerService;

  @Mock
  private FiscalYearService fiscalYearService;

  @Mock
  private LedgerDetailsService ledgerDetailsService;

  @Mock
  private ExpenseClassService expenseClassService;

  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void shouldRetrieveCombinationFundCodeExpClasses() {
    String fiscalYearCode = "FY2021";
    String fiscalYearId = "684b5dc5-92f6-4db7-b996-b549d88f5e4e";

    FiscalYear fiscalYear = new FiscalYear()
      .withId(fiscalYearId)
      .withCode(fiscalYearCode)
      .withName("Fiscal Year 2021")
      .withSeries("FY");
    when(fiscalYearService.getFiscalYearByFiscalYearCode(eq(fiscalYearCode), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(fiscalYear));

    Budget budget1 = new Budget()
      .withId("012e7da4-003c-48ff-9feb-f3745044da35")
      .withFundId("69640328-788e-43fc-9c3c-af39e243f3b7")
      .withFiscalYearId(fiscalYearId)
      .withInitialAllocation(0.03)
      .withAllocationTo(1.04)
      .withAllocationFrom(0.01)
      .withAllocated(1.06)
      .withNetTransfers(1.03)
      .withAwaitingPayment(0.11)
      .withEncumbered(0.47)
      .withExpenditures(0.03)
      .withUnavailable(0.61)
      .withAvailable(1.48)
      .withTotalFunding(2.09)
      .withCashBalance(2.06)
      .withOverEncumbrance(0d)
      .withOverExpended(0d);

    Budget budget2 = new Budget()
      .withId("0916fde2-7a7d-4ff0-9069-37ee1ec0c068")
      .withFundId("bbd4a5e1-c9f3-44b9-bfdf-d184e04f0ba0")
      .withFiscalYearId(fiscalYearId)
      .withInitialAllocation(0.03)
      .withAllocationTo(1.04)
      .withAllocationFrom(0.01)
      .withAllocated(1.06)
      .withNetTransfers(0d)
      .withAwaitingPayment(0.11)
      .withEncumbered(0.47)
      .withExpenditures(1.03)
      .withUnavailable(1.61)
      .withAvailable(0d)
      .withTotalFunding(1.06)
      .withCashBalance(0.03)
      .withOverEncumbrance(0d)
      .withOverExpended(0.55d);

    BudgetsCollection budgetCollection = new BudgetsCollection().withBudgets(Arrays.asList(budget1, budget2));
    when(budgetService.getBudgets(eq(Budget.BudgetStatus.ACTIVE.value()), eq(0), eq(Integer.MAX_VALUE),
      eq(requestContext))).thenReturn(CompletableFuture.completedFuture(budgetCollection));

    Fund fund1 = new Fund()
      .withFundStatus(Fund.FundStatus.ACTIVE)
      .withFundTypeId(UUID.randomUUID().toString())
      .withCode("ENDOW-SUBN")
      .withName("Endowments")
      .withLedgerId("65cb2bf0-d4c2-4886-8ad0-b76f1ba75d61")
      .withDescription("ongoing endowments used for subscriptions")
      .withId("1714f71f-b845-444b-a79e-a577487a6f7d");

    Fund fund2 = new Fund()
      .withFundStatus(Fund.FundStatus.ACTIVE)
      .withFundTypeId(UUID.randomUUID().toString())
      .withCode("GRANT-SUBN")
      .withName("Endowments")
      .withLedgerId("65cb2bf0-d4c2-4886-8ad0-b76f1ba75d63")
      .withDescription("ongoing endowments used for subscriptions")
      .withId("1714f71f-b845-444b-a79e-a577487a6f9d");

    List<Fund> funds = Arrays.asList(fund1, fund2);
    List<String> fundIds = new ArrayList();
    fundIds.add(fund1.getId());
    fundIds.add(fund1.getId());
    when(fundService.getFunds(eq(fundIds), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(funds));

    Ledger ledger1 = new Ledger()
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE)
      .withId("133a7916-f05e-4df4-8f7f-09eb2a7076d1")
      .withCode("ONETIME")
      .withFiscalYearOneId("684b5dc5-92f6-4db7-b996-b549d88f5e4e")
      .withName("One-time")
      .withCurrency("USD");

    Ledger ledger2 = new Ledger()
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE)
      .withId("133a7916-f05e-4df4-8f7f-09eb2a7076d8")
      .withCode("ONETIME")
      .withFiscalYearOneId("684b5dc5-92f6-4db7-b996-b549d88f5e9e")
      .withName("One-time")
      .withCurrency("USD");

    String ledgerId1 = fund1.getLedgerId();
    String ledgerId2 = fund2.getLedgerId();
    List<Ledger> ledgers = Arrays.asList(ledger1, ledger2);
    List<String> ledgerIds = new ArrayList();
    ledgerIds.add(ledgerId1);
    ledgerIds.add(ledgerId2);

    when(ledgerService.getLedgers(eq(ledgerIds), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(ledgers));

    LedgersCollection ledgerCollection = new LedgersCollection();
    ledgerCollection.setLedgers(ledgers);
    when(ledgerService.retrieveLedgers(eq(StringUtils.EMPTY), eq(0), eq(Integer.MAX_VALUE), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(ledgerCollection));

    ExpenseClass expenseClass1 = new ExpenseClass()
      .withCode("Elec")
      .withId("1bcc3247-99bf-4dca-9b0f-7bc51a2998c2")
      .withName("Electronic")
      .withExternalAccountNumberExt("01");

    ExpenseClass expenseClass2 = new ExpenseClass()
      .withCode("Elec")
      .withId("1bcc3247-99bf-4dca-9b0f-7bc51a2998c3")
      .withName("Electronic")
      .withExternalAccountNumberExt("02");

    List<ExpenseClass> expenseClassList = Arrays.asList(expenseClass1, expenseClass2);
    List<String> budgetIds = Arrays.asList(budget1.getId(), budget2.getId());
    when(expenseClassService.getExpenseClassesByBudgetIds(eq(budgetIds), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(expenseClassList));

    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withId("9e662186-7d3e-4732-baaa-93967ccc597e")
      .withBudgetId("d71635df-c08c-49cc-9082-197a30fd0392")
      .withExpenseClassId("1bcc3247-99bf-4dca-9b0f-7bc51a2998c2")
      .withStatus(BudgetExpenseClass.Status.ACTIVE);

    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withId("9e662186-7d3e-4733-baaa-93967ccc597e")
      .withBudgetId("d71635df-c08c-49cc-9082-197a30fd0372")
      .withExpenseClassId("1bcc3247-99bf-4dca-9b0f-7bc51a2998c1")
      .withStatus(BudgetExpenseClass.Status.ACTIVE);

    List<BudgetExpenseClass> budgetExpenseClassList = new ArrayList(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2));
    budgetExpenseClassList.add(budgetExpenseClass1);
    budgetExpenseClassList.add(budgetExpenseClass2);
    when(budgetExpenseClassService.getBudgetExpensesClass(eq(budgetIds), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(budgetExpenseClassList));


    when(ledgerDetailsService.getCurrentFiscalYear(eq(ledgerId1), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(fiscalYear));

    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollection = new FundCodeExpenseClassesCollection();
    FundCodeVsExpClassesType fundCodeVsExpClassesType = new FundCodeVsExpClassesType();
    fundCodeVsExpClassesType.setFundCode("AFRICAHIST");
    fundCodeVsExpClassesType.setLedgerCode("ONETIME");
    List<String> activeList = Arrays.asList("AFRICAHIST:Elec");
    fundCodeVsExpClassesType.setActiveFundCodeVsExpClasses(activeList);
    List<FundCodeVsExpClassesType> fundCodeVsExpClassesTypes = new ArrayList<>();
    fundCodeVsExpClassesTypes.add(fundCodeVsExpClassesType);
    fundCodeExpenseClassesCollection.setFundCodeVsExpClassesTypes(fundCodeVsExpClassesTypes);

    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollection2 = new FundCodeExpenseClassesCollection();
    /*when(fundCodeExpenseClassesService.retrieveCombinationFundCodeExpClasses(eq(fiscalYearId), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(fundCodeExpenseClassesCollection)); */
    fundCodeExpenseClassesCollection2 = fundCodeExpenseClassesService.retrieveCombinationFundCodeExpClasses("FY2021", requestContext).join();

    //assertEquals(0.06, summary.getInitialAllocation());
  }
}
