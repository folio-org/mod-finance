package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.impl.ApiTestSuite.mockPort;

public class ApiTestBase {

  public static final String OKAPI_URL = "X-Okapi-Url";
  static final String VALID_UUID = "8d3881f6-dd93-46f0-b29d-1c36bdb5c9f9";
  static final Header X_OKAPI_URL = new Header(OKAPI_URL, "http://localhost:" + mockPort);
  static final Header X_OKAPI_TOKEN = new Header(OKAPI_HEADER_TOKEN, "eyJhbGciOiJIUzI1NiJ9");
  public static final Header X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, "financeimpltest");
  static final String ID_FOR_INTERNAL_SERVER_ERROR = "168f8a86-d26c-406e-813f-c7527f241ac3";

  static {
    System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  private static final Logger logger = LoggerFactory.getLogger(ApiTestBase.class);

  private static boolean runningOnOwn;

  @BeforeClass
  public static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if(ApiTestSuite.isNotInitialised()) {
      logger.info("Running test on own, initialising suite manually");
      runningOnOwn = true;
      ApiTestSuite.before();
    }
  }

  @AfterClass
  public static void after() {
    if(runningOnOwn) {
      logger.info("Running test on own, un-initialising suite manually");
      ApiTestSuite.after();
    }
  }

  void verifyPostResponse(String url, String body, Headers headers, String expectedContentType, int expectedCode) {
    RestAssured
      .with()
        .header(X_OKAPI_URL)
        .headers(headers)
        .header(X_OKAPI_TOKEN)
        .contentType(APPLICATION_JSON)
        .body(body)
      .post(url)
        .then()
          .log()
          .all()
          .statusCode(expectedCode);
  }

  Response verifyPut(String url, String body, String expectedContentType, int expectedCode) {
    Headers headers = prepareHeaders(X_OKAPI_URL, X_OKAPI_TENANT, X_OKAPI_TOKEN);
    return verifyPut(url, body, headers,expectedContentType, expectedCode);
  }

  Response verifyPut(String url, JsonObject body, String expectedContentType, int expectedCode) {
    Headers headers = prepareHeaders(X_OKAPI_URL, X_OKAPI_TENANT, X_OKAPI_TOKEN);
    return verifyPut(url, body.encode(), headers,expectedContentType, expectedCode);
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

  Headers prepareHeaders(Header... headers) {
    return new Headers(headers);
  }
}
