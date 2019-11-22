package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.impl.ApiTestSuite.mockPort;
import static org.folio.rest.util.HelperUtils.convertToJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ApiTestBase {

  private static final String FINANCE_TENANT = "financeimpltest";
  public static final String ERROR_TENANT = "error_tenant";
  public static final String OKAPI_URL = "X-Okapi-Url";
  static final String VALID_UUID = "8d3881f6-dd93-46f0-b29d-1c36bdb5c9f9";
  static final Header X_OKAPI_URL = new Header(OKAPI_URL, "http://localhost:" + mockPort);
  static final Header X_OKAPI_TOKEN = new Header(OKAPI_HEADER_TOKEN, "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6ImQyYzQ1MTU2LTA2OGItNWU4Yy04YWM0LWZmMDBhZGY5NmFlNyIsImlhdCI6MTU3NDM1MDA0MSwidGVuYW50IjoiZGlrdSJ9.Csw8X_2rssows0T9a3duslnOrewdyiPcK-ZzbdmRLkI");
  public static final Header X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, FINANCE_TENANT);
  public static final Header EMPTY_CONFIG_X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, "EmptyConfig");
  public static final Header ERROR_X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, ERROR_TENANT);

  public static final String BAD_QUERY = "unprocessableQuery";
  public static final String ID_DOES_NOT_EXIST = "d25498e7-3ae6-45fe-9612-ec99e2700d2f";
  public static final String SERIES_DOES_NOT_EXIST = ID_DOES_NOT_EXIST;
  public static final String ID_FOR_INTERNAL_SERVER_ERROR = "168f8a86-d26c-406e-813f-c7527f241ac3";
  public static final String SERIES_INTERNAL_SERVER_ERROR = ID_FOR_INTERNAL_SERVER_ERROR;
  public static final String BASE_MOCK_DATA_PATH = "mockdata/";
  public static final String TOTAL_RECORDS = "totalRecords";


  static {
    System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  private static final Logger logger = LoggerFactory.getLogger(ApiTestBase.class);

  private static boolean runningOnOwn;

  @BeforeAll
  public static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if(ApiTestSuite.isNotInitialised()) {
      logger.info("Running test on own, initialising suite manually");
      runningOnOwn = true;
      ApiTestSuite.before();
    }
  }

  @AfterAll
  public static void after() {
    if(runningOnOwn) {
      logger.info("Running test on own, un-initialising suite manually");
      ApiTestSuite.after();
    }
  }

  @BeforeEach
  public void setUp() {
    clearServiceInteractions();
  }

  protected void clearServiceInteractions() {
    MockServer.serverRqRs.clear();
    MockServer.serverRqQueries.clear();
  }

  Response verifyPostResponse(String url, Object body, String expectedContentType, int expectedCode) {
    Headers headers = prepareHeaders(X_OKAPI_URL, X_OKAPI_TENANT, X_OKAPI_TOKEN);
    return verifyPostResponse(url, body, headers, expectedContentType, expectedCode);
  }

  Response verifyPostResponse(String url, Object body, Headers headers, String expectedContentType, int expectedCode) {
    return RestAssured
      .with()
        .header(X_OKAPI_URL)
        .headers(headers)
        .header(X_OKAPI_TOKEN)
        .contentType(APPLICATION_JSON)
        .body(convertToJson(body).encodePrettily())
      .post(url)
        .then()
          .log().all()
          .statusCode(expectedCode)
          .contentType(expectedContentType)
            .extract().response();
  }

  Response verifyPut(String url, Object body, String expectedContentType, int expectedCode) {
    Headers headers = prepareHeaders(X_OKAPI_URL, X_OKAPI_TENANT, X_OKAPI_TOKEN);
    return verifyPut(url, convertToJson(body).encodePrettily(), headers, expectedContentType, expectedCode);
  }

  Response verifyPut(String url, String body, Headers headers, String expectedContentType, int expectedCode) {
    return RestAssured
      .with()
        .headers(headers)
        .header(X_OKAPI_URL)
        .body(body)
        .contentType(APPLICATION_JSON)
      .put(url)
        .then()
          .statusCode(expectedCode)
          .contentType(expectedContentType)
          .extract()
            .response();
  }

  Response verifyGet(String url, String expectedContentType, int expectedCode) {
    Headers headers = prepareHeaders(X_OKAPI_URL, X_OKAPI_TENANT);
    return verifyGet(url, headers, expectedContentType, expectedCode);
  }

  Response verifyGet(String url, Headers headers, String expectedContentType, int expectedCode) {
    return RestAssured
      .with()
        .headers(headers)
      .get(url)
        .then()
        .log().all()
        .statusCode(expectedCode)
        .contentType(expectedContentType)
        .extract()
          .response();
  }

  Response verifyDeleteResponse(String url, String expectedContentType, int expectedCode) {
    Headers headers =  prepareHeaders(X_OKAPI_URL, X_OKAPI_TENANT);
    return verifyDeleteResponse(url, headers, expectedContentType, expectedCode);
  }

  Response verifyDeleteResponse(String url, Headers headers, String expectedContentType, int expectedCode) {
    return RestAssured
      .with()
        .headers(headers)
      .delete(url)
        .then()
          .statusCode(expectedCode)
          .contentType(expectedContentType)
          .extract()
            .response();
  }

  void verifyRecordNotSentToStorage(HttpMethod method, JsonObject record, TestEntities testEntity) {
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
    Object recordToStorage = entry.mapTo(testEntity.getClazz());

    assertThat(recordToStorage, samePropertyValuesAs(record.mapTo(testEntity.getClazz()), ignoreProperties));
  }

  void compareRecordWithSentToStorage(HttpMethod method, JsonObject record, TestEntities testEntity) {
    // Verify that record sent to storage is the same as in response
    List<JsonObject> rqRsEntries = MockServer.getRqRsEntries(method, testEntity.name());
    assertThat(rqRsEntries, hasSize(1));

    // remove "metadata" before comparing
    JsonObject entry = rqRsEntries.get(0);
    entry.remove("metadata");
    Object recordToStorage = entry.mapTo(testEntity.getClazz());

    assertThat(recordToStorage, samePropertyValuesAs(record.mapTo(testEntity.getClazz())));
  }

  Headers prepareHeaders(Header... headers) {
    return new Headers(headers);
  }

  String buildQueryParam(String query) {
    return "?query=" + query;
  }

  public static String getMockData(String path) throws IOException {
    logger.info("Using mock datafile: {}", path);
    try (InputStream resourceAsStream = ApiTestBase.class.getClassLoader().getResourceAsStream(path)) {
      if (resourceAsStream != null) {
        return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      } else {
        StringBuilder sb = new StringBuilder();
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
          lines.forEach(sb::append);
        }
        return sb.toString();
      }
    }
  }
}
