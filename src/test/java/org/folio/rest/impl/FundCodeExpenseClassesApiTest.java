package org.folio.rest.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.ApiTestSuite;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FundCodeExpenseClassesCollection;
import org.folio.rest.jaxrs.model.FundCodeVsExpClassesType;
import org.folio.rest.jaxrs.resource.FinanceFundCodesExpenseClasses;
import org.folio.rest.util.RestTestUtils;
import org.folio.services.fund.FundCodeExpenseClassesService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.EntityForTest.FUND_CODE_EXPENSE_CLASS;
import static org.folio.rest.util.RestTestUtils.verifyGetWithParam;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  }


  @Test
  public void testGetFinanceFundCodesExpenseClassesWithFiscalYearCode() {
    logger.info("=== Test Get Finance Fund Codes And ExpenseClass Codes By Fiscal Year Code ===");
    String fiscalYearCode = "FY2021";

    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollection = new FundCodeExpenseClassesCollection();
    FundCodeVsExpClassesType fundCodeVsExpClassesType = new FundCodeVsExpClassesType();
    fundCodeVsExpClassesType.setFundCode("GIFT-SUBN");
    fundCodeVsExpClassesType.setLedgerCode("ONGOING");
    List<FundCodeVsExpClassesType> fundCodeVsExpClassesTypeList = new ArrayList<>();
    fundCodeVsExpClassesTypeList.add(fundCodeVsExpClassesType);
    fundCodeExpenseClassesCollection.setFundCodeVsExpClassesTypes(fundCodeVsExpClassesTypeList);

    when(fundCodeExpenseClassesService.retrieveCombinationFundCodeExpClasses(eq(fiscalYearCode), any()))
      .thenReturn(CompletableFuture.completedFuture(fundCodeExpenseClassesCollection));

    FundCodeExpenseClassesCollection resultFundCodeCollection = verifyGetWithParam(FUND_CODE_EXPENSE_CLASS.getEndpoint(FinanceFundCodesExpenseClasses.class),
      APPLICATION_JSON, OK.getStatusCode(), "fiscalYearCode", fiscalYearCode).as(FundCodeExpenseClassesCollection.class);

    assertThat(resultFundCodeCollection.getFundCodeVsExpClassesTypes(), hasSize(1));
    verify(fundCodeExpenseClassesService).retrieveCombinationFundCodeExpClasses(eq(fiscalYearCode), any(RequestContext.class));
  }

  @Test
  public void testGetFinanceFundCodeExpenseClassesEmptyFiscalYearCode() {
    logger.info("=== Test Get Finance Fund Codes And ExpenseClass Codes Without Fiscal Year Code ===");

    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollection = new FundCodeExpenseClassesCollection();
    FundCodeVsExpClassesType fundCodeVsExpClassesType = new FundCodeVsExpClassesType();
    fundCodeVsExpClassesType.setFundCode("GIFT-SUBN");
    fundCodeVsExpClassesType.setLedgerCode("ONGOING");
    List<FundCodeVsExpClassesType> fundCodeVsExpClassesTypeList = new ArrayList<>();
    fundCodeVsExpClassesTypeList.add(fundCodeVsExpClassesType);
    fundCodeExpenseClassesCollection.setFundCodeVsExpClassesTypes(fundCodeVsExpClassesTypeList);

    when(fundCodeExpenseClassesService.retrieveCombinationFundCodeExpClasses(eq(null), any()))
      .thenReturn(CompletableFuture.completedFuture(fundCodeExpenseClassesCollection));

    FundCodeExpenseClassesCollection resultFundCodeCollection = RestTestUtils.verifyGet(FUND_CODE_EXPENSE_CLASS.getEndpoint(FinanceFundCodesExpenseClasses.class),
      APPLICATION_JSON, OK.getStatusCode()).as(FundCodeExpenseClassesCollection.class);

    assertThat(resultFundCodeCollection.getFundCodeVsExpClassesTypes(), hasSize(1));
    verify(fundCodeExpenseClassesService).retrieveCombinationFundCodeExpClasses(eq(null), any(RequestContext.class));
  }

  static class ContextConfiguration {

    @Bean
    public FundCodeExpenseClassesService fundCodeExpenseClassesService() {
      return mock(FundCodeExpenseClassesService.class);
    }
  }
}
