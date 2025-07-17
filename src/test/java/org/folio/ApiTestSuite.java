package org.folio;

import static org.folio.rest.util.TestConfig.closeMockServer;
import static org.folio.rest.util.TestConfig.closeVertx;
import static org.folio.rest.util.TestConfig.deployVerticle;
import static org.folio.rest.util.TestConfig.startMockServer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.rest.impl.BudgetsApiTest;
import org.folio.rest.impl.EncumbrancesTest;
import org.folio.rest.impl.EntitiesCrudBasicsTest;
import org.folio.rest.impl.ExchangeTest;
import org.folio.rest.impl.FinanceDataApiTest;
import org.folio.rest.impl.FiscalYearTest;
import org.folio.rest.impl.FundCodeExpenseClassesApiTest;
import org.folio.rest.impl.FundUpdateLogApiTest;
import org.folio.rest.impl.FundsApiTest;
import org.folio.rest.impl.GroupFiscalYearSummariesTest;
import org.folio.rest.impl.GroupsApiTest;
import org.folio.rest.impl.LedgerRolloverApiTest;
import org.folio.rest.impl.LedgerRolloverBudgetsApiTest;
import org.folio.rest.impl.LedgerRolloverErrorsApiTest;
import org.folio.rest.impl.LedgerRolloverLogsApiTest;
import org.folio.rest.impl.LedgerRolloverProgressApiTest;
import org.folio.rest.impl.LedgersApiTest;
import org.folio.rest.impl.TransactionApiTest;
import org.folio.rest.util.HelperUtilsTest;
import org.folio.services.TransactionServiceTest;
import org.folio.services.ExpenseClassServiceTest;
import org.folio.services.budget.BudgetExpenseClassServiceTest;
import org.folio.services.budget.BudgetExpenseClassTotalsServiceTest;
import org.folio.services.budget.BudgetServiceTest;
import org.folio.services.budget.CreateBudgetServiceTest;
import org.folio.services.budget.RecalculateBudgetServiceTest;
import org.folio.services.exchange.ExchangeServiceTest;
import org.folio.services.exchange.ManualCurrencyConversionTest;
import org.folio.services.financedata.FinanceDataServiceTest;
import org.folio.services.financedata.FinanceDataValidatorTest;
import org.folio.services.fiscalyear.FiscalYearApiServiceTest;
import org.folio.services.fiscalyear.FiscalYearServiceTest;
import org.folio.services.fund.FundCodeExpenseClassesServiceTest;
import org.folio.services.fund.FundDetailsServiceTest;
import org.folio.services.fund.FundServiceTest;
import org.folio.services.fund.FundUpdateLogServiceTest;
import org.folio.services.group.GroupExpenseClassTotalsServiceTest;
import org.folio.services.group.GroupFundFiscalYearServiceTest;
import org.folio.services.group.GroupServiceTest;
import org.folio.services.ledger.LedgerDetailsServiceTest;
import org.folio.services.ledger.LedgerRolloverBudgetsServiceTest;
import org.folio.services.ledger.LedgerRolloverErrorsServiceTest;
import org.folio.services.ledger.LedgerRolloverLogsServiceTest;
import org.folio.services.ledger.LedgerRolloverProgressServiceTest;
import org.folio.services.ledger.LedgerRolloverServiceTest;
import org.folio.services.ledger.LedgerServiceTest;
import org.folio.services.ledger.LedgerTotalsServiceTest;
import org.folio.services.protection.AcqUnitMembershipsServiceTest;
import org.folio.services.protection.AcqUnitsServiceTest;
import org.folio.services.protection.ProtectionServiceTest;
import org.folio.services.transactions.TransactionApiServiceTest;
import org.folio.services.transactions.TransactionTotalApiServiceTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

public class ApiTestSuite {

  @BeforeAll
  public static void before() throws InterruptedException, ExecutionException, TimeoutException {
    startMockServer();
    deployVerticle();
  }

  @AfterAll
  public static void after() {
    closeMockServer();
    closeVertx();
  }

  @Nested
  class EntitiesCrudTestNested extends EntitiesCrudBasicsTest {
  }

  @Nested
  class FundsApiTestNested extends FundsApiTest {
  }

  @Nested
  class BudgetsApiTestNested extends BudgetsApiTest {
  }

  @Nested
  class EncumbranceTestNested extends EncumbrancesTest {
  }

  @Nested
  class FiscalYearTestNested extends FiscalYearTest {
  }

  @Nested
  class GroupFiscalYearSummariesTestNested extends GroupFiscalYearSummariesTest {
  }

  @Nested
  class LedgersApiTestNested extends LedgersApiTest {
  }

  @Nested
  class LedgerRolloversApiTestNested extends LedgerRolloverApiTest {
  }

  @Nested
  class LedgerRolloversLogsApiTestNested extends LedgerRolloverLogsApiTest {
  }

  @Nested
  class LedgerRolloversBudgetsApiTestNested extends LedgerRolloverBudgetsApiTest {
  }

  @Nested
  class LedgerRolloversErrorsApiTestNested extends LedgerRolloverErrorsApiTest {
  }

  @Nested
  class LedgerRolloversProgressApiTestNested extends LedgerRolloverProgressApiTest {
  }

  @Nested
  class ExchangeTestNested extends ExchangeTest {
  }

  @Nested
  class TransactionApiTestNested extends TransactionApiTest {
  }

  @Nested
  class HelperUtilsTestNested extends HelperUtilsTest {
  }

  @Nested
  class BudgetExpenseClassServiceTestNested extends BudgetExpenseClassServiceTest {
  }

  @Nested
  class BudgetExpenseClassTotalsServiceTestNested extends BudgetExpenseClassTotalsServiceTest {
  }

  @Nested
  class ExpenseClassServiceTestNested extends ExpenseClassServiceTest {
  }

  @Nested
  class TransactionServiceTestNested extends TransactionServiceTest {
  }

  @Nested
  class TransactionApiServiceTestNested extends TransactionApiServiceTest {
  }

  @Nested
  class BudgetServiceTestNested extends BudgetServiceTest {
  }

  @Nested
  class GroupFundFiscalYearServiceTestNested extends GroupFundFiscalYearServiceTest {
  }

  @Nested
  class FundDetailsServiceTestNested extends FundDetailsServiceTest {
  }

  @Nested
  class GroupExpenseClassTotalsServiceTestNested extends GroupExpenseClassTotalsServiceTest {
  }

  @Nested
  class GroupsApiTestNested extends GroupsApiTest {
  }

  @Nested
  class LedgerServiceTestNested extends LedgerServiceTest {
  }

  @Nested
  class LedgerRolloverServiceTestNested extends LedgerRolloverServiceTest {
  }

  @Nested
  class LedgerRolloverLogsServiceTestNested extends LedgerRolloverLogsServiceTest {
  }

  @Nested
  class LedgerRolloverBudgetsServiceTestNested extends LedgerRolloverBudgetsServiceTest {
  }

  @Nested
  class LedgerRolloverErrorsServiceTestNested extends LedgerRolloverErrorsServiceTest {
  }

  @Nested
  class LedgerRolloverProgressServiceTestNested extends LedgerRolloverProgressServiceTest {
  }

  @Nested
  class LedgerTotalsServiceTestNested extends LedgerTotalsServiceTest {
  }

  @Nested
  class LedgerDetailsServiceTestNested extends LedgerDetailsServiceTest {
  }

  @Nested
  class CreateBudgetServiceTestNested extends CreateBudgetServiceTest {
  }

  @Nested
  class FundServiceTestNested extends FundServiceTest {
  }

  @Nested
  class FiscalYearApiServiceTestNested extends FiscalYearApiServiceTest {
  }

  @Nested
  class FiscalYearServiceTestNested extends FiscalYearServiceTest {
  }

  @Nested
  class AcqUnitMembershipsServiceTestNested extends AcqUnitMembershipsServiceTest {
  }

  @Nested
  class AcqUnitsServiceTestNested  extends AcqUnitsServiceTest {
  }

  @Nested
  class ProtectionServiceTestNested  extends ProtectionServiceTest {
  }

  @Nested
  class FundCodeExpenseClassesApiTestNested extends FundCodeExpenseClassesApiTest {
  }

  @Nested
  class FundCodeExpenseClassesServiceTestNested extends FundCodeExpenseClassesServiceTest {
  }

  @Nested
  class GroupServiceNested extends GroupServiceTest {}

  @Nested
  class RecalculateBudgetServiceTestNested extends RecalculateBudgetServiceTest {}

  @Nested
  class FinanceDataApiTestNested extends FinanceDataApiTest {}

  @Nested
  class FinanceDataServiceTestNested extends FinanceDataServiceTest {}

  @Nested
  class FinanceDataValidatorTestNested extends FinanceDataValidatorTest {}

  @Nested
  class TransactionTotalApiServiceTestNested extends TransactionTotalApiServiceTest {}

  @Nested
  class FundUpdateLogApiTestNested extends FundUpdateLogApiTest {}

  @Nested
  class FundUpdateLogServiceTestNested extends FundUpdateLogServiceTest {}

  @Nested
  class ExchangeServiceTestNested extends ExchangeServiceTest {}

  @Nested
  class ManualCurrencyConversionTestNested extends ManualCurrencyConversionTest {}
}
