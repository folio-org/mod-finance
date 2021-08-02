package org.folio.services.fund;

import io.vertx.core.impl.EventLoopContext;
import org.apache.commons.lang.StringUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundCodeExpenseClassesCollection;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

  private RequestContext requestContext;
  @Mock
  private EventLoopContext context;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @Test
  public void shouldRetrieveCombinationFundCodeExpClassesWithFiscalYear() {

    String fiscalYearCode = "FY2021";
    String fiscalYearId = "684b5dc5-92f6-4db7-b996-b549d88f5e4e";

    FiscalYear fiscalYear = new FiscalYear()
      .withId(fiscalYearId)
      .withCode(fiscalYearCode)
      .withName("Fiscal Year 2021")
      .withSeries("FY");
    when(fiscalYearService.getFiscalYearByFiscalYearCode(eq(fiscalYearCode), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(fiscalYear));

    String fundId1 = "69640328-788e-43fc-9c3c-af39e243f3b7";
    String fundId2 = "bbd4a5e1-c9f3-44b9-bfdf-d184e04f0ba0";
    String budgetId1 = "012e7da4-003c-48ff-9feb-f3745044da35";
    String budgetId2 = "0916fde2-7a7d-4ff0-9069-37ee1ec0c068";
    Budget budget1 = new Budget()
      .withId(budgetId1)
      .withFundId(fundId1)
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
      .withId(budgetId2)
      .withFundId(fundId2)
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

    BudgetsCollection budgetCollection = new BudgetsCollection();
    budgetCollection.setBudgets(Arrays.asList(budget1, budget2));

    when(budgetService.getBudgets(anyString(), eq(0), eq(Integer.MAX_VALUE),
      eq(requestContext))).thenReturn(CompletableFuture.completedFuture(budgetCollection));

    String ledgerId1 = "65cb2bf0-d4c2-4886-8ad0-b76f1ba75d61";
    String ledgerId2 = "65cb2bf0-d4c2-4886-8ad0-b76f1ba75d63";
    Fund fund1 = new Fund()
      .withFundStatus(Fund.FundStatus.ACTIVE)
      .withFundTypeId(UUID.randomUUID().toString())
      .withCode("ENDOW-SUBN")
      .withName("Endowments")
      .withLedgerId(ledgerId1)
      .withDescription("ongoing endowments used for subscriptions")
      .withId(fundId1);

    Fund fund2 = new Fund()
      .withFundStatus(Fund.FundStatus.ACTIVE)
      .withFundTypeId(UUID.randomUUID().toString())
      .withCode("GRANT-SUBN")
      .withName("Endowments")
      .withLedgerId(ledgerId2)
      .withDescription("ongoing endowments used for subscriptions")
      .withId(fundId2);

    List<Fund> funds = Arrays.asList(fund1, fund2);
    when(fundService.getFunds(any(), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(funds));

    Ledger ledger1 = new Ledger()
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE)
      .withId(ledgerId1)
      .withCode("ONETIME")
      .withFiscalYearOneId(fiscalYearId)
      .withName("One-time")
      .withCurrency("USD");

    Ledger ledger2 = new Ledger()
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE)
      .withId(ledgerId2)
      .withCode("ONETIME")
      .withFiscalYearOneId("684b5dc5-92f6-4db7-b996-b549d88f5e9e")
      .withName("One-time")
      .withCurrency("USD");

    List<Ledger> ledgers = Arrays.asList(ledger1, ledger2);
    List<String> ledgerIds = new ArrayList();
    ledgerIds.add(ledgerId1);
    ledgerIds.add(ledgerId2);

    when(ledgerService.getLedgers(any(), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(ledgers));

    LedgersCollection ledgerCollection = new LedgersCollection();
    ledgerCollection.setLedgers(ledgers);
    when(ledgerService.retrieveLedgers(eq(StringUtils.EMPTY), eq(0), eq(Integer.MAX_VALUE), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(ledgerCollection));

    String expenseClassId1 = "1bcc3247-99bf-4dca-9b0f-7bc51a2998c2";
    String expenseClassId2 = "1bcc3247-99bf-4dca-9b0f-7bc51a2998c3";
    ExpenseClass expenseClass1 = new ExpenseClass()
      .withCode("Elec")
      .withId(expenseClassId1)
      .withName("Electronic")
      .withExternalAccountNumberExt("01");

    ExpenseClass expenseClass2 = new ExpenseClass()
      .withCode("Elec")
      .withId(expenseClassId2)
      .withName("Electronic")
      .withExternalAccountNumberExt("02");

    List<ExpenseClass> expenseClassList = Arrays.asList(expenseClass1, expenseClass2);
    List<String> budgetIds = Arrays.asList(budget1.getId(), budget2.getId());
    when(expenseClassService.getExpenseClassesByBudgetIds(eq(budgetIds), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(expenseClassList));

    String budgetExpenseClassId1 = "9e662186-7d3e-4832-baaa-93967ccc597e";
    String budgetExpenseClassId2 = "9e662186-7d3e-4733-baaa-93967ccc597e";
    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withId(budgetExpenseClassId1)
      .withBudgetId(budgetId1)
      .withExpenseClassId(expenseClassId1)
      .withStatus(BudgetExpenseClass.Status.ACTIVE);

    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withId(budgetExpenseClassId2)
      .withBudgetId(budgetId2)
      .withExpenseClassId(expenseClassId2)
      .withStatus(BudgetExpenseClass.Status.ACTIVE);

    List<BudgetExpenseClass> budgetExpenseClassList = new ArrayList(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2));
    when(budgetExpenseClassService.getBudgetExpensesClass(eq(budgetIds), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(budgetExpenseClassList));

    when(ledgerDetailsService.getCurrentFiscalYear(eq(ledgerId1), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(fiscalYear));

    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollectionReceived = new FundCodeExpenseClassesCollection();

    fundCodeExpenseClassesCollectionReceived = fundCodeExpenseClassesService.retrieveCombinationFundCodeExpClasses("FY2021", requestContext).join();

    assertEquals("ENDOW-SUBN", fundCodeExpenseClassesCollectionReceived.getFundCodeVsExpClassesTypes().get(0).getFundCode());
    assertEquals("ONETIME", fundCodeExpenseClassesCollectionReceived.getFundCodeVsExpClassesTypes().get(0).getLedgerCode());
    assertEquals(":", fundCodeExpenseClassesCollectionReceived.getDelimiter());
    assertEquals("ENDOW-SUBN:Elec", fundCodeExpenseClassesCollectionReceived.getFundCodeVsExpClassesTypes()
      .get(0).getActiveFundCodeVsExpClasses().get(0));
  }

  @Test
  public void shouldRetrieveCombinationFundCodeExpClassesWithoutFiscalYear() {

    String ledgerId1 = "65cb2bf0-d4c2-4886-8ad0-b76f1ba75d61";
    String ledgerId2 = "65cb2bf0-d4c2-4886-8ad0-b76f1ba75d63";
    String fiscalYearId = "684b5dc5-92f6-4db7-b996-b549d88f5e4e";

    Ledger ledger1 = new Ledger()
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE)
      .withId(ledgerId1)
      .withCode("ONETIME")
      .withFiscalYearOneId(fiscalYearId)
      .withName("One-time")
      .withCurrency("USD");

    Ledger ledger2 = new Ledger()
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE)
      .withId(ledgerId2)
      .withCode("ONETIME")
      .withFiscalYearOneId("684b5dc5-92f6-4db7-b996-b549d88f5e9e")
      .withName("One-time")
      .withCurrency("USD");

    List<Ledger> ledgers = Arrays.asList(ledger1, ledger2);
    List<String> ledgerIds = new ArrayList();
    ledgerIds.add(ledgerId1);
    ledgerIds.add(ledgerId2);
    LedgersCollection ledgersCollection = new LedgersCollection();
    ledgersCollection.setLedgers(ledgers);

    when(ledgerService.retrieveLedgers(any(), eq(0), eq(Integer.MAX_VALUE), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(ledgersCollection));

    String fiscalYearCode = "FY2021";
    FiscalYear fiscalYear = new FiscalYear()
      .withId(fiscalYearId)
      .withCode(fiscalYearCode)
      .withName("Fiscal Year 2021")
      .withSeries("FY");
    when(ledgerDetailsService.getCurrentFiscalYear(any(), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(fiscalYear));

    when(fiscalYearService.getFiscalYearByFiscalYearCode(eq(fiscalYearCode), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(fiscalYear));

    String fundId1 = "69640328-788e-43fc-9c3c-af39e243f3b7";
    String fundId2 = "bbd4a5e1-c9f3-44b9-bfdf-d184e04f0ba0";
    Fund fund1 = new Fund()
      .withFundStatus(Fund.FundStatus.ACTIVE)
      .withFundTypeId(UUID.randomUUID().toString())
      .withCode("ENDOW-SUBN")
      .withName("Endowments")
      .withLedgerId(ledgerId1)
      .withDescription("ongoing endowments used for subscriptions")
      .withId(fundId1);

    Fund fund2 = new Fund()
      .withFundStatus(Fund.FundStatus.ACTIVE)
      .withFundTypeId(UUID.randomUUID().toString())
      .withCode("GRANT-SUBN")
      .withName("Endowments")
      .withLedgerId(ledgerId2)
      .withDescription("ongoing endowments used for subscriptions")
      .withId(fundId2);

    List<Fund> funds = Arrays.asList(fund1, fund2);
    when(fundService.getFunds(any(), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(funds));

    when(ledgerService.getLedgers(any(), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(ledgers));

    when(fiscalYearService.getFiscalYearByFiscalYearCode(eq(fiscalYearCode), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(fiscalYear));

    String expenseClassId1 = "1bcc3247-99bf-4dca-9b0f-7bc51a2998c2";
    String expenseClassId2 = "1bcc3247-99bf-4dca-9b0f-7bc51a2998c3";
    String budgetId1 = "012e7da4-003c-48ff-9feb-f3745044da35";
    String budgetId2 = "0916fde2-7a7d-4ff0-9069-37ee1ec0c068";
    String budgetExpenseClassId1 = "9e662186-7d3e-4832-baaa-93967ccc597e";
    String budgetExpenseClassId2 = "9e662186-7d3e-4733-baaa-93967ccc597e";

    Budget budget1 = new Budget()
      .withId(budgetId1)
      .withFundId(fundId1)
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
      .withId(budgetId2)
      .withFundId(fundId2)
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

    BudgetsCollection budgetCollection = new BudgetsCollection();
    budgetCollection.setBudgets(Arrays.asList(budget1, budget2));
    List<String> budgetIds = Arrays.asList(budget1.getId(), budget2.getId());

    BudgetExpenseClass budgetExpenseClass1 = new BudgetExpenseClass()
      .withId(budgetExpenseClassId1)
      .withBudgetId(budgetId1)
      .withExpenseClassId(expenseClassId1)
      .withStatus(BudgetExpenseClass.Status.ACTIVE);

    BudgetExpenseClass budgetExpenseClass2 = new BudgetExpenseClass()
      .withId(budgetExpenseClassId2)
      .withBudgetId(budgetId2)
      .withExpenseClassId(expenseClassId2)
      .withStatus(BudgetExpenseClass.Status.ACTIVE);

    List<BudgetExpenseClass> budgetExpenseClassList = new ArrayList(Arrays.asList(budgetExpenseClass1, budgetExpenseClass2));

    when(budgetService.getBudgets(anyString(), eq(0), eq(Integer.MAX_VALUE),
      eq(requestContext))).thenReturn(CompletableFuture.completedFuture(budgetCollection));

    when(budgetExpenseClassService.getBudgetExpensesClass(eq(budgetIds), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(budgetExpenseClassList));

    ExpenseClass expenseClass1 = new ExpenseClass()
      .withCode("Elec")
      .withId(expenseClassId1)
      .withName("Electronic")
      .withExternalAccountNumberExt("01");

    ExpenseClass expenseClass2 = new ExpenseClass()
      .withCode("Elec")
      .withId(expenseClassId2)
      .withName("Electronic")
      .withExternalAccountNumberExt("02");

    List<ExpenseClass> expenseClassList = Arrays.asList(expenseClass1, expenseClass2);
    when(expenseClassService.getExpenseClassesByBudgetIds(eq(budgetIds), eq(requestContext)))
      .thenReturn(CompletableFuture.completedFuture(expenseClassList));
    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollectionReceived;

    fundCodeExpenseClassesCollectionReceived = fundCodeExpenseClassesService.retrieveCombinationFundCodeExpClasses(null, requestContext).join();

    assertEquals("ENDOW-SUBN", fundCodeExpenseClassesCollectionReceived.getFundCodeVsExpClassesTypes().get(0).getFundCode());
    assertEquals("ONETIME", fundCodeExpenseClassesCollectionReceived.getFundCodeVsExpClassesTypes().get(0).getLedgerCode());
    assertEquals(":", fundCodeExpenseClassesCollectionReceived.getDelimiter());
    assertEquals("ENDOW-SUBN:Elec", fundCodeExpenseClassesCollectionReceived.getFundCodeVsExpClassesTypes()
      .get(0).getActiveFundCodeVsExpClasses().get(0));
  }
}
