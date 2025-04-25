package org.folio.rest.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.RestTestUtils.verifyGet;
import static org.folio.rest.util.RestTestUtils.verifyGetWithParam;
import static org.folio.rest.util.RestTestUtils.verifyPut;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestUtils.getMockData;
import static org.folio.services.protection.AcqUnitConstants.NO_FD_FUND_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.HttpException;
import org.folio.ApiTestSuite;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.services.financedata.FinanceDataService;
import org.folio.services.protection.AcqUnitsService;
import org.folio.util.CopilotGenerated;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

@CopilotGenerated(partiallyGenerated = true)
public class FinanceDataApiTest {

  @Autowired
  public FinanceDataService financeDataService;
  @Autowired
  public AcqUnitsService acqUnitsService;

  private static boolean runningOnOwn;
  private static final String FINANCE_DATA_ENDPOINT = "/finance/finance-data";

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
  void positive_testGetFinanceFinanceDataSuccess() {
    var fiscalYearId = "123e4567-e89b-12d3-a456-426614174004";
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(new FyFinanceData().withFiscalYearId(fiscalYearId)))
      .withTotalRecords(1);
    String query = "fiscalYearId==" + fiscalYearId;
    int limit = 5;
    int offset = 1;

    Map<String, Object> params = new HashMap<>();
    params.put("query", query);
    params.put("limit", limit);
    params.put("offset", offset);

    when(acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(any())).thenReturn(succeededFuture(NO_FD_FUND_UNIT_ASSIGNED_CQL));
    when(financeDataService.getFinanceDataWithAcqUnitsRestriction(anyString(), anyInt(), anyInt(), any()))
      .thenReturn(succeededFuture(financeDataCollection));

    var response = verifyGetWithParam(FINANCE_DATA_ENDPOINT, APPLICATION_JSON, OK.getStatusCode(), params)
      .as(FyFinanceDataCollection.class);

    assertEquals(financeDataCollection, response);
    verify(financeDataService).getFinanceDataWithAcqUnitsRestriction(eq(query), eq(offset), eq(limit), any(RequestContext.class));
  }

  @Test
  void negative_testGetFinanceFinanceDataFailure() {
    Future<FyFinanceDataCollection> financeDataFuture = failedFuture(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(financeDataService.getFinanceDataWithAcqUnitsRestriction(any(), anyInt(), anyInt(), any(RequestContext.class)))
      .thenReturn(financeDataFuture);

    var errors = verifyGet(FINANCE_DATA_ENDPOINT, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().getFirst().getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  @Test
  void positive_testPutFinanceFinanceDataSuccess() throws IOException {
    var financeDataCollection = getFinanceDataCollection();
    when(financeDataService.putFinanceData(any(FyFinanceDataCollection.class), any(RequestContext.class)))
      .thenReturn(succeededFuture(financeDataCollection));

    var response = verifyPut(FINANCE_DATA_ENDPOINT, financeDataCollection, APPLICATION_JSON, OK.getStatusCode())
      .as(FyFinanceDataCollection.class);

    assertEquals(financeDataCollection, response);
    verify(financeDataService).putFinanceData(eq(financeDataCollection), any(RequestContext.class));
  }

  @Test
  void negative_testPutFinanceFinanceDataFailure() throws IOException {
    var financeDataCollection = getFinanceDataCollection();
    Future<FyFinanceDataCollection> failedFuture = failedFuture(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(financeDataService.putFinanceData(any(FyFinanceDataCollection.class), any(RequestContext.class)))
      .thenReturn(failedFuture);

    var errors = verifyPut(FINANCE_DATA_ENDPOINT, financeDataCollection, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode())
      .as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().getFirst().getCode(), is(GENERIC_ERROR_CODE.getCode()));
    verify(financeDataService).putFinanceData(eq(financeDataCollection), any(RequestContext.class));
  }

  @Test
  void negative_testPutFinanceFinanceDataBadRequest() throws IOException {
    var financeDataCollection = getFinanceDataCollection();
    // Modify one field to make it invalid
    financeDataCollection.getFyFinanceData().getFirst().setFiscalYearId(null);

    var errors = verifyPut(FINANCE_DATA_ENDPOINT, financeDataCollection, APPLICATION_JSON, 422)
      .as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().getFirst().getCode(), is("jakarta.validation.constraints.NotNull.message"));
  }

  @Test
  void positive_testPutFinanceFinanceDataWithEmptyCollection() {
    var entity = new FyFinanceDataCollection()
      .withFyFinanceData(emptyList())
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT)
      .withTotalRecords(0);

    when(financeDataService.putFinanceData(any(FyFinanceDataCollection.class), any(RequestContext.class)))
      .thenReturn(succeededFuture(entity));

    var response = verifyPut(FINANCE_DATA_ENDPOINT, entity, APPLICATION_JSON, OK.getStatusCode())
      .as(FyFinanceDataCollection.class);

    assertEquals(entity, response);
    verify(financeDataService).putFinanceData(eq(entity), any(RequestContext.class));
  }

  @Test
  void positive_testPutFinanceFinanceDataPreviewMode() throws IOException {
    var financeDataCollection = getFinanceDataCollection();
    financeDataCollection.setUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(financeDataService.putFinanceData(any(FyFinanceDataCollection.class), any(RequestContext.class)))
      .thenReturn(succeededFuture(financeDataCollection));

    var response = verifyPut(FINANCE_DATA_ENDPOINT, financeDataCollection, APPLICATION_JSON, OK.getStatusCode())
      .as(FyFinanceDataCollection.class);

    assertEquals(financeDataCollection, response);
    verify(financeDataService).putFinanceData(eq(financeDataCollection), any(RequestContext.class));
  }

  private FyFinanceDataCollection getFinanceDataCollection() throws IOException {
    var jsonData = getMockData("mockdata/finance-data/fy_finance_data_collection_put.json");
    var jsonObject = new JsonObject(jsonData);
    return jsonObject.mapTo(FyFinanceDataCollection.class);
  }

  static class ContextConfiguration {

    @Bean
    public FinanceDataService financeDataService() {
      return mock(FinanceDataService.class);
    }

    @Bean
    AcqUnitsService acqUnitsService() {
      return mock(AcqUnitsService.class);
    }
  }
}
