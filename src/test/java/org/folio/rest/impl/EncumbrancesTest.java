package org.folio.rest.impl;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.folio.rest.util.ErrorCodes.INVALID_TRANSACTION_TYPE;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.RestTestUtils.verifyPostResponse;
import static org.folio.rest.util.TestConfig.clearServiceInteractions;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.TRANSACTIONS;
import static org.folio.rest.util.TestUtils.getMockData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.folio.rest.util.MockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

public class EncumbrancesTest {

  private static final Logger logger = LogManager.getLogger(EncumbrancesTest.class);
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
  void testPostReleaseEncumbrance() {
    logger.info("=== Test POST Release Encumbrance ===");

    String encumbranceID = "5c9f769c-5fe2-4a6e-95fa-021f0d8834a0";

    verifyPostResponse("/finance/release-encumbrance/" + encumbranceID , null, "", NO_CONTENT.getStatusCode());

    Batch batch = MockServer.getRqRsEntries(HttpMethod.POST, "batchTransactions").get(0).mapTo(Batch.class);
    assertThat(batch.getTransactionsToUpdate(), hasSize(1));
    Transaction updatedEncumbrance = batch.getTransactionsToUpdate().get(0);
    assertEquals(Encumbrance.Status.RELEASED, updatedEncumbrance.getEncumbrance().getStatus());
  }

  @Test
  void testPostReleaseNonEncumbrance() throws IOException {
    logger.info("=== Test POST Release non Encumbrance transaction ===");

    String transactionID = "a0b1e290-c42f-435a-b9d7-4ae7f77eb4ef";
    Transaction allocation = new JsonObject(getMockData("mockdata/transactions/allocations.json")).mapTo(TransactionCollection.class).getTransactions().get(0);

    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(allocation));
    Errors errors = verifyPostResponse("/finance/release-encumbrance/" + transactionID, null, "", 422).then()
      .extract()
      .as(Errors.class);

    assertEquals(INVALID_TRANSACTION_TYPE.getCode(), errors.getErrors().get(0).getCode());
  }

  @Test
  void testPostReleasedEncumbrance() throws IOException {
    logger.info("=== Test POST Released Encumbrance transaction ===");

    String transactionID = "5c9f769c-5fe2-4a6e-95fa-021f0d8834a0";
    Transaction releasedEncumbrance = new JsonObject(getMockData("mockdata/transactions/encumbrances.json")).mapTo(TransactionCollection.class).getTransactions().get(0);
    releasedEncumbrance.getEncumbrance().setStatus(Encumbrance.Status.UNRELEASED);
    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(releasedEncumbrance));

    verifyPostResponse("/finance/release-encumbrance/" + transactionID , null, "", NO_CONTENT.getStatusCode());

    Batch batch = MockServer.getRqRsEntries(HttpMethod.POST, "batchTransactions").get(0).mapTo(Batch.class);
    assertThat(batch.getTransactionsToUpdate(), hasSize(1));
    Transaction updatedEncumbrance = batch.getTransactionsToUpdate().get(0);
    assertEquals(Encumbrance.Status.RELEASED, updatedEncumbrance.getEncumbrance().getStatus());
  }

  @Test
  void testPostEncumbranceInvalidId() {
    logger.info("=== Test POST release encumbrance bad ID ===");

    String transactionID = "bad-encumbrance-id";

    verifyPostResponse("/finance/release-encumbrance/" + transactionID , null, "", BAD_REQUEST.getStatusCode());
  }

  @Test
  void testPostUnreleaseEncumbrance() throws IOException {
    logger.info("=== Test POST Unrelease Encumbrance ===");

    String encumbranceID = "5c9f769c-5fe2-4a6e-95fa-021f0d8834a0";
    Transaction releasedEncumbrance = new JsonObject(getMockData("mockdata/transactions/encumbrances.json")).mapTo(TransactionCollection.class).getTransactions().get(0);
    releasedEncumbrance.getEncumbrance().setStatus(Encumbrance.Status.RELEASED);
    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(releasedEncumbrance));

    verifyPostResponse("/finance/unrelease-encumbrance/" + encumbranceID , null, "", NO_CONTENT.getStatusCode());

    Batch batch = MockServer.getRqRsEntries(HttpMethod.POST, "batchTransactions").get(0).mapTo(Batch.class);
    assertThat(batch.getTransactionsToUpdate(), hasSize(1));
    Transaction updatedEncumbrance = batch.getTransactionsToUpdate().get(0);
    assertEquals(Encumbrance.Status.UNRELEASED, updatedEncumbrance.getEncumbrance().getStatus());
  }

  @Test
  void testPostUnreleaseNonEncumbrance() throws IOException {
    logger.info("=== Test POST Unrelease non Encumbrance transaction ===");

    String transactionID = "a0b1e290-c42f-435a-b9d7-4ae7f77eb4ef";
    Transaction allocation = new JsonObject(getMockData("mockdata/transactions/allocations.json"))
      .mapTo(TransactionCollection.class).getTransactions().get(0);

    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(allocation));

    Errors errors = verifyPostResponse("/finance/unrelease-encumbrance/" + transactionID, null, "", 422)
      .then()
      .extract()
      .as(Errors.class);

    assertEquals(INVALID_TRANSACTION_TYPE.getCode(), errors.getErrors().get(0).getCode());
  }

  @Test
  void testPostUnreleasedEncumbrance() throws IOException {
    logger.info("=== Test POST Unreleased Encumbrance transaction ===");

    String transactionID = "5c9f769c-5fe2-4a6e-95fa-021f0d8834a0";
    Transaction releasedEncumbrance = new JsonObject(getMockData("mockdata/transactions/encumbrances.json"))
      .mapTo(TransactionCollection.class).getTransactions().get(0);

    releasedEncumbrance.getEncumbrance().setStatus(Encumbrance.Status.UNRELEASED);
    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(releasedEncumbrance));

    verifyPostResponse("/finance/unrelease-encumbrance/" + transactionID , null, "", NO_CONTENT.getStatusCode());
  }

  @Test
  void testPostEncumbranceToUnreleaseInvalidId() {
    logger.info("=== Test POST unrelease encumbrance bad ID ===");

    String transactionID = "bad-encumbrance-id";

    verifyPostResponse("/finance/unrelease-encumbrance/" + transactionID , null, "", BAD_REQUEST.getStatusCode());
  }

}
