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
import static org.folio.rest.util.TestEntities.FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.LEDGER;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.resource.FinanceLedgers;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.HelperUtils;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.Test;

public class FiscalYearTest extends ApiTestBase {
  private static final Logger logger = LoggerFactory.getLogger(FiscalYearTest.class);

  @Test
  public void testPostFiscalYearWithCurrency() {
    logger.info("=== Test create FiscalYear with Currency record populated===");

    Response response = verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), APPLICATION_JSON,
        CREATED.getStatusCode());
    assertThat(response.getBody()
      .as(FiscalYear.class)
      .getCurrency(), notNullValue());
  }

  @Test
  public void testPostFiscalYearNoConfigurationEntry() {
    logger.info("=== Test create FiscalYear with default currency record ===");
    Headers headers = prepareHeaders(X_OKAPI_URL, EMPTY_CONFIG_X_OKAPI_TENANT, X_OKAPI_TOKEN);
    Response response = verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), headers, APPLICATION_JSON,
        CREATED.getStatusCode());
    assertThat(response.getBody()
      .as(FiscalYear.class)
      .getCurrency(), notNullValue());
  }

  @Test
  public void testPostFiscalYearErrorConfigurationEntry() {
    logger.info("=== Test create FiscalYear with Error ===");
    Headers headers = prepareHeaders(X_OKAPI_URL, ERROR_X_OKAPI_TENANT, X_OKAPI_TOKEN);
    verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), headers, APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @Test
  public void testPutFiscalYearWithCurrency() {
    logger.info("=== Test update FiscalYear with Currency record populated===");

    JsonObject body = FISCAL_YEAR.getMockObject();
    verifyPut(FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body, "", NO_CONTENT.getStatusCode());

    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(HttpMethod.PUT, FISCAL_YEAR.name());
    assertThat(rqRsPostFund.get(0)
      .getString("currency"), notNullValue());
  }

  @Test
  public void testPutFiscalYearNoConfigurationEntry() {
    logger.info("=== Test update FiscalYear with default currency record ===");

    Headers headers = prepareHeaders(X_OKAPI_URL, EMPTY_CONFIG_X_OKAPI_TENANT, X_OKAPI_TOKEN);
    JsonObject body = FISCAL_YEAR.getMockObject();

    verifyPut(FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body.toString(), headers, "", NO_CONTENT.getStatusCode());

    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(HttpMethod.PUT, FISCAL_YEAR.name());
    assertThat(rqRsPostFund.get(0)
      .getString("currency"), notNullValue());
    assertEquals("USD", rqRsPostFund.get(0)
      .getString("currency"));
  }

  @Test
  public void testPutFiscalYearErrorConfigurationEntry() {
    logger.info("=== Test create FiscalYear with Error ===");
    Headers headers = prepareHeaders(X_OKAPI_URL, ERROR_X_OKAPI_TENANT, X_OKAPI_TOKEN);
    JsonObject body = FISCAL_YEAR.getMockObject();

    verifyPut(FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body.toString(), headers, "",
        INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @Test
  public void testPostFiscalYearWithInvalidConfig() {
    logger.info("=== Test create FiscalYear with currency not present in config===");

    Headers headers = prepareHeaders(X_OKAPI_URL, INVALID_CONFIG_X_OKAPI_TENANT, X_OKAPI_TOKEN);

    Errors errors = verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), headers, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    assertThat(errors.getErrors()
      .get(0)
      .getCode(), equalTo(ErrorCodes.CURRENCY_NOT_FOUND.getCode()));
  }

  @Test
  public void testOneFiscalYear() {

    logger.info("=== Test Get Current Fiscal Year - One Fiscal Year ===");

    FiscalYear year = new FiscalYear().withId(UUID.randomUUID().toString());

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(year.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(year));

    FiscalYear response = verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(year.getId()));
  }

  @Test
  public void testTwoOverlappedFiscalYears() {

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

    FiscalYear response = verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(secondYear.getId()));
  }

  @Test
  public void testTwoNonOverlappedFiscalYears() {

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

    FiscalYear response = verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(firstYear.getId()));
  }

  @Test
  public void testFiscalYearNotFound() {

    logger.info("=== Test Get Current Fiscal Year - Fiscal Year Not Found ===");

    FiscalYear year = new FiscalYear().withId(UUID.randomUUID().toString());
    year.setSeries(SERIES_DOES_NOT_EXIST);

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(year.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(year));

    verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, NOT_FOUND.getStatusCode());
  }

  @Test
  public void testGetFiscalYearLedgerNotFound() {
    verifyGet(getCurrentFiscalYearEndpoint(UUID.randomUUID().toString()), APPLICATION_JSON, NOT_FOUND.getStatusCode());
  }

  @Test
  public void testPostRecordEmptySeriesFY() throws IOException {
    logger.info("=== Test create {} record empty series for FY and it should calculate series ===", TestEntities.FISCAL_YEAR);
    JsonObject record = TestEntities.FISCAL_YEAR.getMockObject();
    record.putNull("series");

    FiscalYear fiscalYear = verifyPostResponse(TestEntities.FISCAL_YEAR.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode()).as(FiscalYear.class);
    assertThat(fiscalYear.getSeries(), is(notNullValue()));
  }

  @Test
  public void testUpdateRecordEmptySeriesFY() {
    logger.info("=== Test update {} record with empty series for FY and it should calculate series ===", TestEntities.FISCAL_YEAR);

    JsonObject body = TestEntities.FISCAL_YEAR.getMockObject();
    body.putNull("series");

    verifyPut(TestEntities.FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body, "", NO_CONTENT.getStatusCode());
    assertThat(getRqRsEntries(HttpMethod.PUT, TestEntities.FISCAL_YEAR.toString()).get(0).getString("series"), is(notNullValue()));
  }

  private String getCurrentFiscalYearEndpoint(String ledgerId) {
    return HelperUtils.getEndpoint(FinanceLedgers.class) + "/" + ledgerId + "/current-fiscal-year";
  }

}
