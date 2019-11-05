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
import static org.folio.rest.util.TestEntities.GROUP_FUND_FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.LEDGER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

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
  public void testUpdateRecord() {
    logger.info("=== Test update Composite Fund record ===");

    JsonObject body = FUND.getMockObject();

    body.put(FUND.getUpdatedFieldName(), FUND.getUpdatedFieldValue());
    JsonObject expected = JsonObject.mapFrom(body);

    String id = (String) body.remove(ID);
    CompositeFund compositeFund = new CompositeFund().withFund(body.mapTo(Fund.class));

    verifyPut(FUND.getEndpointWithId(id), compositeFund, "", NO_CONTENT.getStatusCode());
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, FUND);
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
    String now = LocalDate.now().toString();
    String next = LocalDateTime.now().plus(getFiscalYearDuration(fiscalYearOne)).toLocalDate().toString();
    assertThat(query, containsString(fiscalYearOne.getSeries()));
    assertThat(query, containsString(now));
    assertThat(query, containsString(next));
  }

  private void verifyRsEntitiesQuantity(HttpMethod httpMethod, TestEntities entity, int expectedQuantity) {
    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(httpMethod, entity.name());
    assertThat(rqRsPostFund, hasSize(expectedQuantity));
  }
}
