package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.TestEntities.FISCAL_YEAR;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.MockServer;
import org.junit.jupiter.api.Test;

public class FiscalYearTest extends ApiTestBase {
  private static final Logger logger = LoggerFactory.getLogger(FiscalYearTest.class);

  @Test
  public void testPostFiscalYearWithCurreny() {
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
  public void testPutFiscalYearWithCurreny() {
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
    assertEquals(rqRsPostFund.get(0)
      .getString("currency"), "USD");
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

}
