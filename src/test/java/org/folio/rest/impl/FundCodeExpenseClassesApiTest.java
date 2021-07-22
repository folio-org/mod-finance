package org.folio.rest.impl;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.ApiTestSuite;
import org.folio.rest.jaxrs.model.FundCodeExpenseClassesCollection;
import org.folio.rest.jaxrs.model.FundCodeVsExpClassesType;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.TestEntities;
import org.folio.services.fund.FundCodeExpenseClassesService;
import org.folio.services.ledger.LedgerDetailsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.RestTestUtils.verifyGetWithParam;
import static org.folio.rest.util.TestConfig.*;
import static org.folio.rest.util.TestEntities.FUND_CODE_EXPENSE_CLASS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FundCodeExpenseClassesApiTest {

  private static final Logger logger = LogManager.getLogger(FundCodeExpenseClassesApiTest.class);

  @Autowired
  private FundCodeExpenseClassesService fundCodeExpenseClassesService;

  private static boolean runningOnOwn;

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(FundCodeExpenseClassesApiTest.ContextConfiguration.class);
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterAll
  static void afterAll() {
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
    clearVertxContext();
  }

  @AfterEach
  void resetMocks() {
    reset(fundCodeExpenseClassesService);
    //reset()
  }


  @Test
  public void testGetFinanceFundCodesExpenseClasses() {
    logger.info("=== Test Get Finance Fund Codes And ExpenseClass Codes By Fiscal Year Code ===");
    String fiscalYearCode = "FY2021";
    /*FundCodeExpenseClassesCollection fundCodeExpenseClassesCollection = RestTestUtils.verifyGet(FUND_CODE_EXPENSE_CLASS.getEndpoint(),
      APPLICATION_JSON, OK.getStatusCode()).as(FundCodeExpenseClassesCollection.class); */

    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollection = new FundCodeExpenseClassesCollection();
    FundCodeVsExpClassesType fundCodeVsExpClassesType = new FundCodeVsExpClassesType();
    fundCodeVsExpClassesType.setFundCode("GIFT-SUBN");
    fundCodeVsExpClassesType.setLedgerCode("ONGOING");
    List<FundCodeVsExpClassesType> fundCodeVsExpClassesTypeList = new ArrayList<>();
    fundCodeVsExpClassesTypeList.add(fundCodeVsExpClassesType);
    fundCodeExpenseClassesCollection.setFundCodeVsExpClassesTypes(fundCodeVsExpClassesTypeList);

    when(fundCodeExpenseClassesService.retrieveCombinationFundCodeExpClasses(eq(fiscalYearCode), any()))
      .thenReturn(CompletableFuture.completedFuture(fundCodeExpenseClassesCollection));

    FundCodeExpenseClassesCollection resultFundCodeCollection = verifyGetWithParam(FUND_CODE_EXPENSE_CLASS.getEndpoint(),
      APPLICATION_JSON, OK.getStatusCode(), "fiscalYearCode", fiscalYearCode).as(FundCodeExpenseClassesCollection.class);

    //assertThat(fundCodeExpenseClassesCollection.getFundCodeVsExpClassesTypes(), hasSize(1));
  }

  private void verifyRsEntitiesQuantity(HttpMethod httpMethod, TestEntities entity, int expectedQuantity) {
    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(httpMethod, entity.name());
    assertThat(rqRsPostFund, hasSize(expectedQuantity));
  }

  static class ContextConfiguration {

    @Bean
    public FundCodeExpenseClassesService fundCodeExpenseClassesService() {
      return mock(FundCodeExpenseClassesService.class);
    }

    @Bean
    public LedgerDetailsService currentFiscalYearService() {
      return mock(LedgerDetailsService.class);
    }
  }
}
