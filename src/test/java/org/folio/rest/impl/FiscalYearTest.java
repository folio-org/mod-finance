package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEAR_INVALID_PERIOD;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.MockServer.getRqRsEntries;
import static org.folio.rest.util.TestConfig.*;
import static org.folio.rest.util.TestConstants.*;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.rest.util.TestEntities.*;
import static org.folio.services.configuration.ConfigurationEntriesService.DEFAULT_CURRENCY;
import static org.folio.services.ledger.LedgerDetailsService.SEARCH_CURRENT_FISCAL_YEAR_QUERY;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.jaxrs.resource.FinanceLedgers;
import org.folio.rest.util.HelperUtils;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.RestTestUtils;
import org.folio.rest.util.TestConfig;
import org.folio.rest.util.TestEntities;
import org.folio.services.configuration.ConfigurationEntriesService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FiscalYearTest {

  private RequestContext requestContext;

  private static final Logger logger = LogManager.getLogger(FiscalYearTest.class);

  private static boolean runningOnOwn;

  @InjectMocks
  private LedgerDetailsService ledgerDetailsService;

  @Mock
  private FiscalYearService fiscalYearService;

  @Mock
  private LedgerService ledgerService;

  @Mock
  private ConfigurationEntriesService configurationEntriesService;

  @BeforeEach
  public void initMocks() throws ExecutionException, InterruptedException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(ApplicationConfig.class);
    MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @AfterEach
  void afterEach() {
    clearServiceInteractions();
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
  void testPostFiscalYearWithInvalidCode() {
    logger.info("=== Test create FiscalYear with invalid FiscalYearCode===");

    FiscalYear fiscalYear = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYear.setCode("dcscs");
    RestTestUtils.verifyPostResponse(FISCAL_YEAR.getEndpoint(), fiscalYear, APPLICATION_JSON,
      422).as(Errors.class);
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
  void testPutFiscalYearWithInvalidPeriod() {
    logger.info("=== Test update FiscalYear with invalid period ===");

    JsonObject body = FISCAL_YEAR.getMockObject();

    body.remove(PERIOD_START);
    body.put(PERIOD_START, VALID_DATE_2021);
    body.remove(PERIOD_END);
    body.put(PERIOD_END, VALID_DATE_2020);

    Errors errors = RestTestUtils.verifyPut(FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body, APPLICATION_JSON, 422)
      .as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals(FISCAL_YEAR_INVALID_PERIOD.toError().getCode(), errors.getErrors().get(0).getCode());
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
        NO_CONTENT.getStatusCode());
  }

  @Test
  void testPostFiscalYearWithIfSystemCurrencyNotSetInConfigThenUSDAsDefault() {
    logger.info("=== Test create FiscalYear with currency not present in config===");

    Headers headers = RestTestUtils.prepareHeaders(TestConfig.X_OKAPI_URL, INVALID_CONFIG_X_OKAPI_TENANT, X_OKAPI_TOKEN);

    FiscalYear fiscalYear = RestTestUtils.verifyPostResponse(FISCAL_YEAR.getEndpoint(), FISCAL_YEAR.getMockObject(), headers, APPLICATION_JSON, CREATED.getStatusCode()).as(FiscalYear.class);

    assertThat(fiscalYear.getCurrency(), equalTo(DEFAULT_CURRENCY));
  }

  @Test
  void testOneFiscalYear() throws ParseException {

    logger.info("=== Test Get Current Fiscal Year - One Fiscal Year ===");

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);

    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("31/12/" + year);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withSeries("FY").withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear));

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(completedFuture("America/Los_Angeles")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext).join();
    //Then
    assertThat(actFY.getId(), Matchers.equalTo(firstfiscalYear.getId()));
    LocalDate now = Instant.now().atZone(ZoneId.of("America/Los_Angeles")).toLocalDate();
    String expQuery = String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, "FY", now);
    verify(fiscalYearService).getFiscalYears(eq(expQuery), eq(0), eq(3), eq(requestContext));
  }

  @Test
  void testTwoOverlappedFiscalYears() throws ParseException {
    logger.info("=== Test Get Current Fiscal Year - Two Overlapped Fiscal Years ===");
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);

    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("31/12/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("03/01/" + year);
    Date sEndDate = sdf.parse("31/12/" + year);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstFiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear secFiscalYear = new FiscalYear().withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstFiscalYear, secFiscalYear));

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(firstFiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(completedFuture("UTC")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext).join();
    //Then
    assertThat(actFY.getId(), Matchers.equalTo(secFiscalYear.getId()));
  }

  @Test
  void testTwoNonOverlappedFiscalYears() throws ParseException {

    logger.info("=== Test Get Current Fiscal Year - Two Non-Overlapped Fiscal Years ===");
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int nextYear = year + 1;
    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("03/31/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("01/01/" + nextYear);
    Date sEndDate = sdf.parse("31/12/" + nextYear);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear secfiscalYear = new FiscalYear().withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear, secfiscalYear));

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(completedFuture("UTC")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext).join();
    //Then
    assertThat(actFY.getId(), Matchers.equalTo(firstfiscalYear.getId()));
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

  @Test
  void testPutFiscalYearWithInvalidCode() {
    logger.info("=== Test put FiscalYear with invalid FiscalYearCode===");

    JsonObject body = FISCAL_YEAR.getMockObject();
    body.put("code","test");

    RestTestUtils.verifyPut(FISCAL_YEAR.getEndpointWithId((String) body.remove(ID)), body, "", 422);
  }

  private String getCurrentFiscalYearEndpoint(String ledgerId) {
    return HelperUtils.getEndpoint(FinanceLedgers.class) + "/" + ledgerId + "/current-fiscal-year";
  }

}
