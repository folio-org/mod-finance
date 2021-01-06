package org.folio;

import static org.folio.rest.util.TestConfig.closeMockServer;
import static org.folio.rest.util.TestConfig.closeVertx;
import static org.folio.rest.util.TestConfig.deployVerticle;
import static org.folio.rest.util.TestConfig.startMockServer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.rest.impl.*;
import org.folio.rest.util.HelperUtilsTest;
import org.folio.services.*;
import org.folio.services.budget.BudgetExpenseClassServiceTest;
import org.folio.services.budget.BudgetExpenseClassTotalsServiceTest;
import org.folio.services.budget.BudgetServiceTest;
import org.folio.services.fiscalyear.FiscalYearServiceTest;
import org.folio.services.fund.FundDetailsServiceTest;
import org.folio.services.budget.CreateBudgetServiceTest;
import org.folio.services.fund.FundServiceTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
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
}
