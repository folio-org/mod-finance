package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.MockServer.getCollectionRecords;
import static org.folio.rest.util.MockServer.getRecordById;
import static org.folio.rest.util.TestConfig.clearServiceInteractions;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestConstants.BAD_QUERY;
import static org.folio.rest.util.TestConstants.ERROR_X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.ID_DOES_NOT_EXIST;
import static org.folio.rest.util.TestConstants.ID_FOR_INTERNAL_SERVER_ERROR;
import static org.folio.rest.util.TestConstants.TOTAL_RECORDS;
import static org.folio.rest.util.TestConstants.VALID_UUID;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.folio.rest.util.TestEntities.FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.FUND;
import static org.folio.rest.util.TestEntities.GROUP;
import static org.folio.rest.util.TestEntities.GROUP_FUND_FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.INVOICE_TRANSACTION_SUMMARY;
import static org.folio.rest.util.TestEntities.LEDGER;
import static org.folio.rest.util.TestEntities.LEDGER_ROLLOVER;
import static org.folio.rest.util.TestEntities.LEDGER_ROLLOVER_BUDGETS;
import static org.folio.rest.util.TestEntities.LEDGER_ROLLOVER_ERRORS;
import static org.folio.rest.util.TestEntities.LEDGER_ROLLOVER_LOGS;
import static org.folio.rest.util.TestEntities.LEDGER_ROLLOVER_PROGRESS;
import static org.folio.rest.util.TestEntities.ORDER_TRANSACTION_SUMMARY;
import static org.folio.rest.util.TestEntities.TRANSACTIONS;
import static org.folio.rest.util.TestEntities.TRANSACTIONS_ALLOCATION;
import static org.folio.rest.util.TestEntities.TRANSACTIONS_CREDIT;
import static org.folio.rest.util.TestEntities.TRANSACTIONS_ENCUMBRANCE;
import static org.folio.rest.util.TestEntities.TRANSACTIONS_PAYMENT;
import static org.folio.rest.util.TestEntities.TRANSACTIONS_PENDING_PAYMENT;
import static org.folio.rest.util.TestEntities.TRANSACTIONS_TRANSFER;
import static org.folio.rest.util.TestUtils.getMockData;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.RestTestUtils;
import org.folio.rest.util.TestConfig;
import org.folio.rest.util.TestEntities;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EntitiesCrudBasicsTest {

  private static final Logger logger = LogManager.getLogger(EntitiesCrudBasicsTest.class);
  private static final List<TestEntities> transactionEntities = Arrays.asList(TRANSACTIONS_ALLOCATION, TRANSACTIONS_ENCUMBRANCE
      , TRANSACTIONS_TRANSFER, TRANSACTIONS_PAYMENT, TRANSACTIONS_PENDING_PAYMENT
        , TRANSACTIONS_CREDIT, ORDER_TRANSACTION_SUMMARY, INVOICE_TRANSACTION_SUMMARY);
  private static boolean runningOnOwn;


  /**
   * Test entities except for FUND
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntities() {
    return Arrays.stream(TestEntities.values())
      .filter(e -> !e.equals(FUND))
      .filter(e -> !e.equals(BUDGET))
      .filter(e -> !e.equals(LEDGER_ROLLOVER))
      .filter(e -> !e.equals(LEDGER_ROLLOVER_LOGS))
      .filter(e -> !e.equals(LEDGER_ROLLOVER_BUDGETS))
      .filter(e -> !e.equals(LEDGER_ROLLOVER_PROGRESS))
      .filter(e -> !e.equals(LEDGER_ROLLOVER_ERRORS))
      .filter(e -> !e.equals(LEDGER));
  }

  /**
   * Test entities except for TRANSACTIONS_ALLOCATION, TRANSACTIONS_TRANSFER, TRANSACTIONS_ENCUMBRANCE
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesWithGetEndpoint() {
    return getTestEntities().filter(e -> !transactionEntities.contains(e));
  }

  static Stream<TestEntities> getTestEntitiesWithGetEndpointWithoutGroup() {
    return getTestEntitiesWithGetEndpoint().filter(e -> !e.equals(FISCAL_YEAR) && !e.equals(GROUP));
  }

  /**
   * Test entities except for all TransactionTypes, GROUP_FUND_FISCAL_YEAR
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesWithGetByIdEndpoint() {
    return getTestEntitiesWithGetEndpoint()
      .filter(e -> !e.equals(GROUP_FUND_FISCAL_YEAR));
  }

  static Stream<TestEntities> getTestEntitiesWithPostEndpoint() {
    return getTestEntities().filter(e -> !e.equals(TRANSACTIONS));
  }

  /**
   * Test entities except for TransactionTypes, GROUP_FUND_FISCAL_YEAR, TRANSACTIONS
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesWithPutEndpoint() {
    return Stream.concat(getTestEntitiesWithGetByIdEndpoint()
      .filter(e -> !e.equals(TRANSACTIONS)), Stream.of(TRANSACTIONS_PENDING_PAYMENT));
  }

  /**
   * Test entities except for TransactionTypes, TRANSACTIONS
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesWithDeleteEndpoint() {
    return getTestEntitiesWithGetEndpoint().filter(e -> !e.equals(TRANSACTIONS));
  }

  /**
   * Test entities only for TRANSACTIONS_ALLOCATION, TRANSACTIONS_TRANSFER, TRANSACTIONS_ENCUMBRANCE
   *
   * @return stream of test entities
   */
  static Stream<TestEntities> getTestEntitiesForOnlyTransactionTypes() {
    return transactionEntities.stream().filter(e -> !e.equals(ORDER_TRANSACTION_SUMMARY)
            && !e.equals(INVOICE_TRANSACTION_SUMMARY));
  }

  @BeforeAll
  static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(ApplicationConfig.class);
  }

  @AfterAll
  static void after() {
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
  }

  @AfterEach
  void afterEach() {
    clearServiceInteractions();
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithGetEndpointWithoutGroup")
  void testGetCollection(TestEntities testEntity) {
    logger.info("=== Test Get collection of {} ===", testEntity.name());

    String responseBody = RestTestUtils.verifyGet(testEntity.getEndpoint(), APPLICATION_JSON, OK.getStatusCode()).asString();

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
  @MethodSource("getTestEntitiesWithGetEndpointWithoutGroup")
  void testGetCollectionInternalServerError(TestEntities testEntity) {
    logger.info("=== Test Get collection of {} records - Internal Server Error ===", testEntity.name());

    String query = RestTestUtils.buildQueryParam("id==" + ID_FOR_INTERNAL_SERVER_ERROR);
    RestTestUtils.verifyGet(testEntity.getEndpoint() + query, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithGetEndpointWithoutGroup")
  void testGetCollectionBadQuery(TestEntities testEntity) {
    logger.info("=== Test Get collection of {} records - Bad Request error ===", testEntity.name());

    String query = RestTestUtils.buildQueryParam(BAD_QUERY);
    RestTestUtils.verifyGet(testEntity.getEndpoint() + query, APPLICATION_JSON, BAD_REQUEST.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithGetByIdEndpoint")
  void testGetRecordById(TestEntities testEntity) {
    logger.info("=== Test Get {} record by id ===", testEntity.name());

    RestTestUtils.verifyGet(testEntity.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(testEntity.getClazz());

    // Make sure that correct storage endpoint was used
    assertThat(getRecordById(testEntity.name()), hasSize(1));
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithGetByIdEndpoint")
  void testGetRecordByIdServerError(TestEntities testEntity) {
    logger.info("=== Test Get {} record by id - Internal Server Error ===", testEntity.name());

    RestTestUtils.verifyGet(testEntity.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode())
      .as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithGetByIdEndpoint")
  void testGetRecordByIdNotFound(TestEntities testEntity) {
    logger.info("=== Test Get {} record by id - Not Found ===", testEntity.name());

    RestTestUtils.verifyGet(testEntity.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPostEndpoint")
  void testPostRecord(TestEntities testEntity) throws IOException {
    logger.info("=== Test create {} record ===", testEntity.name());
    if (testEntity.equals(TRANSACTIONS_ALLOCATION) || testEntity.equals(TRANSACTIONS_TRANSFER)) {
      addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/HIST.json")));
      addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));
    }

    JsonObject record = testEntity.getMockObject();
    RestTestUtils.verifyPostResponse(testEntity.getEndpoint(), record, APPLICATION_JSON, CREATED.getStatusCode());
    compareRecordWithSentToStorage(HttpMethod.POST, record, testEntity, testEntity.getIgnoreProperties());
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesForOnlyTransactionTypes")
  void testPostRecordForUnprocessibleEntity(TestEntities testEntity) {
    logger.info("=== Test create {} record - Unprocessible entity ===", testEntity.name());

    JsonObject record = testEntity.getMockObject();
    Transaction t = record.mapTo(Transaction.class);
    // set invalid transactionType
    if (t.getTransactionType() != Transaction.TransactionType.ALLOCATION) {
      t.setTransactionType(Transaction.TransactionType.ALLOCATION);
    } else {
      t.setTransactionType(Transaction.TransactionType.TRANSFER);
    }
    record = JsonObject.mapFrom(t);
    RestTestUtils.verifyPostResponse(testEntity.getEndpoint(), record, APPLICATION_JSON, 422);
    verifyRecordNotSentToStorage(HttpMethod.POST, testEntity);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPostEndpoint")
  void testPostRecordServerError(TestEntities testEntity) throws IOException {
    logger.info("=== Test create {} record - Internal Server Error ===", testEntity.name());
    if (testEntity.equals(TRANSACTIONS_ALLOCATION) || testEntity.equals(TRANSACTIONS_TRANSFER)) {
      addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/HIST.json")));
      addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));
    }

    Headers headers = RestTestUtils.prepareHeaders(TestConfig.X_OKAPI_URL, ERROR_X_OKAPI_TENANT);
    JsonObject record = testEntity.getMockObject();

    RestTestUtils.verifyPostResponse(testEntity.getEndpoint(), record, headers, APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPutEndpoint")
  void testUpdateRecord(TestEntities testEntity) {
    logger.info("=== Test update {} record ===", testEntity.name());

    JsonObject body = testEntity.getMockObject();
    body.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());

    JsonObject expected = JsonObject.mapFrom(body);

    RestTestUtils.verifyPut(testEntity.getEndpointWithId((String) body.remove(ID)), body, "", NO_CONTENT.getStatusCode());
    compareRecordWithSentToStorage(HttpMethod.PUT, expected, testEntity, testEntity.getIgnoreProperties());
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPutEndpoint")
  void testUpdateRecordServerError(TestEntities testEntity) {
    logger.info("=== Test update {} record - Internal Server Error ===", testEntity.name());

    JsonObject body = testEntity.getMockObject();
    body.put(ID, ID_FOR_INTERNAL_SERVER_ERROR);

    RestTestUtils.verifyPut(testEntity.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), body, APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPutEndpoint")
  void testUpdateRecordNotFound(TestEntities testEntity) {
    logger.info("=== Test update {} record - Not Found ===", testEntity.name());

    JsonObject body = testEntity.getMockObject();
    body.put(ID, ID_DOES_NOT_EXIST);

    RestTestUtils.verifyPut(testEntity.getEndpointWithId(ID_DOES_NOT_EXIST), body, APPLICATION_JSON, NOT_FOUND.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithPutEndpoint")
  void testUpdateRecordIdMismatch(TestEntities testEntity) {
    logger.info("=== Test update {} record - Path and body id mismatch ===", testEntity.name());

    Errors errors = RestTestUtils.verifyPut(testEntity.getEndpointWithId(VALID_UUID), testEntity.getMockObject(), APPLICATION_JSON, 422)
      .as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors()
      .get(0), equalTo(ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()));
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithDeleteEndpoint")
  void testDeleteRecord(TestEntities testEntity) {
    logger.info("=== Test delete {} record ===", testEntity.name());

    RestTestUtils.verifyDeleteResponse(testEntity.getEndpointWithDefaultId(), "", NO_CONTENT.getStatusCode());
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithDeleteEndpoint")
  void testDeleteRecordServerError(TestEntities testEntity) {
    logger.info("=== Test delete {} record - Internal Server Error ===", testEntity.name());

    RestTestUtils.verifyDeleteResponse(testEntity.getEndpointWithId(ID_FOR_INTERNAL_SERVER_ERROR), APPLICATION_JSON,
        INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);
  }

  @ParameterizedTest
  @MethodSource("getTestEntitiesWithDeleteEndpoint")
  void testDeleteRecordNotFound(TestEntities testEntity) {
    logger.info("=== Test delete {} record - Not Found ===", testEntity.name());

    RestTestUtils.verifyDeleteResponse(testEntity.getEndpointWithId(ID_DOES_NOT_EXIST), APPLICATION_JSON, NOT_FOUND.getStatusCode())
      .as(Errors.class);
  }

  @ParameterizedTest
  @EnumSource(value = TestEntities.class, names = {"ORDER_TRANSACTION_SUMMARY", "INVOICE_TRANSACTION_SUMMARY"})
  void testPostRecordMinimumValidation(TestEntities testEntity) {
    logger.info("=== Test create {} record with less then minimum validation fails===", testEntity.name());

    JsonObject record = testEntity.getMockObject();
    record.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());
    RestTestUtils.verifyPostResponse(testEntity.getEndpoint(), record, APPLICATION_JSON, 422);
  }

  @ParameterizedTest
  @EnumSource(value = TestEntities.class, names = {"ORDER_TRANSACTION_SUMMARY", "INVOICE_TRANSACTION_SUMMARY"})
  void testPutRecordMinimumValidation(TestEntities testEntity) {
    logger.info("=== Test create {} record with less then minimum validation fails===", testEntity.name());

    JsonObject record = testEntity.getMockObject();
    record.put(testEntity.getUpdatedFieldName(), 4);
    RestTestUtils.verifyPut(testEntity.getEndpointWithId(UUID.randomUUID().toString()), record, "", 422);
    RestTestUtils.verifyPut(testEntity.getEndpointWithDefaultId(), record, "", 204);
  }

  @ParameterizedTest
  @EnumSource(value = TestEntities.class, names = {"GROUP"})
  void testPostGroupWhenJsonErrorComeFromStorage(TestEntities testEntity) {
    logger.info("=== Test create {} record with less then minimum validation fails===", testEntity.name());

    JsonObject record = testEntity.getMockObject();
    record.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());
    RestTestUtils.verifyPostResponse(testEntity.getEndpoint(), record, APPLICATION_JSON, 201);
    Response response = RestTestUtils.verifyPostResponse(testEntity.getEndpoint(), record, APPLICATION_JSON, 400);

    Pattern pattern = Pattern.compile("(uniqueField.*Error)");
    Matcher matcher = pattern.matcher(response.getBody().asString());
    Assertions.assertTrue(matcher.find());
  }

  void verifyRecordNotSentToStorage(HttpMethod method, TestEntities testEntity) {
    // Verify that record not sent to storage
    List<JsonObject> rqRsEntries = MockServer.getRqRsEntries(method, testEntity.name());
    assertThat(rqRsEntries, hasSize(0));
  }

  /**
   * Compare the record returned with the record that was sent in request, properties to be ignored from comparision can be added
   * @param method
   * @param record
   * @param testEntity
   * @param ignoreProperties - Properties that will be ignored from comparison
   */
  void compareRecordWithSentToStorage(HttpMethod method, JsonObject record, TestEntities testEntity, String ignoreProperties) {
    // Verify that record sent to storage is the same as in response
    List<JsonObject> rqRsEntries = MockServer.getRqRsEntries(method, testEntity.name());
    assertThat(rqRsEntries, hasSize(1));

    // remove "metadata" before comparing
    JsonObject entry = rqRsEntries.get(0);
    entry.remove("metadata");
    Optional.ofNullable(ignoreProperties).ifPresent(p -> entry.remove(ignoreProperties));
    Object recordToStorage = entry.mapTo(testEntity.getClazz());
    Optional.ofNullable(ignoreProperties).ifPresent(p -> record.remove(ignoreProperties));
    assertThat(recordToStorage, SamePropertyValuesAs.samePropertyValuesAs(record.mapTo(testEntity.getClazz())));
  }

}
