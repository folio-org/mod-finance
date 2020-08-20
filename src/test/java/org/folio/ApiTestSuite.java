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
import org.folio.rest.impl.ExchangeRateTest;
import org.folio.rest.impl.FiscalYearTest;
import org.folio.rest.impl.FundsApiTest;
import org.folio.rest.impl.GroupFiscalYearSummariesTest;
import org.folio.rest.impl.GroupsApiTest;
import org.folio.rest.impl.LedgersApiTest;
import org.folio.rest.impl.TransactionTest;
import org.folio.rest.util.HelperUtilsTest;
import org.folio.services.BudgetExpenseClassServiceTest;
import org.folio.services.BudgetExpenseClassTotalsServiceTest;
import org.folio.services.BudgetServiceTest;
import org.folio.services.ExpenseClassServiceTest;
import org.folio.services.FundDetailsServiceTest;
import org.folio.services.GroupExpenseClassTotalsServiceTest;
import org.folio.services.GroupFundFiscalYearServiceTest;
import org.folio.services.LedgerServiceTest;
import org.folio.services.LedgerTotalsServiceTest;
import org.folio.services.TransactionServiceTest;
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
  class ExchangeRateTestNested extends ExchangeRateTest {
  }

  @Nested
  class TransactionTestNested extends TransactionTest {
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
  class LedgerTotalsServiceTestNested extends LedgerTotalsServiceTest {
  }

}
