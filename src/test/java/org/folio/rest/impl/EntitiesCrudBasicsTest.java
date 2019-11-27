package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.MockServer.ERROR_X_OKAPI_TENANT;
import static org.folio.rest.util.MockServer.getCollectionRecords;
import static org.folio.rest.util.MockServer.getRecordById;
import static org.folio.rest.util.MockServer.getRqRsEntries;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.http.Headers;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EntitiesCrudBasicsTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(EntitiesCrudBasicsTest.class);
  private static final List<TestEntities> transactionEntities = Arrays.asList(TestEntities.TRANSACTIONS_ALLOCATION,
      TestEntities.TRANSACTIONS_ENCUMBRANCE, TestEntities.TRANSACTIONS_TRANSFER, TestEntities.ORDER_TRANSACTION_SUMMARY);

  /**
   * Test entities except for FUND
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntities() {
    return Arrays.stream(TestEntities.values())
      .filter(e -> !e.equals(TestEntities.FUND));
  }

  /**
   * Test entities except for TRANSACTIONS_ALLOCATION, TRANSACTIONS_TRANSFER, TRANSACTIONS_ENCUMBRANCE
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesWithGetEndpoint() {
    return getTestEntities().filter(e -> !transactionEntities.contains(e));
  }

  /**
   * Test entities except for all TransactionTypes, GROUP_FUND_FISCAL_YEAR
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitieswithGetByIdEndpoint() {
    return getTestEntitiesWithGetEndpoint().filter(e -> !e.equals(TestEntities.GROUP_FUND_FISCAL_YEAR));
  }

  static Stream<TestEntities> getTestEntitiesWithPostEndpoint() {
    return getTestEntities().filter(e -> !e.equals(TestEntities.TRANSACTIONS));
  }

  /**
   * Test entities except for TransactionTypes, GROUP_FUND_FISCAL_YEAR, TRANSACTIONS
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesWithPutEndpoint() {
    return getTestEntitieswithGetByIdEndpoint()
      .filter(e -> !e.equals(TestEntities.TRANSACTIONS));
  }

  /**
   * Test entities except for TransactionTypes, TRANSACTIONS
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesWithDeleteEndpoint() {
    return getTestEntitiesWithGetEndpoint().filter(e -> !e.equals(TestEntities.TRANSACTIONS));
  }

  /**
   * Test entities only for TRANSACTIONS_ALLOCATION, TRANSACTIONS_TRANSFER, TRANSACTIONS_ENCUMBRANCE
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesForOnlyTransactionTypes() {
    return transactionEntities.stream().filter(e -> !e.equals(TestEntities.ORDER_TRANSACTION_SUMMARY));
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

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithGetEndpoint")
  public void testGetCollection(TestEntities testEntity) {
    logger.info("=== Test Get collection of {} ===", testEntity.name());

    String responseBody = verifyGet(testEntity.getEndpoint(), APPLICATION_JSON, OK.getStatusCode()).asString();

    // Verify collection response
    JsonObject collection = new JsonObject(responseBody);

    // 2 properties are expected
    assertThat(collection, iterableWithSize(2));
    collection.iterator()
      .forEachRemaining(entry -> {
        if (TOTAL_RECORDS.equals(entry.getKey())) {
          assertThat(entry.getValue(), is(testEntity.getCollectionQuantity()));
        } else {
          assertThat(entry.getValue(), instanceOf(JsonArray.class));
          ((JsonArray) entry.getValue()).forEach(obj -> {
            assertThat(obj, instanceOf(JsonObject.class));
            // Each record is of expected type
            assertThat(((JsonObject) obj).mapTo(testEntity.getClazz()), notNullValue());
          });
        }
      });

    // Make sure that correct storage endpoint was used
    assertThat(getCollectionRecords(testEntity.name()), hasSize(1));
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithGetEndpoint")
  public void testGetCollectionInternalServerError(TestEntities testEntity) {
    logger.info("=== Test Get collection of {} records - Internal Server Error ===", testEntity.name());

    String query = buildQueryParam("id==" + ID_FOR_INTERNAL_SERVER_ERROR);
    verifyGet(testEntity.getEndpoint() + query, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithGetEndpoint")
  public void testGetCollectionBadQuery(TestEntities testEntity) {
    logger.info("=== Test Get collection of {} records - Bad Request error ===", testEntity.name());

    String query = buildQueryParam(BAD_QUERY);
    verifyGet(testEntity.getEndpoint() + query, APPLICATION_JSON, BAD_REQUEST.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitieswithGetByIdEndpoint")
  public void testGetRecordById(TestEntities testEntity) {
    logger.info("=== Test Get {} record by id ===", testEntity.name());

    verifyGet(testEntity.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(testEntity.getClazz());

    // Make sure that correct storage endpoint was used
    assertThat(getRecordById(testEntity.name()), hasSize(1));
  }

  @ParameterizedTest
  @MethodSource("getTestEntitieswithGetByIdEndpoint")
  public void testGetRecordByIdServerError(TestEntities testEntity) {
    logger.info("=== Test Get {} record by id - Internal Server Error ===", testEntity.name());

    verifyGet(testEntity.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode())
      .as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitieswithGetByIdEndpoint")
  public void testGetRecordByIdNotFound(TestEntities testEntity) {
    logger.info("=== Test Get {} record by id - Not Found ===", testEntity.name());

    verifyGet(testEntity.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPostEndpoint")
  public void testPostRecord(TestEntities testEntity) throws IOException {
    logger.info("=== Test create {} record ===", testEntity.name());

    JsonObject record = testEntity.getMockObject();
    verifyPostResponse(testEntity.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode());
    compareRecordWithSentToStorage(HttpMethod.POST, record, testEntity);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesForOnlyTransactionTypes")
  public void testPostRecordForUnprocessibleEntity(TestEntities testEntity) {
    logger.info("=== Test create {} record - Unprocessible entity ===", testEntity.name());

    JsonObject record = testEntity.getMockObject();
    Transaction t = record.mapTo(Transaction.class);
    // set invalid transactionType
    if (t.getTransactionType() == Transaction.TransactionType.ALLOCATION) {
      t.setTransactionType(Transaction.TransactionType.ENCUMBRANCE);
    } else if (t.getTransactionType() == Transaction.TransactionType.ENCUMBRANCE) {
      t.setTransactionType(Transaction.TransactionType.TRANSFER);
    } else if (t.getTransactionType() == Transaction.TransactionType.TRANSFER) {
      t.setTransactionType(Transaction.TransactionType.ALLOCATION);
    }
    record = JsonObject.mapFrom(t);
    verifyPostResponse(testEntity.getEndpoint(), record, APPLICATION_JSON, 422);
    verifyRecordNotSentToStorage(HttpMethod.POST, record, testEntity);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPostEndpoint")
  public void testPostRecordServerError(TestEntities testEntity) throws IOException {
    logger.info("=== Test create {} record - Internal Server Error ===", testEntity.name());

    Headers headers = prepareHeaders(X_OKAPI_URL, ERROR_X_OKAPI_TENANT);
    JsonObject record = testEntity.getMockObject();

    verifyPostResponse(testEntity.getEndpoint(), record, headers, APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPutEndpoint")
  public void testUpdateRecord(TestEntities testEntity) {
    logger.info("=== Test update {} record ===", testEntity.name());

    JsonObject body = testEntity.getMockObject();
    body.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());

    JsonObject expected = JsonObject.mapFrom(body);

    verifyPut(testEntity.getEndpointWithId((String) body.remove(ID)), body, "", NO_CONTENT.getStatusCode());
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, testEntity);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPutEndpoint")
  public void testUpdateRecordServerError(TestEntities testEntity) {
    logger.info("=== Test update {} record - Internal Server Error ===", testEntity.name());

    JsonObject body = testEntity.getMockObject();
    body.put(ID, ID_FOR_INTERNAL_SERVER_ERROR);

    verifyPut(testEntity.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), body, APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPutEndpoint")
  public void testUpdateRecordNotFound(TestEntities testEntity) {
    logger.info("=== Test update {} record - Not Found ===", testEntity.name());

    JsonObject body = testEntity.getMockObject();
    body.put(ID, ID_DOES_NOT_EXIST);

    verifyPut(testEntity.getEndpointWithId(ID_DOES_NOT_EXIST), body, APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPutEndpoint")
  public void testUpdateRecordIdMismatch(TestEntities testEntity) {
    logger.info("=== Test update {} record - Path and body id mismatch ===", testEntity.name());

    Errors errors = verifyPut(testEntity.getEndpointWithId(VALID_UUID), testEntity.getMockObject(), APPLICATION_JSON, 422)
      .as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors()
      .get(0), equalTo(ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()));
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithDeleteEndpoint")
  public void testDeleteRecord(TestEntities testEntity) {
    logger.info("=== Test delete {} record ===", testEntity.name());

    verifyDeleteResponse(testEntity.getEndpointWithDefaultId(), "", NO_CONTENT.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithDeleteEndpoint")
  public void testDeleteRecordServerError(TestEntities testEntity) {
    logger.info("=== Test delete {} record - Internal Server Error ===", testEntity.name());

    verifyDeleteResponse(testEntity.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithDeleteEndpoint")
  public void testDeleteRecordNotFound(TestEntities testEntity) {
    logger.info("=== Test delete {} record - Not Found ===", testEntity.name());

    verifyDeleteResponse(testEntity.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode())
      .as(Errors.class);
  }

  @ParameterizedTest
  @EnumSource(value = TestEntities.class, names = {"ORDER_TRANSACTION_SUMMARY"})
  public void testPostRecordMinimumValidation(TestEntities testEntity) {
    logger.info("=== Test create {} record with less then minimum validation fails===", testEntity.name());

    JsonObject record = testEntity.getMockObject();
    record.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());
    verifyPostResponse(testEntity.getEndpoint(), record, APPLICATION_JSON, 422);
  }
}
