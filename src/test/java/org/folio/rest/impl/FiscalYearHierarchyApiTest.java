package org.folio.rest.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.RestTestUtils.verifyGetWithParam;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.ApiTestSuite;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYearHierarchy;
import org.folio.rest.jaxrs.model.FiscalYearHierarchyCollection;
import org.folio.services.fiscalyear.FiscalYearHierarchyService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.vertx.core.Future;

public class FiscalYearHierarchyApiTest {

  private static final String FISCAL_YEAR_HIERARCHY_ENDPOINT = "/finance/fiscal-year-hierarchy";

  @Autowired
  public FiscalYearHierarchyService fiscalYearHierarchyService;

  private static boolean runningOnOwn;

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(FiscalYearHierarchyApiTest.ContextConfiguration.class);
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
    reset(fiscalYearHierarchyService);
  }

  @Test
  void positive_testGetFiscalYearHierarchyForwardsAllParams() {
    var fiscalYearId = UUID.randomUUID().toString();
    var hierarchyCollection = new FiscalYearHierarchyCollection()
      .withFiscalYearHierarchies(List.of(new FiscalYearHierarchy().withFiscalYearId(fiscalYearId)))
      .withTotalRecords(1);

    when(fiscalYearHierarchyService.retrieveFiscalYearHierarchy(any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(succeededFuture(hierarchyCollection));

    Map<String, Object> params = new HashMap<>();
    params.put("fiscalYearId", fiscalYearId);
    params.put("ledgerStatus", "Active");
    params.put("groupStatus", "Active");
    params.put("fundStatus", "Active");
    params.put("budgetStatus", "Active");
    params.put("expenseClassStatus", "Active");

    var response = verifyGetWithParam(FISCAL_YEAR_HIERARCHY_ENDPOINT, APPLICATION_JSON, OK.getStatusCode(), params)
      .as(FiscalYearHierarchyCollection.class);

    assertEquals(hierarchyCollection, response);
    verify(fiscalYearHierarchyService).retrieveFiscalYearHierarchy(eq(fiscalYearId), eq("Active"), eq("Active"), eq("Active"),
      eq("Active"), eq("Active"), any(RequestContext.class));
  }

  @Test
  void positive_testGetFiscalYearHierarchyWithOnlyRequiredParam() {
    var fiscalYearId = UUID.randomUUID().toString();
    var hierarchyCollection = new FiscalYearHierarchyCollection()
      .withFiscalYearHierarchies(List.of(new FiscalYearHierarchy().withFiscalYearId(fiscalYearId)))
      .withTotalRecords(1);

    when(fiscalYearHierarchyService.retrieveFiscalYearHierarchy(any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(succeededFuture(hierarchyCollection));

    Map<String, Object> params = new HashMap<>();
    params.put("fiscalYearId", fiscalYearId);

    var response = verifyGetWithParam(FISCAL_YEAR_HIERARCHY_ENDPOINT, APPLICATION_JSON, OK.getStatusCode(), params)
      .as(FiscalYearHierarchyCollection.class);

    assertEquals(hierarchyCollection, response);
    verify(fiscalYearHierarchyService).retrieveFiscalYearHierarchy(eq(fiscalYearId), isNull(), isNull(), isNull(), isNull(),
      isNull(), any(RequestContext.class));
  }

  @Test
  void negative_testGetFiscalYearHierarchyFailure() {
    Future<FiscalYearHierarchyCollection> failedFuture = failedFuture(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(fiscalYearHierarchyService.retrieveFiscalYearHierarchy(any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(failedFuture);

    Map<String, Object> params = new HashMap<>();
    params.put("fiscalYearId", UUID.randomUUID().toString());

    var errors = verifyGetWithParam(FISCAL_YEAR_HIERARCHY_ENDPOINT, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode(), params)
      .as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().getFirst().getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  static class ContextConfiguration {
    @Bean
    public FiscalYearHierarchyService fiscalYearHierarchyService() {
      return mock(FiscalYearHierarchyService.class);
    }
  }
}
