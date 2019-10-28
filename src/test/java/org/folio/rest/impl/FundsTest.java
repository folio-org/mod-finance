package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.MockServer.ERROR_X_OKAPI_TENANT;
import static org.folio.rest.util.MockServer.getRecordById;
import static org.folio.rest.util.TestEntities.FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.FUND;
import static org.folio.rest.util.TestEntities.GROUP_FUND_FISCAL_YEAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.MockServer;
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

    verifyGet(FUND.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(CompositeFund.class);

    // Make sure that correct storage endpoint was used
    assertThat(getRecordById(FUND.name()), hasSize(1));
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
    verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode());
    compareRecordWithSentToStorage(HttpMethod.POST, JsonObject.mapFrom(fundRecord), FUND);

    List<JsonObject> rqRsPostEntries = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(rqRsPostEntries, hasSize(0));

    List<JsonObject> rqRsGetEntries = MockServer.getRqRsEntries(HttpMethod.GET, FISCAL_YEAR.name());
    assertThat(rqRsGetEntries, hasSize(0));
  }

  @Test
  public void testPostCompositeFundWithGroupIds() {
    logger.info("=== Test create Composite Fund record ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());
    record.getGroupIds().add(UUID.randomUUID().toString());
    verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode());
    compareRecordWithSentToStorage(HttpMethod.POST, JsonObject.mapFrom(fundRecord), FUND);

    List<JsonObject> rqRsPostEntries = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(rqRsPostEntries, hasSize(record.getGroupIds().size()));

    List<JsonObject> rqRsGetEntries = MockServer.getRqRsEntries(HttpMethod.GET, FISCAL_YEAR.name());
    assertThat(rqRsGetEntries, hasSize(1));
  }


  @Test
  public void testPostCompositeFundWithInternalServerErrorOnSearchFiscalYear() {
    logger.info("=== Test create Composite Fund record, get current Fiscal Year Internal Sever Error ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    fundRecord.setLedgerId(ID_FOR_INTERNAL_SERVER_ERROR);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());
    verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);


    List<JsonObject> rqRsPostEntries = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(rqRsPostEntries, hasSize(0));

    List<JsonObject> rqRsGetEntries = MockServer.getRqRsEntries(HttpMethod.GET, FISCAL_YEAR.name());
    assertThat(rqRsGetEntries, hasSize(0));
  }

  @Test
  public void testPostCompositeFundWithEmptyResultOnSearchFiscalYear() {
    logger.info("=== Test create Composite Fund record current Fiscal Year not found ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    fundRecord.setLedgerId(ID_DOES_NOT_EXIST);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());
    Errors errors = verifyPostResponse(FUND.getEndpoint(), record, APPLICATION_JSON, 422).as(Errors.class);

    assertThat(errors.getErrors().get(0).getCode(), equalTo(FISCAL_YEARS_NOT_FOUND.getCode()));

    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(rqRsPostFund, hasSize(0));

    List<JsonObject> rqRsPostGroupFundFY = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(rqRsPostGroupFundFY, hasSize(0));

    List<JsonObject> rqRsGetFY = MockServer.getRqRsEntries(HttpMethod.GET, FISCAL_YEAR.name());
    assertThat(rqRsGetFY, hasSize(0));
  }

  @Test
  public void testPostCompositeFundWithInternalServerErrorOnGroupFundFYCreation() {
    logger.info("=== Test create Composite Fund record, Internal Server Error upon groupFundFiscalYear ===");

    Fund fundRecord = FUND.getMockObject().mapTo(Fund.class);
    CompositeFund record = new CompositeFund().withFund(fundRecord);
    record.getGroupIds().add(UUID.randomUUID().toString());
    Headers headers = prepareHeaders(X_OKAPI_URL, ERROR_X_OKAPI_TENANT);
    verifyPostResponse(FUND.getEndpoint(), record,  headers, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    List<JsonObject> rqRsPostFund = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(rqRsPostFund, hasSize(0));

    List<JsonObject> rqRsPostGroupFundFY = MockServer.getRqRsEntries(HttpMethod.POST, GROUP_FUND_FISCAL_YEAR.name());
    assertThat(rqRsPostGroupFundFY, hasSize(0));

    List<JsonObject> rqRsGetFY = MockServer.getRqRsEntries(HttpMethod.GET, FISCAL_YEAR.name());
    assertThat(rqRsGetFY, hasSize(1));
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
}
