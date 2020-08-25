package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ALLOCATION;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ENCUMBRANCE;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.PAYMENT;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.TRANSFER;
import static org.folio.rest.util.ErrorCodes.NEGATIVE_VALUE;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestConfig.clearServiceInteractions;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.FUND;
import static org.folio.rest.util.TestUtils.getMockData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.RestTestUtils;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TransactionApiTest {
  private static final Logger logger = LoggerFactory.getLogger(TransactionApiTest.class);
  private static boolean runningOnOwn;

  private final String dollars = "USD";
  private final String FISCAL_YEAR_ID = "684b5dc5-92f6-4db7-b996-b549d88f5e4e";
  private final String HIST_ID = "fb7b70f1-b898-4924-a991-0e4b6312bb5f";
  private final String CANHIST_ID = "68872d8a-bf16-420b-829f-206da38f6c10";
  private final String LATHIST_ID = "e6d7e91a-4dbc-4a70-9b38-e000d2fbdc79";
  private final String ASIAHIST_ID = "55f48dc6-efa7-4cfe-bc7c-4786efe493e3";

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
  static void afterAll() {
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
  }

  @Test
  void shouldReturnResponseWithErrorWhenPostPaymentWithNegativeAmount() {

    Transaction transaction = TestEntities.TRANSACTIONS_PAYMENT.getMockObject().mapTo(Transaction.class);
    transaction.setAmount(-1d);
    Errors errors = RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_PAYMENT.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals(NEGATIVE_VALUE.toError().getCode(), errors.getErrors().get(0).getCode());
  }

  @Test
  void shouldReturnResponseWithErrorWhenPostCreditWithNegativeAmount() {

    Transaction transaction = TestEntities.TRANSACTIONS_CREDIT.getMockObject().mapTo(Transaction.class);
    transaction.setAmount(-1d);
    Errors errors = RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_CREDIT.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals(NEGATIVE_VALUE.toError().getCode(), errors.getErrors().get(0).getCode());
  }

  @Test
  void testTransferAndAllocationWithMatchingAllocatedIds() throws Exception {
    logger.info("=== Test transfer and allocation toFund.allocatedFromIds matches fromFund.allocatedToIds) - Success ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/HIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(HIST_ID)
      .withToFundId(CANHIST_ID);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);
  }

  @Test
  void testTransferAndAllocationWithEmptyAllocatedIds() throws Exception {
    logger.info("=== Test transfer and allocation with empty toFund.allocatedFromIds and fromFund.allocatedToIds - Success ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/LATHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(CANHIST_ID)
      .withToFundId(LATHIST_ID);
    JsonObject body = JsonObject.mapFrom(transaction);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    body = JsonObject.mapFrom(transaction);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);
  }

  @Test
  void testTransferAndAllocationWithEmptyAllocatedFromIds() throws Exception {
    logger.info("=== Test transfer and allocation with empty toFund.allocatedFromIds) - Success ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/HIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/LATHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(HIST_ID)
      .withToFundId(LATHIST_ID);
    JsonObject body = JsonObject.mapFrom(transaction);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    body = JsonObject.mapFrom(transaction);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);
  }

  @Test
  void testTransferAndAllocationWithAllocatedIdsMismatch() throws Exception {
    logger.info("=== Test transfer and allocation with toFund.allocatedFromIds and fromFund.allocatedToIds mismatch - Unprocessable entity ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/ASIAHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(ASIAHIST_ID)
      .withToFundId(CANHIST_ID);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testTransferAndAllocationWithEmptyAllocatedFromIdsMismatch() throws Exception {
    logger.info("=== Test transfer and allocation with empty fromFund.allocatedToIds mismatch - Unprocessable entity ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/LATHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(LATHIST_ID)
      .withToFundId(CANHIST_ID);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testTransferMissingToOrFromFundId() {
    logger.info("=== Test transfer missing toFundId or fromFundId - Unprocessable entity ===");

    // missing toFundId
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(HIST_ID);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);

    // missing fromFundId
    transaction = createTransaction(TRANSFER)
      .withToFundId(HIST_ID);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testExternalAllocation() {
    logger.info("=== Test external allocation (missing fromFundId or toFundId) - Success ===");
    // external from allocation
    Transaction transaction = createTransaction(ALLOCATION)
      .withFromFundId(HIST_ID);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);

    // external to allocation
    transaction.setTransactionType(ALLOCATION);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);
  }

  @Test
  void testAllocationMissingBothFundIds() {
    logger.info("=== Test allocation missing both FROM and TO fund ids - Unprocessable entity ===");
    Transaction transaction = createTransaction(ALLOCATION);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testUpdateEncumbrance() {
    logger.info("=== Test update encumbrance - Success ===");
    String id = UUID.randomUUID().toString();
    Transaction transaction = createTransaction(ENCUMBRANCE);
    transaction.setId(id);
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_ENCUMBRANCE.getEndpointWithId(id), JsonObject.mapFrom(transaction), "", 204);
  }

  @Test
  void testUpdateEncumbranceIdsMismatch() {
    logger.info("=== Test update encumbrance with ids mismatch - Unprocessable entity ===");
    String id = UUID.randomUUID().toString();
    Transaction transaction = createTransaction(ENCUMBRANCE);
    transaction.setId(UUID.randomUUID().toString());
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_ENCUMBRANCE.getEndpointWithId(id), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testUpdateNonEncumbranceTransaction() {
    logger.info("=== Test update non-encumbrance transaction - Unprocessable entity ===");
    String id = UUID.randomUUID().toString();
    Transaction transaction = createTransaction(PAYMENT);
    transaction.setId(id);
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_ENCUMBRANCE.getEndpointWithId(id), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  private Transaction createTransaction(Transaction.TransactionType type) {
    return new Transaction()
      .withAmount(25.0)
      .withCurrency(dollars)
      .withFiscalYearId(FISCAL_YEAR_ID)
      .withSource(Transaction.Source.USER)
      .withTransactionType(type);
  }
}
