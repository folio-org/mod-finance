package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.RestTestUtils.verifyGet;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.FINANCE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.vertx.core.Context;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.resource.FinanceFinanceData;
import org.folio.rest.util.HelperUtils;
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

class FinanceDataApiTest {

  @Autowired
  private FinanceDataService financeDataService;
  @Autowired
  private AcqUnitsService acqUnitsService;

  private Context vertxContext;
  private Map<String, String> okapiHeaders;
  private static boolean runningOnOwn;

  @BeforeAll
  static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(ApplicationConfig.class);
  }

  @BeforeEach
  void setUp() {
    autowireDependencies(this);
    vertxContext = mock(Context.class);
    okapiHeaders = mock(Map.class);
  }

  @AfterEach
  void resetMocks() {
    reset(financeDataService);
    reset(acqUnitsService);
  }

  @AfterAll
  static void after() {
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
    clearVertxContext();
  }

  @Test
  void testGetFinanceFinanceDataSuccess() {
    var fiscalYearId = "123e4567-e89b-12d3-a456-426614174004";
    var getFinanceDataWithFiscalYearId = FINANCE_DATA.getEndpoint() + "?query=fiscalYearId==" + fiscalYearId + "&offset=0&limit=10";

    addMockEntry(FINANCE_DATA.name(), FINANCE_DATA.getMockObject());
//    when(acqUnitsService.buildAcqUnitsCqlClause(any()))
//      .thenReturn(io.vertx.core.Future.succeededFuture(""));
    when(financeDataService.getFinanceDataWithAcqUnitsRestriction(any(), anyInt(), anyInt(), any(RequestContext.class)));
    var financeData = RestTestUtils.verifyGet(FINANCE_DATA.getEndpoint(), APPLICATION_JSON, OK.getStatusCode())
      .as(FyFinanceData.class);

    assertThat(fiscalYearId, equalTo(financeData.getFiscalYearId()));
  }

//  @Test
//  void getFinanceFinanceDataReturnsEmptyCollectionWhenNoData() {
//    var fiscalYearId = "123e4567-e89b-12d3-a456-426614174004";
//    var getFinanceDataWithFiscalYearId = FINANCE_DATA.getEndpoint() + "?query=fiscalYearId==" + fiscalYearId + "&offset=0&limit=10";
//
//    var financeData = RestTestUtils.verifyGet(FINANCE_DATA.getEndpoint(), APPLICATION_JSON, OK.getStatusCode())
//      .as(FyFinanceDataCollection.class);
//
//    assertThat(financeData.getTotalRecords(), is(0));
//    assertThat(financeData.getFyFinanceData(), is(empty()));
//  }
//
//  @Test
//  void testGetFinanceFinanceDataFailure() {
//    String query = "query";
//    int offset = 0;
//    int limit = 10;
//    Throwable throwable = new RuntimeException("Error");
//
//    when(financeDataService.getFinanceDataWithAcqUnitsRestriction(any(), anyInt(), anyInt(), any(RequestContext.class)))
//      .thenReturn(io.vertx.core.Future.failedFuture(throwable));
//
//    Handler<AsyncResult<Response>> asyncResultHandler = asyncResult -> {
//      assertThat(asyncResult.failed(), equalTo(true));
//      assertThat(asyncResult.cause(), equalTo(throwable));
//    };
//
////    financeDataApi.getFinanceFinanceData(query, null, offset, limit, okapiHeaders, asyncResultHandler, vertxContext);
//
//    verify(financeDataService).getFinanceDataWithAcqUnitsRestriction(query, offset, limit, new RequestContext(vertxContext, okapiHeaders));
//  }

  @Test
  void testGetCollectionGffyNotFound() {
    var collection = verifyGet(
      HelperUtils.getEndpoint(FinanceFinanceData.class) + RestTestUtils.buildQueryParam("id==(" + UUID.randomUUID() + ")"),
      APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(FyFinanceDataCollection.class);

    assertThat(collection.getTotalRecords(), is(0));
    assertThat(collection.getTotalRecords(), is(0));
  }

  static class ContextConfiguration {

    @Bean
    public FinanceDataService financeDataService() {
      return mock(FinanceDataService.class);
    }

    @Bean
    public AcqUnitsService acqUnitsService() {
      return mock(AcqUnitsService.class);
    }
  }
}
