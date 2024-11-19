package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.RestTestUtils.verifyGet;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Future;
import io.vertx.ext.web.handler.HttpException;
import org.folio.ApiTestSuite;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.util.RestTestUtils;
import org.folio.services.financedata.FinanceDataService;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class FinanceDataApiTest {

  @Autowired
  public FinanceDataService financeDataService;
  @Autowired
  public AcqUnitsService acqUnitsService;

  private static boolean runningOnOwn;
  private static final String FINANCE_DATA_ENDPOINT = "/finance-storage/finance-data";

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(FinanceDataApiTest.ContextConfiguration.class);
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
    reset(financeDataService);
    reset(acqUnitsService);
  }

  @Test
  void testGetFinanceFinanceDataSuccess() {
    var fiscalYearId = "123e4567-e89b-12d3-a456-426614174004";
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(new FyFinanceData().withFiscalYearId(fiscalYearId)))
      .withTotalRecords(1);

    when(financeDataService.getFinanceDataWithAcqUnitsRestriction(any(), anyInt(), anyInt(), any(RequestContext.class)))
      .thenReturn(succeededFuture(financeDataCollection));
    when(acqUnitsService.buildAcqUnitsCqlClause(any())).thenReturn(succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL));
    var actualFinanceDataCollection = RestTestUtils.verifyGet(FINANCE_DATA_ENDPOINT, APPLICATION_JSON, OK.getStatusCode())
      .as(FyFinanceDataCollection.class);

    assertThat(fiscalYearId, equalTo(actualFinanceDataCollection.getFyFinanceData().get(0).getFiscalYearId()));
  }

  @Test
  void testGetFinanceFinanceDataFailure() {
    Future<FyFinanceDataCollection> financeDataFuture = Future.failedFuture(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(financeDataService.getFinanceDataWithAcqUnitsRestriction(any(), anyInt(), anyInt(), any(RequestContext.class)))
      .thenReturn(financeDataFuture);

    var errors = verifyGet(FINANCE_DATA_ENDPOINT, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  static class ContextConfiguration {

    @Bean
    public FinanceDataService financeDataService() {
      return mock(FinanceDataService.class);
    }

    @Bean AcqUnitsService acqUnitsService() {
      return mock(AcqUnitsService.class);
    }
  }
}
