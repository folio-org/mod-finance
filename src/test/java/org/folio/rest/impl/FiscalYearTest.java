package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.MockServer.getRqRsEntries;
import static org.folio.rest.util.TestConfig.clearServiceInteractions;
import static org.folio.rest.util.TestConfig.deployVerticle;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestConstants.EMPTY_CONFIG_X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.ERROR_X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.INVALID_CONFIG_X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.SERIES_DOES_NOT_EXIST;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestEntities.FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.LEDGER;
import static org.folio.rest.util.TestUtils.convertLocalDateTimeToDate;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.resource.FinanceLedgers;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.HelperUtils;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.RestTestUtils;
import org.folio.rest.util.TestConfig;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class FiscalYearTest {
  private static final Logger logger = LogManager.getLogger(FiscalYearTest.class);
  private static boolean runningOnOwn;

  @BeforeAll
  static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(ApplicationConfig.class);
  }

  @AfterEach
  void afterEach() {
    clearServiceInteractions();
  }

  @AfterAll
  static void after() {
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
  }

  @Test
  void testPostFiscalYearWithCurrency() {
    logger.info("=== Test create FiscalYear with Currency record populated===");

    Response response = RestTestUtils.verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), APPLICATION_JSON,
        CREATED.getStatusCode());
    assertThat(response.getBody()
      .as(FiscalYear.class)
      .getCurrency(), notNullValue());
  }

  @Test
  void testPostFiscalYearNoConfigurationEntry() {
    logger.info("=== Test create FiscalYear with default currency record ===");
    Headers headers = RestTestUtils.prepareHeaders(TestConfig.X_OKAPI_URL, EMPTY_CONFIG_X_OKAPI_TENANT, X_OKAPI_TOKEN);
    Response response = RestTestUtils.verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), headers, APPLICATION_JSON,
        CREATED.getStatusCode());
    assertThat(response.getBody()
      .as(FiscalYear.class)
      .getCurrency(), notNullValue());
  }

  @Test
  void testPostFiscalYearErrorConfigurationEntry() {
    logger.info("=== Test create FiscalYear with Error ===");
    Headers headers = RestTestUtils.prepareHeaders(TestConfig.X_OKAPI_URL, ERROR_X_OKAPI_TENANT, X_OKAPI_TOKEN);
    RestTestUtils.verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), headers, APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @Test
  void testPutFiscalYearWithCurrency() {
    logger.info("=== Test update FiscalYear with Currency record populated===");

    JsonObject body = FISCAL_YEAR.getMockObject();
    RestTestUtils.verifyPut(FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body, "", NO_CONTENT.getStatusCode());

    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(HttpMethod.PUT, FISCAL_YEAR.name());
    assertThat(rqRsPostFund.get(0)
      .getString("currency"), notNullValue());
  }

  @Test
  void testPutFiscalYearNoConfigurationEntry() {
    logger.info("=== Test update FiscalYear with default currency record ===");

    Headers headers = RestTestUtils.prepareHeaders(TestConfig.X_OKAPI_URL, EMPTY_CONFIG_X_OKAPI_TENANT, X_OKAPI_TOKEN);
    JsonObject body = FISCAL_YEAR.getMockObject();

    RestTestUtils.verifyPut(FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body.toString(), headers, "", NO_CONTENT.getStatusCode());

    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(HttpMethod.PUT, FISCAL_YEAR.name());
    assertThat(rqRsPostFund.get(0)
      .getString("currency"), notNullValue());
    assertEquals("USD", rqRsPostFund.get(0)
      .getString("currency"));
  }

  @Test
  void testPutFiscalYearErrorConfigurationEntry() {
    logger.info("=== Test create FiscalYear with Error ===");
    Headers headers = RestTestUtils.prepareHeaders(TestConfig.X_OKAPI_URL, ERROR_X_OKAPI_TENANT, X_OKAPI_TOKEN);
    JsonObject body = FISCAL_YEAR.getMockObject();

    RestTestUtils.verifyPut(FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body.toString(), headers, "",
        INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @Test
  void testPostFiscalYearWithInvalidConfig() {
    logger.info("=== Test create FiscalYear with currency not present in config===");

    Headers headers = RestTestUtils.prepareHeaders(TestConfig.X_OKAPI_URL, INVALID_CONFIG_X_OKAPI_TENANT, X_OKAPI_TOKEN);

    Errors errors = RestTestUtils.verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), headers, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    assertThat(errors.getErrors()
      .get(0)
      .getCode(), equalTo(ErrorCodes.CURRENCY_NOT_FOUND.getCode()));
  }

  @Test
  void testOneFiscalYear() {

    logger.info("=== Test Get Current Fiscal Year - One Fiscal Year ===");

    FiscalYear year = new FiscalYear().withId(UUID.randomUUID().toString());

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(year.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(year));

    FiscalYear response = RestTestUtils.verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(year.getId()));
  }

  @Test
  void testTwoOverlappedFiscalYears() {

    logger.info("=== Test Get Current Fiscal Year - Two Overlapped Fiscal Years ===");

    LocalDateTime now = LocalDateTime.now();

    FiscalYear firstYear = new FiscalYear().withId(UUID.randomUUID().toString());
    firstYear.setPeriodStart(convertLocalDateTimeToDate(now.minusDays(10)));
    firstYear.setPeriodEnd(convertLocalDateTimeToDate(now.plusDays(10)));

    FiscalYear secondYear = new FiscalYear().withId(UUID.randomUUID().toString());
    secondYear.setPeriodStart(convertLocalDateTimeToDate(now.minusDays(5)));
    secondYear.setPeriodEnd(convertLocalDateTimeToDate(now.plusDays(10)));

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstYear.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(firstYear));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(secondYear));

    FiscalYear response = RestTestUtils.verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(secondYear.getId()));
  }

  @Test
  void testTwoNonOverlappedFiscalYears() {

    logger.info("=== Test Get Current Fiscal Year - Two Non-Overlapped Fiscal Years ===");

    LocalDateTime now = LocalDateTime.now();

    FiscalYear firstYear = new FiscalYear().withId(UUID.randomUUID().toString());
    firstYear.setPeriodStart(convertLocalDateTimeToDate(now.minusDays(10)));
    firstYear.setPeriodEnd(convertLocalDateTimeToDate(now));

    FiscalYear secondYear = new FiscalYear().withId(UUID.randomUUID().toString());
    secondYear.setPeriodStart(convertLocalDateTimeToDate(now.plusDays(10)));
    secondYear.setPeriodEnd(convertLocalDateTimeToDate(now.plusDays(20)));

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstYear.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(firstYear));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(secondYear));

    FiscalYear response = RestTestUtils.verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(firstYear.getId()));
  }

  @Test
  void testFiscalYearNotFound() {

    logger.info("=== Test Get Current Fiscal Year - Fiscal Year Not Found ===");

    FiscalYear year = new FiscalYear().withId(UUID.randomUUID().toString());
    year.setSeries(SERIES_DOES_NOT_EXIST);

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(year.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(year));

    RestTestUtils.verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, NOT_FOUND.getStatusCode());
  }

  @Test
  void testGetFiscalYearLedgerNotFound() {
    RestTestUtils.verifyGet(getCurrentFiscalYearEndpoint(UUID.randomUUID().toString()), APPLICATION_JSON, NOT_FOUND.getStatusCode());
  }

  @Test
  void testPostRecordEmptySeriesFY() {
    logger.info("=== Test create {} record empty series for FY and it should calculate series ===", TestEntities.FISCAL_YEAR);
    JsonObject record = TestEntities.FISCAL_YEAR.getMockObject();
    record.putNull("series");

    FiscalYear fiscalYear = RestTestUtils.verifyPostResponse(TestEntities.FISCAL_YEAR.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode()).as(FiscalYear.class);
    assertThat(fiscalYear.getSeries(), is(notNullValue()));
  }

  @Test
  void testUpdateRecordEmptySeriesFY() {
    logger.info("=== Test update {} record with empty series for FY and it should calculate series ===", TestEntities.FISCAL_YEAR);

    JsonObject body = TestEntities.FISCAL_YEAR.getMockObject();
    body.putNull("series");

    RestTestUtils.verifyPut(TestEntities.FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body, "", NO_CONTENT.getStatusCode());
    assertThat(getRqRsEntries(HttpMethod.PUT, TestEntities.FISCAL_YEAR.toString()).get(0).getString("series"), is(notNullValue()));
  }

  private String getCurrentFiscalYearEndpoint(String ledgerId) {
    return HelperUtils.getEndpoint(FinanceLedgers.class) + "/" + ledgerId + "/current-fiscal-year";
  }

}
