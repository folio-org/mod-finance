package org.folio.rest.impl;

import static java.time.temporal.ChronoUnit.DAYS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.HelperUtils.getFiscalYearDuration;
import static org.folio.rest.util.MockServer.ERROR_X_OKAPI_TENANT;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.MockServer.getQueryParams;
import static org.folio.rest.util.MockServer.getRecordById;
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

import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.TestEntities;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import io.restassured.http.Headers;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FundsTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(FundsTest.class);
  public static final String FUND_FIELD_NAME = "fund";
  public static final String GROUP_ID_FIELD_NAME = "groupId";
  public static final String GROUP_ID_FOR_DELETION = "f33ed99b-852a-4f90-9891-5efe0feab165";
  public static final String GROUP_ID = "e9285a1c-1dfc-4380-868c-e74073003f43";

  @Test
  public void testGetCompositeFundById() {

    logger.info("=== Test Get Composite Fund record by id ===");

    CompositeFund compositeFund = verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(CompositeFund.class);

    // Make sure that correct storage endpoint was used
    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    assertThat(getRecordById(FUND.name()), hasSize(1));
    assertThat(compositeFund.getGroupIds(), hasSize(1));
  }

  @Test
  public void testGetCompositeFundByIdFiscalYearsNotFound() {

    logger.info("=== Test Get Composite Fund record by id, current Fiscal Year not found ===");

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setSeries(SERIES_DOES_NOT_EXIST);
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    CompositeFund compositeFund = verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(CompositeFund.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);

    assertThat(getRecordById(FUND.name()), hasSize(1));
    assertThat(compositeFund.getGroupIds(), hasSize(0));
  }

  @Test
  public void testGetCompositeFundByIdLedgerByIdError() {

    logger.info("=== Test Get Composite Fund record by id, get Ledger by id Internal server error ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);
    fund.setLedgerId(ID_FOR_INTERNAL_SERVER_ERROR);
    addMockEntry(FUND.name(), JsonObject.mapFrom(fund));

    verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, FUND, 1);
  }

  @Test
  public void testGetCompositeFundByIdFiscalYearByIdError() {

    logger.info("=== Test Get Composite Fund record by id, get Fiscal Year by id Internal server error ===");

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setFiscalYearOneId(ID_FOR_INTERNAL_SERVER_ERROR);
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FUND, 1);
  }

  @Test
  public void testGetCompositeFundByIdFiscalYearsByQueryError() {

    logger.info("=== Test Get Composite Fund record by id, get Fiscal Years by query Internal server error ===");

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYearOne.setSeries(SERIES_INTERNAL_SERVER_ERROR);
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FUND, 1);
  }

  @Test
  public void testGetCompositeFundByIdGroupFundFyByQueryError() {

    logger.info("=== Test Get Composite Fund record by id, get Group Fund Fiscal Year by query Internal server error ===");

    FiscalYear fiscalYear = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    fiscalYear.setId(ID_FOR_INTERNAL_SERVER_ERROR);
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYear));

    FiscalYear fiscalYearOne = FISCAL_YEAR.getMockObject().mapTo(FiscalYear.class);
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(fiscalYearOne));

    verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    verifyRsEntitiesQuantity(HttpMethod.GET, FUND, 1);
  }

  @Test
  public void testGetCompositeFundByIdServerError() {
    logger.info("=== Test Get Composite Fund record by id - Internal Server Error ===");

    verifyGet(FUND.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode())
      .as(Errors.class);
  }

  @Test
  public void testGetCompositeFundByIdNotFound() {
    logger.info("=== Test Get Composite Fund record by id - Not Found ===");

    verifyGet(FUND.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);
  }

  @Test
  public void testPostCompositeFundEmptyGroupIds() {
    logger.info("=== Test create Composite Fund record ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    CompositeFund compositeFund = verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode()).as(CompositeFund.class);

    assertThat(compositeFund.getFund(), hasProperty(ID));

    compareRecordWithSentToStorage(HttpMethod.POST, JsonObject.mapFrom(fundRecord), FUND);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 0);
  }

  @Test
  public void testPostCompositeFundWithGroupIds() {
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

    verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode());

    compareRecordWithSentToStorage(HttpMethod.POST, JsonObject.mapFrom(fundRecord), FUND);
    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, record.getGroupIds().size());

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);
  }

  @Test
  public void testPostCompositeFundWithInternalServerErrorOnGetLedger() {
    logger.info("=== Test create Composite Fund record, get current Fiscal Year Internal Sever Error ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    fundRecord.setLedgerId(ID_FOR_INTERNAL_SERVER_ERROR);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());
    verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 0);
  }

  @Test
  public void testPostCompositeFundWithInternalServerErrorOnGetFiscalYearById() {
    logger.info("=== Test create Composite Fund record, get Fiscal Year by id Internal Sever Error ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);

    Ledger ledger = LEDGER.getMockObject().mapTo(Ledger.class);
    ledger.setId(fundRecord.getLedgerId());
    ledger.setFiscalYearOneId(ID_FOR_INTERNAL_SERVER_ERROR);
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));

    record.getGroupIds().add(UUID.randomUUID().toString());
    verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);
  }


  @Test
  public void testPostCompositeFundWithEmptyResultOnSearchFiscalYear() {
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

    Errors errors = verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, 422).as(Errors.class);
    verifyCurrentFYQuery(fiscalYearOne);

    assertThat(errors.getErrors().get(0).getCode(), equalTo(FISCAL_YEARS_NOT_FOUND.getCode()));

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
  }

  @Test
  public void testPostCompositeFundWithInternalServerErrorOnGroupFundFYCreation() {
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

    Headers headers = prepareHeaders(X_OKAPI_URL, ERROR_X_OKAPI_TENANT);
    verifyPostResponse(FUND.getEndpoint(), record,  headers, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);

    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
  }

  @Test
  public void testUpdateRecordWithAssignUnassignGroupIds() {
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
    verifyPut(FUND.getEndpointWithId(id), body, "", NO_CONTENT.getStatusCode());

    JsonObject expected = JsonObject.mapFrom(body).getJsonObject(FUND_FIELD_NAME);
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, FUND);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    List<JsonObject> createdGroupFundFiscalYears = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(createdGroupFundFiscalYears, hasSize(1));
    assertThat(createdGroupFundFiscalYears.get(0).getString(GROUP_ID_FIELD_NAME), equalTo(GROUP_ID));

    List<JsonObject> deletedGroupFundFiscalYears = MockServer.getRqRsEntries(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(deletedGroupFundFiscalYears, hasSize(1));
    assertThat(GROUP_FUND_FISCAL_YEAR.getMockObject().getString(GROUP_ID_FIELD_NAME), equalTo(GROUP_ID_FOR_DELETION));

  }

  @Test
  public void testUpdateRecordWithAssignGroupIds() {
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
    verifyPut(FUND.getEndpointWithId(id), body, "", NO_CONTENT.getStatusCode());

    JsonObject expected = JsonObject.mapFrom(body).getJsonObject(FUND_FIELD_NAME);
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, FUND);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    List<JsonObject> createdGroupFundFiscalYears = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(createdGroupFundFiscalYears, hasSize(1));
    assertThat(createdGroupFundFiscalYears.get(0).getString(GROUP_ID_FIELD_NAME), equalTo(GROUP_ID));

    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 0);

  }

  @Test
  public void testUpdateRecordWithUnassignGroupIds() {
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
    verifyPut(FUND.getEndpointWithId(id), body, "", NO_CONTENT.getStatusCode());

    JsonObject expected = JsonObject.mapFrom(body).getJsonObject(FUND_FIELD_NAME);
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, FUND);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);

    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);

    List<JsonObject> deletedGroupFundFiscalYears = MockServer.getRqRsEntries(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(deletedGroupFundFiscalYears, hasSize(1));
    assertThat(GROUP_FUND_FISCAL_YEAR.getMockObject().getString(GROUP_ID_FIELD_NAME), equalTo(GROUP_ID_FOR_DELETION));

  }

  @Test
  public void testUpdateRecordWithGroupIdsGroupNotFound() {
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
    verifyPut(FUND.getEndpointWithId(id), body, "", 422);

    verifyCurrentFYQuery(fiscalYearOne);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 0);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 2);
    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 0);

  }

  @Test
  public void testUpdateRecordFiscalYearNotFound() {
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
    verifyPut(FUND.getEndpointWithId(id), body, "", 422);

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 0);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 1);
    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 0);

  }

  @Test
  public void testUpdateRecordLedgerNotFound() {
    logger.info("=== Test update Composite Fund record - Ledger Internal Server Error ===");

    Fund fund = FUND.getMockObject().mapTo(Fund.class);
    fund.setLedgerId(ID_FOR_INTERNAL_SERVER_ERROR);

    CompositeFund record = new CompositeFund().withFund(fund);
    record.getGroupIds().add(UUID.randomUUID().toString());

    JsonObject body = JsonObject.mapFrom(record);

    body.getJsonObject(FUND_FIELD_NAME).put(FUND.getUpdatedFieldName(), FUND.getUpdatedFieldValue());

    String id = body.getJsonObject(FUND_FIELD_NAME).getString(ID);
    verifyPut(FUND.getEndpointWithId(id), body, "", INTERNAL_SERVER_ERROR.getStatusCode());

    verifyRsEntitiesQuantity(HttpMethod.PUT, FUND, 0);
    verifyRsEntitiesQuantity(HttpMethod.GET, LEDGER, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, FISCAL_YEAR, 0);
    verifyRsEntitiesQuantity(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR, 0);
    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 0);

  }

  @Test
  public void testUpdateRecordServerError() {
    logger.info("=== Test update Composite Fund record - Internal Server Error ===");

    JsonObject body = FUND.getMockObject();
    body.put(ID, ID_FOR_INTERNAL_SERVER_ERROR);
    CompositeFund compositeFund = new CompositeFund().withFund(body.mapTo(Fund.class));
    verifyPut(FUND.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), compositeFund, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode())
      .as(Errors.class);
  }

  @Test
  public void testUpdateRecordNotFound() {
    logger.info("=== Test update Composite Fund record - Not Found ===");

    JsonObject body = FUND.getMockObject();
    body.put(ID, ID_DOES_NOT_EXIST);
    CompositeFund compositeFund = new CompositeFund().withFund(body.mapTo(Fund.class));
    verifyPut(FUND.getEndpointWithId(ID_DOES_NOT_EXIST), compositeFund, APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);
  }

  @Test
  public void testUpdateRecordIdMismatch() {
    logger.info("=== Test update Composite Fund record - Path and body id mismatch ===");

    JsonObject body = FUND.getMockObject();
    CompositeFund compositeFund = new CompositeFund().withFund(body.mapTo(Fund.class));
    Errors errors = verifyPut(FUND.getEndpointWithId(VALID_UUID), compositeFund, APPLICATION_JSON, 422)
      .as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0), IsEqual.equalTo(ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()));
  }

  @Test
  public void testDeleteRecord() {
    logger.info("=== Test delete Composite Fund record ===");

    verifyDeleteResponse(FUND.getEndpointWithDefaultId(), "", NO_CONTENT.getStatusCode());

    verifyRsEntitiesQuantity(HttpMethod.DELETE, FUND, 1);
    verifyRsEntitiesQuantity(HttpMethod.GET, GROUP_FUND_FISCAL_YEAR, 1);
    verifyRsEntitiesQuantity(HttpMethod.DELETE, GROUP_FUND_FISCAL_YEAR, 1);
  }

  @Test
  public void testDeleteRecordServerError() {
    logger.info("=== Test delete Composite Fund record - Internal Server Error ===");

    verifyDeleteResponse(FUND.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @Test
  public void testDeleteRecordNotFound() {
    logger.info("=== Test delete Composite Fund record - Not Found ===");

    verifyDeleteResponse(FUND.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode())
      .as(Errors.class);
  }

  private void verifyCurrentFYQuery(FiscalYear fiscalYearOne) {
    String query = getQueryParams(FISCAL_YEAR.name()).get(0);
    String now = LocalDate.now(Clock.systemUTC()).toString();
    String next = LocalDateTime.now(Clock.systemUTC()).plus(getFiscalYearDuration(fiscalYearOne)).toLocalDate().toString();
    assertThat(query, containsString(fiscalYearOne.getSeries()));
    assertThat(query, containsString(now));
    assertThat(query, containsString(next));
  }

  private void verifyRsEntitiesQuantity(HttpMethod httpMethod, TestEntities entity, int expectedQuantity) {
    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(httpMethod, entity.name());
    assertThat(rqRsPostFund, hasSize(expectedQuantity));
  }
}
