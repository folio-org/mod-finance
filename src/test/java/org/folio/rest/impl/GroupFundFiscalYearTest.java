package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.MockServer.ERROR_X_OKAPI_TENANT;
import static org.folio.rest.util.MockServer.getCollectionRecords;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.List;

import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.TestEntities;
import org.junit.Test;

import io.restassured.http.Headers;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class GroupFundFiscalYearTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(GroupFundFiscalYearTest.class);

  @Test
  public void testGetCollection() {
    logger.info("=== Test Get collection of {} ===", TestEntities.GROUP_FUND_FISCAL_YEAR.name());

    String responseBody = verifyGet(TestEntities.GROUP_FUND_FISCAL_YEAR.getEndpoint(), APPLICATION_JSON, OK.getStatusCode()).asString();

    // Verify collection response
    JsonObject collection = new JsonObject(responseBody);

    // 2 properties are expected
    assertThat(collection, iterableWithSize(2));
    collection.iterator().forEachRemaining(entry -> {
      if (TOTAL_RECORDS.equals(entry.getKey())) {
        assertThat(entry.getValue(), is(TestEntities.GROUP_FUND_FISCAL_YEAR.getCollectionQuantity()));
      } else {
        assertThat(entry.getValue(), instanceOf(JsonArray.class));
        ((JsonArray) entry.getValue()).forEach(obj -> {
          assertThat(obj, instanceOf(JsonObject.class));
          // Each record is of expected type
          assertThat(((JsonObject) obj).mapTo(TestEntities.GROUP_FUND_FISCAL_YEAR.getClazz()), notNullValue());
        });
      }
    });

    // Make sure that correct storage endpoint was used
    assertThat(getCollectionRecords(TestEntities.GROUP_FUND_FISCAL_YEAR.name()), hasSize(1));
  }

  @Test
  public void testGetCollectionInternalServerError() {
    logger.info("=== Test Get collection of {} records - Internal Server Error ===", TestEntities.GROUP_FUND_FISCAL_YEAR.name());

    String query = buildQueryParam("id==" + ID_FOR_INTERNAL_SERVER_ERROR);
    verifyGet(TestEntities.GROUP_FUND_FISCAL_YEAR.getEndpoint() + query, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @Test
  public void testGetCollectionBadQuery() {
    logger.info("=== Test Get collection of {} records - Bad Request error ===", TestEntities.GROUP_FUND_FISCAL_YEAR.name());

    String query = buildQueryParam(BAD_QUERY);
    verifyGet(TestEntities.GROUP_FUND_FISCAL_YEAR.getEndpoint() + query, APPLICATION_JSON, BAD_REQUEST.getStatusCode()).as(Errors.class);
  }

  @Test
  public void testPostRecord() {
    logger.info("=== Test create {} record ===", TestEntities.GROUP_FUND_FISCAL_YEAR.name());

    JsonObject record = TestEntities.GROUP_FUND_FISCAL_YEAR.getMockObject();
    verifyPostResponse(TestEntities.GROUP_FUND_FISCAL_YEAR.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode());
    compareRecordWithSentToStorage(HttpMethod.POST, record);
  }

  @Test
  public void testPostRecordServerError() {
    logger.info("=== Test create {} record - Internal Server Error ===", TestEntities.GROUP_FUND_FISCAL_YEAR.name());

    Headers headers = prepareHeaders(X_OKAPI_URL, ERROR_X_OKAPI_TENANT);
    verifyPostResponse(TestEntities.GROUP_FUND_FISCAL_YEAR.getEndpoint(), TestEntities.GROUP_FUND_FISCAL_YEAR.getMockObject(), headers, APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @Test
  public void testDeleteRecord() {
    logger.info("=== Test delete {} record ===", TestEntities.GROUP_FUND_FISCAL_YEAR.name());

    verifyDeleteResponse(TestEntities.GROUP_FUND_FISCAL_YEAR.getEndpointWithDefaultId(), "", NO_CONTENT.getStatusCode());
  }

  @Test
  public void testDeleteRecordServerError() {
    logger.info("=== Test delete {} record - Internal Server Error ===", TestEntities.GROUP_FUND_FISCAL_YEAR.name());

    verifyDeleteResponse(TestEntities.GROUP_FUND_FISCAL_YEAR.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @Test
  public void testDeleteRecordNotFound() {
    logger.info("=== Test delete {} record - Not Found ===", TestEntities.GROUP_FUND_FISCAL_YEAR.name());

    verifyDeleteResponse(TestEntities.GROUP_FUND_FISCAL_YEAR.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode())
      .as(Errors.class);
  }

  private void compareRecordWithSentToStorage(HttpMethod method, JsonObject record) {
    // Verify that record sent to storage is the same as in response
    List<JsonObject> rqRsEntries = MockServer.getRqRsEntries(method, TestEntities.GROUP_FUND_FISCAL_YEAR.name());
    assertThat(rqRsEntries, hasSize(1));

    // remove "metadata" before comparing
    JsonObject entry = rqRsEntries.get(0);
    entry.remove("metadata");
    Object recordToStorage = entry.mapTo(TestEntities.GROUP_FUND_FISCAL_YEAR.getClazz());

    assertThat(record.mapTo(TestEntities.GROUP_FUND_FISCAL_YEAR.getClazz()), equalTo(recordToStorage));
  }
}
