package org.folio;

import org.folio.rest.impl.BudgetsApiTest;
import org.folio.rest.impl.EncumbrancesTest;
import org.folio.rest.impl.EntitiesCrudBasicsTest;
import org.folio.rest.impl.ExchangeRateTest;
import org.folio.rest.impl.FiscalYearTest;
import org.folio.rest.impl.FundCodeExpenseClassesApiTest;
import org.folio.rest.impl.FundsApiTest;
import org.folio.rest.impl.GroupFiscalYearSummariesTest;
import org.folio.rest.impl.GroupsApiTest;
import org.folio.rest.impl.LedgerRolloverApiTest;
import org.folio.rest.impl.LedgerRolloverLogsApiTest;
import org.folio.rest.impl.LedgerRolloverErrorsApiTest;
import org.folio.rest.impl.LedgerRolloverProgressApiTest;
import org.folio.rest.impl.LedgersApiTest;
import org.folio.rest.impl.TransactionApiTest;
import org.folio.rest.util.HelperUtilsTest;
import org.folio.services.CommonTransactionServiceTest;
import org.folio.services.ExpenseClassServiceTest;
import org.folio.services.budget.BudgetExpenseClassServiceTest;
import org.folio.services.budget.BudgetExpenseClassTotalsServiceTest;
import org.folio.services.budget.BudgetServiceTest;
import org.folio.services.budget.CreateBudgetServiceTest;
import org.folio.services.fiscalyear.FiscalYearServiceTest;
import org.folio.services.fund.FundCodeExpenseClassesServiceTest;
import org.folio.services.fund.FundDetailsServiceTest;
import org.folio.services.fund.FundServiceTest;
import org.folio.services.group.GroupExpenseClassTotalsServiceTest;
import org.folio.services.group.GroupFundFiscalYearServiceTest;
import org.folio.services.ledger.LedgerDetailsServiceTest;
import org.folio.services.ledger.LedgerRolloverLogsServiceTest;
import org.folio.services.ledger.LedgerRolloverErrorsServiceTest;
import org.folio.services.ledger.LedgerRolloverProgressServiceTest;
import org.folio.services.ledger.LedgerRolloverServiceTest;
import org.folio.services.ledger.LedgerServiceTest;
import org.folio.services.ledger.LedgerTotalsServiceTest;
import org.folio.services.protection.AcqUnitMembershipsServiceTest;
import org.folio.services.protection.AcqUnitsServiceTest;
import org.folio.services.protection.ProtectionServiceTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.folio.rest.util.TestConfig.closeMockServer;
import static org.folio.rest.util.TestConfig.closeVertx;
import static org.folio.rest.util.TestConfig.deployVerticle;
import static org.folio.rest.util.TestConfig.startMockServer;

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
  class LedgerRolloversErrorsApiTestNested extends LedgerRolloverErrorsApiTest {
  }

  @Nested
  class LedgerRolloversProgressApiTestNested extends LedgerRolloverProgressApiTest {
  }

  @Nested
  class ExchangeRateTestNested extends ExchangeRateTest {
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
  class CommonTransactionServiceTestNested extends CommonTransactionServiceTest {
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

}
