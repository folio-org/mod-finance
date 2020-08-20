package org.folio.rest.impl;

import static java.time.temporal.ChronoUnit.DAYS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.TestConfig.deployVerticle;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestConstants.ERROR_X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.ID_DOES_NOT_EXIST;
import static org.folio.rest.util.TestConstants.ID_FOR_INTERNAL_SERVER_ERROR;
import static org.folio.rest.util.TestConstants.SERIES_DOES_NOT_EXIST;
import static org.folio.rest.util.TestConstants.SERIES_INTERNAL_SERVER_ERROR;
import static org.folio.rest.util.TestConstants.VALID_UUID;
import static org.folio.rest.util.TestUtils.convertLocalDateTimeToDate;
import static org.folio.rest.util.TestConfig.clearServiceInteractions;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.MockServer.getQueryParams;
import static org.folio.rest.util.MockServer.getRecordById;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.folio.rest.util.TestEntities.FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.FUND;
import static org.folio.rest.util.TestEntities.GROUP;
import static org.folio.rest.util.TestEntities.GROUP_FUND_FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.LEDGER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.util.TestConfig;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.RestTestUtils;
import org.folio.rest.util.TestEntities;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.http.Headers;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FundsApiTest {

  private static final Logger logger = LoggerFactory.getLogger(FundsApiTest.class);
  public static final String FUND_FIELD_NAME = "fund";
  public static final String GROUP_ID_FIELD_NAME = "groupId";
  public static final String GROUP_ID_FOR_DELETION = "f33ed99b-852a-4f90-9891-5efe0feab165";
  public static final String GROUP_ID = "e9285a1c-1dfc-4380-868c-e74073003f43";
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
  void testGetCompositeFundById() {

    logger.info("=== Test Get Composite Fund record by id ===");

    CompositeFund compositeFund = RestTestUtils.verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(CompositeFund.class);

    // Make sure that correct storage endpoint was used
    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    assertThat(getRecordById(FUND.name()), hasSize(1));
    assertThat(compositeFund.getGroupIds(), hasSize(1));
  }

  @Test
  void testGetCompositeFundByIdFiscalYearsNotFound() {

    logger.info("=== Test Get Composite Fund record by id, current Fiscal Year not found ===");

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setSeries(SERIES_DOES_NOT_EXIST);
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    CompositeFund compositeFund = RestTestUtils.verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(CompositeFund.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);

    assertThat(getRecordById(FUND.name()), hasSize(1));
    assertThat(compositeFund.getGroupIds(), hasSize(0));
  }

  @Test
  void testGetCompositeFundByIdLedgerByIdError() {

    logger.info("=== Test Get Composite Fund record by id, get Ledger by id Internal server error ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);
    fund.setLedgerId(ID_FOR_INTERNAL_SERVER_ERROR);
    addMockEntry(FUND.name(), JsonObject.mapFrom(fund));

    RestTestUtils.verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, FUND, 1);
  }

  @Test
  void testGetCompositeFundByIdFiscalYearByIdError() {

    logger.info("=== Test Get Composite Fund record by id, get Fiscal Year by id Internal server error ===");

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setFiscalYearOneId(ID_FOR_INTERNAL_SERVER_ERROR);
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    RestTestUtils.verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FUND, 1);
  }

  @Test
  void testGetCompositeFundByIdFiscalYearsByQueryError() {

    logger.info("=== Test Get Composite Fund record by id, get Fiscal Years by query Internal server error ===");

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setSeries(SERIES_INTERNAL_SERVER_ERROR);
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    RestTestUtils.verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FUND, 1);
  }

  @Test
  void testGetCompositeFundByIdGroupFundFyByQueryError() {

    logger.info("=== Test Get Composite Fund record by id, get Group Fund Fiscal Year by query Internal server error ===");

    LocalDateTime now = LocalDateTime.now();

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setPeriodStart(convertLocalDateTimeToDate(now.minusDays(10)));
    fiscalYearOne.setPeriodEnd(convertLocalDateTimeToDate(now.plusDays(10)));

    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    FiscalYear fiscalYear = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYear.setId(ID_FOR_INTERNAL_SERVER_ERROR);
    fiscalYear.setPeriodStart(convertLocalDateTimeToDate(now.minusDays(5)));
    fiscalYear.setPeriodEnd(convertLocalDateTimeToDate(now.plusDays(10)));

    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYear));

    RestTestUtils.verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    verifyRsEntitiesQuantity(HttpMethod.GET, FUND, 1);
  }

  @Test
  void testGetCompositeFundByIdServerError() {
    logger.info("=== Test Get Composite Fund record by id - Internal Server Error ===");

    RestTestUtils.verifyGet(FUND.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode())
      .as(Errors.class);
  }

  @Test
  void testGetCompositeFundByIdNotFound() {
    logger.info("=== Test Get Composite Fund record by id - Not Found ===");

    RestTestUtils.verifyGet(FUND.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);
  }

  @Test
  void testPostCompositeFundEmptyGroupIds() {
    logger.info("=== Test create Composite Fund record ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    CompositeFund compositeFund = RestTestUtils.verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode()).as(CompositeFund.class);

    assertThat(compositeFund.getFund(), hasProperty(ID));

    compareRecordWithSentToStorage(HttpMethod.POST, JsonObject.mapFrom(fundRecord), FUND);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 0);
  }

  @Test
  void testPostCompositeFundWithGroupIds() {
    logger.info("=== Test create Composite Fund record ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());
    record.getGroupIds().add(UUID.randomUUID().toString());

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fundRecord.getLedgerId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setId(ledger.getFiscalYearOneId());
    fiscalYearOne.setPeriodStart(Date.from(Instant.now().minus(1, DAYS)));
    fiscalYearOne.setPeriodEnd(Date.from(Instant.now().plus(1, DAYS)));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    RestTestUtils.verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode());

    compareRecordWithSentToStorage(HttpMethod.POST, JsonObject.mapFrom(fundRecord), FUND);
    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, record.getGroupIds().size());

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);
  }

  @Test
  void testPostCompositeFundWithInternalServerErrorOnGetLedger() {
    logger.info("=== Test create Composite Fund record, get current Fiscal Year Internal Sever Error ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    fundRecord.setLedgerId(ID_FOR_INTERNAL_SERVER_ERROR);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());
    RestTestUtils.verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 0);
  }

  @Test
  void testPostCompositeFundWithInternalServerErrorOnGetFiscalYearById() {
    logger.info("=== Test create Composite Fund record, get Fiscal Year by id Internal Sever Error ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fundRecord.getLedgerId());
    ledger.setFiscalYearOneId(ID_FOR_INTERNAL_SERVER_ERROR);
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    record.getGroupIds().add(UUID.randomUUID().toString());
    RestTestUtils.verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);
  }


  @Test
  void testPostCompositeFundWithEmptyResultOnSearchFiscalYear() {
    logger.info("=== Test create Composite Fund record current Fiscal Year not found ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fundRecord.getLedgerId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setSeries(SERIES_DOES_NOT_EXIST);
    fiscalYearOne.setId(ledger.getFiscalYearOneId());
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    Errors errors = RestTestUtils.verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, 422).as(Errors.class);
    verifyCurrentFYQuery(fiscalYearOne);

    assertThat(errors.getErrors().get(0).getCode(), equalTo(FISCAL_YEARS_NOT_FOUND.getCode()));

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
  }

  @Test
  void testPostCompositeFundWithInternalServerErrorOnGroupFundFYCreation() {
    logger.info("=== Test create Composite Fund record, Internal Server Error upon POST groupFundFiscalYear ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fundRecord.getLedgerId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setId(ledger.getFiscalYearOneId());
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    Headers headers = RestTestUtils.prepareHeaders(TestConfig.X_OKAPI_URL, ERROR_X_OKAPI_TENANT);
    RestTestUtils.verifyPostResponse(FUND.getEndpoint(), record,  headers, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
  }

  @Test
  void testUpdateRecordWithAssignUnassignGroupIds() {
    logger.info("=== Test update Composite Fund record - assign and unassign group ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fund.getLedgerId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setId(ledger.getFiscalYearOneId());
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    CompositeFund record = new CompositeFund().withFund(fund);

    Group group = new Group();
    group.setId(GROUP_ID);
    addMockEntry(GROUP.name(), JsonObject.mapFrom(group));

    record.getGroupIds().add(group.getId());

    JsonObject body = JsonObject.mapFrom(record);

    body.getJsonObject(FUND_FIELD_NAME).put(FUND.getUpdatedFieldName(), FUND.getUpdatedFieldValue());

    String id = body.getJsonObject(FUND_FIELD_NAME).getString(ID);
    RestTestUtils.verifyPut(FUND.getEndpointWithId(id), body, "", NO_CONTENT.getStatusCode());

    JsonObject expected = JsonObject.mapFrom(body).getJsonObject(FUND_FIELD_NAME);
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, FUND);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, BUDGET, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    List<JsonObject> createdGroupFundFiscalYears = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(createdGroupFundFiscalYears, hasSize(1));
    assertThat(createdGroupFundFiscalYears.get(0).getString(GROUP_ID_FIELD_NAME), equalTo(GROUP_ID));

    List<JsonObject> deletedGroupFundFiscalYears = MockServer.getRqRsEntries(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(deletedGroupFundFiscalYears, hasSize(1));
    assertThat(GROUP_FUND_FISCAL_YEAR.getMockObject().getString(GROUP_ID_FIELD_NAME), equalTo(GROUP_ID_FOR_DELETION));

  }

  @Test
  void testUpdateRecordWithAssignGroupIds() {
    logger.info("=== Test update Composite Fund record - Assign Group ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fund.getLedgerId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setId(ledger.getFiscalYearOneId());
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    CompositeFund record = new CompositeFund().withFund(fund);

    Group group = new Group();
    group.setId(GROUP_ID);
    addMockEntry(GROUP.name(), JsonObject.mapFrom(group));

    record.getGroupIds().add(group.getId());
    record.getGroupIds().add(GROUP_ID_FOR_DELETION);

    JsonObject body = JsonObject.mapFrom(record);

    body.getJsonObject(FUND_FIELD_NAME).put(FUND.getUpdatedFieldName(), FUND.getUpdatedFieldValue());

    String id = body.getJsonObject(FUND_FIELD_NAME).getString(ID);
    RestTestUtils.verifyPut(FUND.getEndpointWithId(id), body, "", NO_CONTENT.getStatusCode());

    JsonObject expected = JsonObject.mapFrom(body).getJsonObject(FUND_FIELD_NAME);
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, FUND);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, BUDGET, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    List<JsonObject> createdGroupFundFiscalYears = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(createdGroupFundFiscalYears, hasSize(1));
    assertThat(createdGroupFundFiscalYears.get(0).getString(GROUP_ID_FIELD_NAME), equalTo(GROUP_ID));

    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 0);

  }

  @Test
  void testUpdateRecordWithUnassignGroupIds() {
    logger.info("=== Test update Composite Fund record - Unassign Group ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fund.getLedgerId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setId(ledger.getFiscalYearOneId());
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    CompositeFund record = new CompositeFund().withFund(fund);

    JsonObject body = JsonObject.mapFrom(record);

    body.getJsonObject(FUND_FIELD_NAME).put(FUND.getUpdatedFieldName(), FUND.getUpdatedFieldValue());

    String id = body.getJsonObject(FUND_FIELD_NAME).getString(ID);
    RestTestUtils.verifyPut(FUND.getEndpointWithId(id), body, "", NO_CONTENT.getStatusCode());

    JsonObject expected = JsonObject.mapFrom(body).getJsonObject(FUND_FIELD_NAME);
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, FUND);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    verifyRsEntitiesQuantity(HttpMethod.GET, BUDGET, 0);
    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    List<JsonObject> deletedGroupFundFiscalYears = MockServer.getRqRsEntries(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(deletedGroupFundFiscalYears, hasSize(1));
    assertThat(GROUP_FUND_FISCAL_YEAR.getMockObject().getString(GROUP_ID_FIELD_NAME), equalTo(GROUP_ID_FOR_DELETION));

  }

  @Test
  void testUpdateRecordWithGroupIdsGroupNotFound() {
    logger.info("=== Test update Composite Fund record - Group Not Found ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fund.getLedgerId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setId(ledger.getFiscalYearOneId());
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    CompositeFund record = new CompositeFund().withFund(fund);

    Group group = new Group();
    group.setId(UUID.randomUUID().toString());

    record.getGroupIds().add(group.getId());

    JsonObject body = JsonObject.mapFrom(record);

    body.getJsonObject(FUND_FIELD_NAME).put(FUND.getUpdatedFieldName(), FUND.getUpdatedFieldValue());

    String id = body.getJsonObject(FUND_FIELD_NAME).getString(ID);
    RestTestUtils.verifyPut(FUND.getEndpointWithId(id), body, "", 422);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 0);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);
    verifyRsEntitiesQuantity(HttpMethod.GET, BUDGET, 0);
    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 0);

  }

  @Test
  void testUpdateRecordFiscalYearNotFound() {
    logger.info("=== Test update Composite Fund record - Fiscal Year Not Found ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fund.getLedgerId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setSeries(SERIES_DOES_NOT_EXIST);
    fiscalYearOne.setId(ledger.getFiscalYearOneId());
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    CompositeFund record = new CompositeFund().withFund(fund);
    record.getGroupIds().add(UUID.randomUUID().toString());
    record.getGroupIds().add(UUID.randomUUID().toString());

    JsonObject body = JsonObject.mapFrom(record);

    body.getJsonObject(FUND_FIELD_NAME).put(FUND.getUpdatedFieldName(), FUND.getUpdatedFieldValue());

    String id = body.getJsonObject(FUND_FIELD_NAME).getString(ID);
    RestTestUtils.verifyPut(FUND.getEndpointWithId(id), body, "", 422);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 0);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, BUDGET, 0);
    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 0);

  }

  @Test
  void testUpdateRecordLedgerNotFound() {
    logger.info("=== Test update Composite Fund record - Ledger Internal Server Error ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);
    fund.setLedgerId(ID_FOR_INTERNAL_SERVER_ERROR);

    CompositeFund record = new CompositeFund().withFund(fund);
    record.getGroupIds().add(UUID.randomUUID().toString());

    JsonObject body = JsonObject.mapFrom(record);

    body.getJsonObject(FUND_FIELD_NAME).put(FUND.getUpdatedFieldName(), FUND.getUpdatedFieldValue());

    String id = body.getJsonObject(FUND_FIELD_NAME).getString(ID);
    RestTestUtils.verifyPut(FUND.getEndpointWithId(id), body, "", INTERNAL_SERVER_ERROR.getStatusCode());

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 0);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 0);
    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
    verifyRsEntitiesQuantity(HttpMethod.GET, BUDGET, 0);
    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 0);

  }

  @Test
  void testUpdateRecordServerError() {
    logger.info("=== Test update Composite Fund record - Internal Server Error ===");

    JsonObject body = FUND.getMockObject();
    body.put(ID, ID_FOR_INTERNAL_SERVER_ERROR);
    CompositeFund compositeFund = new CompositeFund().withFund(body.mapTo(Fund.class));
    RestTestUtils.verifyPut(FUND.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), compositeFund, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode())
      .as(Errors.class);
  }

  @Test
  void testUpdateRecordNotFound() {
    logger.info("=== Test update Composite Fund record - Not Found ===");

    JsonObject body = FUND.getMockObject();
    body.put(ID, ID_DOES_NOT_EXIST);
    CompositeFund compositeFund = new CompositeFund().withFund(body.mapTo(Fund.class));
    RestTestUtils.verifyPut(FUND.getEndpointWithId(ID_DOES_NOT_EXIST), compositeFund, APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);
  }

  @Test
  void testUpdateRecordIdMismatch() {
    logger.info("=== Test update Composite Fund record - Path and body id mismatch ===");

    JsonObject body = FUND.getMockObject();
    CompositeFund compositeFund = new CompositeFund().withFund(body.mapTo(Fund.class));
    Errors errors = RestTestUtils.verifyPut(FUND.getEndpointWithId(VALID_UUID), compositeFund, APPLICATION_JSON, 422)
      .as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0), IsEqual.equalTo(ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()));
  }

  @Test
  void testDeleteRecord() {
    logger.info("=== Test delete Composite Fund record ===");

    RestTestUtils.verifyDeleteResponse(FUND.getEndpointWithDefaultId(), "", NO_CONTENT.getStatusCode());

    verifyRsEntitiesQuantity(HttpMethod.DELETE, FUND, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 1);
    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 1);
  }

  @Test
  void testDeleteRecordServerError() {
    logger.info("=== Test delete Composite Fund record - Internal Server Error ===");

    RestTestUtils.verifyDeleteResponse(FUND.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @Test
  void testDeleteRecordNotFound() {
    logger.info("=== Test delete Composite Fund record - Not Found ===");

    RestTestUtils.verifyDeleteResponse(FUND.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode())
      .as(Errors.class);
  }

  private void compareRecordWithSentToStorage(HttpMethod method, JsonObject record, TestEntities testEntity) {
    // Verify that record sent to storage is the same as in response
    List<JsonObject> rqRsEntries = MockServer.getRqRsEntries(method, testEntity.name());
    assertThat(rqRsEntries, hasSize(1));

    // remove "metadata" before comparing
    JsonObject entry = rqRsEntries.get(0);
    entry.remove("metadata");
    Object recordToStorage = entry.mapTo(testEntity.getClazz());

    assertThat(recordToStorage, equalTo(record.mapTo(testEntity.getClazz())));
  }

  private void verifyCurrentFYQuery(FiscalYear fiscalYearOne) {
    String query = getQueryParams(FISCAL_YEAR.name()).get(0);
    String now = LocalDate.now(Clock.systemUTC()).toString();
    assertThat(query, containsString(fiscalYearOne.getSeries()));
    assertThat(query, containsString(now));
  }

  private void verifyRsEntitiesQuantity(HttpMethod httpMethod, TestEntities entity, int expectedQuantity) {
    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(httpMethod, entity.name());
    assertThat(rqRsPostFund, hasSize(expectedQuantity));
  }
}
