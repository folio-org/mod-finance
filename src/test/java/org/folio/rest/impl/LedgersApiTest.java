package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.RestTestUtils.verifyDeleteResponse;
import static org.folio.rest.util.RestTestUtils.verifyGet;
import static org.folio.rest.util.RestTestUtils.verifyGetWithParam;
import static org.folio.rest.util.RestTestUtils.verifyPostResponse;
import static org.folio.rest.util.RestTestUtils.verifyPut;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.LEDGER;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.vertx.core.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.ApiTestSuite;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.util.RestTestUtils;
import org.folio.rest.util.TestEntities;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerService;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.vertx.core.json.JsonObject;


public class LedgersApiTest {

  private static final Logger logger = LogManager.getLogger(LedgersApiTest.class);
  private static boolean runningOnOwn;

  @Autowired
  public LedgerService ledgerMockService;
  @Autowired
  public LedgerDetailsService currentFiscalYearMockService;
  @Autowired
  public AcqUnitsService acqUnitsService;

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(LedgersApiTest.ContextConfiguration.class);
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
    reset(ledgerMockService);
    reset(currentFiscalYearMockService);
    reset(acqUnitsService);
  }

  @Test
  void shouldCallCreateLedgerWhenCallPost() {

    Ledger ledger = new Ledger()
      .withName("Test")
      .withCode("Test")
      .withFiscalYearOneId(UUID.randomUUID().toString())
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE);

    when(ledgerMockService.createLedger(any(Ledger.class), any(RequestContext.class)))
      .thenAnswer(invocation -> succeededFuture(invocation.getArgument(0)));

    Ledger responseLedger = verifyPostResponse(LEDGER.getEndpoint(), ledger, APPLICATION_JSON, 201).as(Ledger.class);

    ArgumentCaptor<Ledger> ledgerArgumentCaptor = ArgumentCaptor.forClass(Ledger.class);
    verify(ledgerMockService).createLedger(ledgerArgumentCaptor.capture(), any(RequestContext.class));
    Ledger ledgerFromArgument = ledgerArgumentCaptor.getValue();

    assertEquals(ledgerFromArgument, responseLedger);
    assertThat(responseLedger, hasProperty("metadata"));
  }

  @Test
  void testGetLedgerByIdWithFiscalYearParam() {

    String ledgerId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger()
      .withId(ledgerId);

    when(ledgerMockService.retrieveLedgerWithTotals(anyString(), anyString(), any())).thenReturn(succeededFuture(ledger));

    Ledger resultLedger = verifyGetWithParam(LEDGER.getEndpointWithId(ledgerId), APPLICATION_JSON, OK.getStatusCode(), "fiscalYear",
      fiscalYearId).as(Ledger.class);

    assertEquals(ledger, resultLedger);
    verify(ledgerMockService).retrieveLedgerWithTotals(eq(ledgerId), eq(fiscalYearId), any(RequestContext.class));
  }

  @Test
  void testGetLedgerByIdWithSummaryEmptyFiscalYearParam() {
    logger.info("=== Test Get Ledger by id with summary, fiscalYear parameter not provided ===");

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger()
      .withId(ledgerId);

    when(ledgerMockService.retrieveLedgerWithTotals(anyString(), any(), any())).thenReturn(succeededFuture(ledger));

    Ledger resultLedger = verifyGet(LEDGER.getEndpointWithId(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(Ledger.class);

    assertEquals(ledger, resultLedger);
    verify(ledgerMockService).retrieveLedgerWithTotals(eq(ledgerId), isNull(), any(RequestContext.class));
  }

  @Test
  void testGetLedgerByIdWithSummaryInternalServerError() {
    logger.info("=== Test Get Ledger by id with summary, internal server error ===");
    Future<Ledger> errorFuture = Future.failedFuture(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(ledgerMockService.retrieveLedgerWithTotals(anyString(), anyString(), any())).thenReturn(errorFuture);
    String ledgerId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();

    Errors errors = verifyGetWithParam(LEDGER.getEndpointWithId(ledgerId), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode(), "fiscalYear",
      fiscalYearId).as(Errors.class);

    // Make sure that correct storage endpoint was used
    assertThat(errors.getErrors(), hasSize(1));
    Error expectedError = new Error().withMessage(INTERNAL_SERVER_ERROR.getReasonPhrase()).withCode(GENERIC_ERROR_CODE.getCode());
    assertEquals(expectedError, errors.getErrors().get(0));

  }

  @Test
  void testGetLedgersCollectionWithFiscalYear() {
    logger.info("=== Test Get collection of Ledgers records (with fiscalYearId parameter) ===");

    String fiscalYearId = UUID.randomUUID().toString();

    LedgersCollection ledgersCollection = new LedgersCollection();
    Ledger ledger1 = new Ledger()
      .withId(UUID.randomUUID().toString());
    Ledger ledger2 = new Ledger()
      .withId(UUID.randomUUID().toString());
    ledgersCollection.getLedgers().add(ledger1);
    ledgersCollection.getLedgers().add(ledger2);
    ledgersCollection.setTotalRecords(2);

    when(ledgerMockService.retrieveLedgersWithAcqUnitsRestrictionAndTotals(anyString(), anyInt(), anyInt(), anyString(), any())).thenReturn(succeededFuture(ledgersCollection));
    when(acqUnitsService.buildAcqUnitsCqlClause(any())).thenReturn(succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL));
    String query = "status==Active";
    int limit = 5;
    int offset = 1;

    Map<String, Object> params = new HashMap<>();
    params.put("query", query);
    params.put("fiscalYear", fiscalYearId);
    params.put("limit", limit);
    params.put("offset", offset);

    LedgersCollection response = verifyGetWithParam(TestEntities.LEDGER.getEndpoint(), APPLICATION_JSON, OK.getStatusCode(), params)
      .as(LedgersCollection.class);

    assertEquals(ledgersCollection, response);

    verify(ledgerMockService).retrieveLedgersWithAcqUnitsRestrictionAndTotals(eq(query), eq(offset), eq(limit), eq(fiscalYearId), any(RequestContext.class));

  }

  @Test
  void testGetLedger() {
    String fiscalYearId = UUID.randomUUID().toString();
    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    LedgersCollection ledgerCollection = new LedgersCollection().withLedgers(List.of(ledger)).withTotalRecords(1);
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledgerCollection));
    when(ledgerMockService.retrieveLedgersWithAcqUnitsRestrictionAndTotals(anyString(), anyInt(), anyInt(), anyString(), any())).thenReturn(succeededFuture(ledgerCollection));

    Map<String, Object> params = new HashMap<>();
    params.put("query", "status=Active");
    params.put("fiscalYear", fiscalYearId);
    params.put("limit", 10);
    params.put("offset", 10);

    RestTestUtils.verifyGetWithParam(TestEntities.LEDGER.getEndpoint(), APPLICATION_JSON, 200, params).as(LedgersCollection.class);
  }

  @Test
  void testGetLedgersCollectionWithFiscalYearInternalServerError() {
    logger.info("=== Test Get collection of Ledgers records (with fiscalYearId parameter) - Internal Server Error ===");

    Future<LedgersCollection> errorFuture = Future.failedFuture(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(ledgerMockService.retrieveLedgersWithAcqUnitsRestrictionAndTotals(anyString(), anyInt(), anyInt(), anyString(), any())).thenReturn(errorFuture);
    when(acqUnitsService.buildAcqUnitsCqlClause(any())).thenReturn(succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL));

    String query = "id==" + UUID.randomUUID();
    Map<String, Object> params = new HashMap<>();
    params.put("query", query);
    params.put("fiscalYear", UUID.randomUUID().toString());
    Errors errors = verifyGetWithParam(TestEntities.LEDGER.getEndpoint(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode(), params).as(Errors.class);
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
    assertThat(errors.getErrors().get(0).getMessage(), is("Internal Server Error"));
  }

  @Test
  void shouldReturnResponseWith204StatusCodeWhenPut() {
    Ledger ledger = new Ledger()
      .withId(UUID.randomUUID().toString())
      .withName("Test")
      .withCode("Test")
      .withFiscalYearOneId(UUID.randomUUID().toString())
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE);

    when(ledgerMockService.updateLedger(any(), any())).thenReturn(succeededFuture(null));

    verifyPut(LEDGER.getEndpointWithId(ledger.getId()), ledger, "", 204);

    verify(ledgerMockService).updateLedger(refEq(ledger, "metadata"), any(RequestContext.class));
  }

  @Test
  void shouldPopulateIdWhenPutWithEmptyIdInBody() {
    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger()
      .withName("Test")
      .withCode("Test")
      .withFiscalYearOneId(UUID.randomUUID().toString())
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE);

    when(ledgerMockService.updateLedger(any(), any())).thenReturn(succeededFuture(null));

    verifyPut(LEDGER.getEndpointWithId(ledgerId), ledger, "", 204);

    ArgumentCaptor<Ledger> ledgerArgumentCaptor = ArgumentCaptor.forClass(Ledger.class);
    verify(ledgerMockService).updateLedger(ledgerArgumentCaptor.capture(), any(RequestContext.class));

    Ledger ledgerFromArgument = ledgerArgumentCaptor.getValue();

    assertEquals(ledgerId, ledgerFromArgument.getId());
  }

  @Test
  void shouldReturnResponseWithIdMismatchErrorWhenPutWithDifferentIdInBodyAndRequestParameter() {
    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger()
      .withId(UUID.randomUUID().toString())
      .withName("Test")
      .withCode("Test")
      .withFiscalYearOneId(UUID.randomUUID().toString())
      .withLedgerStatus(Ledger.LedgerStatus.ACTIVE);

    Errors errors = verifyPut(LEDGER.getEndpointWithId(ledgerId), ledger, APPLICATION_JSON, 422).as(Errors.class);

    verify(ledgerMockService, never()).updateLedger(any(), any());

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError(), errors.getErrors().get(0));
  }

  @Test
  void shouldCallDeleteLedgerMethodWhenCallDeleteApi() {
    String ledgerId = UUID.randomUUID().toString();

    when(ledgerMockService.deleteLedger(anyString(), any())).thenReturn(succeededFuture(null));

    verifyDeleteResponse(LEDGER.getEndpointWithId(ledgerId), "", 204);

    verify(ledgerMockService).deleteLedger(eq(ledgerId), any(RequestContext.class));
  }

  /**
   * Define unit test specific beans to override actual ones
   */
  static class ContextConfiguration {

    @Bean
    public LedgerService ledgerService() {
      return mock(LedgerService.class);
    }

    @Bean
    public LedgerDetailsService currentFiscalYearService() {
      return mock(LedgerDetailsService.class);
    }

    @Bean AcqUnitsService acqUnitsService() {
      return mock(AcqUnitsService.class);
    }
  }

}
