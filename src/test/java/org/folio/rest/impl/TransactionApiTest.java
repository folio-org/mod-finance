package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ALLOCATION;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.CREDIT;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ENCUMBRANCE;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.PAYMENT;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.TRANSFER;
import static org.folio.rest.util.ErrorCodes.NEGATIVE_VALUE;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_NOT_RELEASED;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.ResourcePathResolver.BATCH_TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.rest.util.TestConfig.clearServiceInteractions;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.FUND;
import static org.folio.rest.util.TestEntities.TRANSACTIONS;
import static org.folio.rest.util.TestUtils.getMockData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.RestTestUtils;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class TransactionApiTest {
  private static final Logger logger = LogManager.getLogger(TransactionApiTest.class);
  private static boolean runningOnOwn;

  private static final String USD = "USD";
  private static final String FISCAL_YEAR_ID = "684b5dc5-92f6-4db7-b996-b549d88f5e4e";
  private static final String HIST_ID = "fb7b70f1-b898-4924-a991-0e4b6312bb5f";
  private static final String CANHIST_ID = "68872d8a-bf16-420b-829f-206da38f6c10";
  private static final String LATHIST_ID = "e6d7e91a-4dbc-4a70-9b38-e000d2fbdc79";
  private static final String ASIAHIST_ID = "55f48dc6-efa7-4cfe-bc7c-4786efe493e3";

  public static final String DELETE_TRANSACTION_ID = UUID.randomUUID().toString();
  public static final String DELETE_CONNECTED_TRANSACTION_ID = "ac857ef5-4e21-4929-8b97-1a4ae06add16";

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
    JsonObject allocationJson = JsonObject.mapFrom(transaction);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), allocationJson, APPLICATION_JSON, 201);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    var transferJson = JsonObject.mapFrom(transaction);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), transferJson, APPLICATION_JSON, 201);
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
    JsonObject transferBody = JsonObject.mapFrom(transaction);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), transferBody, APPLICATION_JSON, 201);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    var allocationBody = JsonObject.mapFrom(transaction);
    RestTestUtils.verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), allocationBody, APPLICATION_JSON, 201);
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

  @Test
  void testDeleteEncumbrance() {
    logger.info("=== Test delete encumbrance - Success ===");
    String id = DELETE_TRANSACTION_ID;
    Transaction transaction = createTransaction(ENCUMBRANCE)
      .withEncumbrance(new Encumbrance().withStatus(Encumbrance.Status.RELEASED));
    transaction.setId(id);
    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(transaction));
    RestTestUtils.verifyDeleteResponse(TestEntities.TRANSACTIONS_ENCUMBRANCE.getEndpointWithId(id), "", 204);
  }

  @Test
  void testDeleteEncumbranceConnectedToInvoice() {
    logger.info("=== Test delete encumbrance connected to an invoice - Unprocessable entity ===");
    String id = DELETE_CONNECTED_TRANSACTION_ID;
    Transaction transaction = createTransaction(ENCUMBRANCE)
      .withEncumbrance(new Encumbrance().withStatus(Encumbrance.Status.RELEASED));
    transaction.setId(id);
    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(transaction));
    RestTestUtils.verifyDeleteResponse(TestEntities.TRANSACTIONS_ENCUMBRANCE.getEndpointWithId(id), "", 422);
  }

  @Test
  void testDeleteUnreleasedEncumbrance() {
    logger.info("=== Test delete unreleased encumbrance===");
    String id = DELETE_CONNECTED_TRANSACTION_ID;
    Transaction transaction = createTransaction(ENCUMBRANCE)
      .withEncumbrance(new Encumbrance().withStatus(Encumbrance.Status.UNRELEASED));
    transaction.setId(id);
    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(transaction));
    Errors err = RestTestUtils.verifyDeleteResponse(TestEntities.TRANSACTIONS_ENCUMBRANCE.getEndpointWithId(id), "", 400)
      .as(Errors.class);

    assertEquals(TRANSACTION_NOT_RELEASED.getCode(), err.getErrors().get(0).getCode());
  }

  @Test
  void testUpdatePayment() {
    logger.info("=== Test update payment - Success ===");
    Transaction transaction = TestEntities.TRANSACTIONS_PAYMENT.getMockObject().mapTo(Transaction.class);
    transaction.setInvoiceCancelled(true);
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_PAYMENT.getEndpointWithId(transaction.getId()),
      JsonObject.mapFrom(transaction), "", 204);
  }

  @Test
  void testUpdatePaymentWithoutId() {
    logger.info("=== Test update payment without id - Success ===");
    Transaction transaction = TestEntities.TRANSACTIONS_PAYMENT.getMockObject().mapTo(Transaction.class);
    String id = transaction.getId();
    transaction.setId(null);
    transaction.setInvoiceCancelled(true);
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_PAYMENT.getEndpointWithId(id),
      JsonObject.mapFrom(transaction), "", 204);
  }

  @Test
  void testUpdatePaymentIdsMismatch() {
    logger.info("=== Test update payment with ids mismatch - Unprocessable entity ===");
    String id = UUID.randomUUID().toString();
    Transaction transaction = createTransaction(PAYMENT);
    transaction.setId(UUID.randomUUID().toString());
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_PAYMENT.getEndpointWithId(id), JsonObject.mapFrom(transaction),
      APPLICATION_JSON, 422);
  }

  @Test
  void testUpdatePaymentWithAnotherChange() {
    logger.info("=== Test update payment with another change - Unprocessable entity ===");
    Transaction transaction = TestEntities.TRANSACTIONS_PAYMENT.getMockObject().mapTo(Transaction.class);
    transaction.setDescription("Test fail");
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_PAYMENT.getEndpointWithId(transaction.getId()),
      JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testUpdatePaymentWithoutInvoiceCancelled() {
    logger.info("=== Test update payment without invoiceCancelled - Unprocessable entity ===");
    Transaction transaction = TestEntities.TRANSACTIONS_PAYMENT.getMockObject().mapTo(Transaction.class);
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_PAYMENT.getEndpointWithId(transaction.getId()),
      JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testUpdateCredit() {
    logger.info("=== Test update credit - Success ===");
    Transaction transaction = TestEntities.TRANSACTIONS_CREDIT.getMockObject().mapTo(Transaction.class);
    transaction.setInvoiceCancelled(true);
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_CREDIT.getEndpointWithId(transaction.getId()),
      JsonObject.mapFrom(transaction), "", 204);
  }

  @Test
  void testUpdateCreditWithoutId() {
    logger.info("=== Test update credit without id - Success ===");
    Transaction transaction = TestEntities.TRANSACTIONS_CREDIT.getMockObject().mapTo(Transaction.class);
    String id = transaction.getId();
    transaction.setId(null);
    transaction.setInvoiceCancelled(true);
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_CREDIT.getEndpointWithId(id),
      JsonObject.mapFrom(transaction), "", 204);
  }

  @Test
  void testUpdateCreditIdsMismatch() {
    logger.info("=== Test update credit with ids mismatch - Unprocessable entity ===");
    String id = UUID.randomUUID().toString();
    Transaction transaction = createTransaction(CREDIT);
    transaction.setId(UUID.randomUUID().toString());
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_CREDIT.getEndpointWithId(id), JsonObject.mapFrom(transaction),
      APPLICATION_JSON, 422);
  }

  @Test
  void testUpdateCreditWithAnotherChange() {
    logger.info("=== Test update credit with another change - Unprocessable entity ===");
    Transaction transaction = TestEntities.TRANSACTIONS_CREDIT.getMockObject().mapTo(Transaction.class);
    transaction.setDescription("Test fail");
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_CREDIT.getEndpointWithId(transaction.getId()),
      JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testUpdateCreditWithoutInvoiceCancelled() {
    logger.info("=== Test update credit without invoiceCancelled - Unprocessable entity ===");
    Transaction transaction = TestEntities.TRANSACTIONS_CREDIT.getMockObject().mapTo(Transaction.class);
    RestTestUtils.verifyPut(TestEntities.TRANSACTIONS_CREDIT.getEndpointWithId(transaction.getId()),
      JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  void testTransactionBatch() throws Exception {
    String batchAsString = getMockData("mockdata/transactions/batch_with_patch.json");
    RestTestUtils.verifyPostResponse(resourcesPath(BATCH_TRANSACTIONS), new JsonObject(batchAsString), "", 204);
  }

  @Test
  void testTransactionBatchError() throws Exception {
    String batchAsString = getMockData("mockdata/transactions/batch_with_delete.json");
    RestTestUtils.verifyPostResponse(resourcesPath(BATCH_TRANSACTIONS), new JsonObject(batchAsString), "", 422);
  }

  private Transaction createTransaction(Transaction.TransactionType type) {
    return new Transaction()
      .withAmount(25.0)
      .withCurrency(USD)
      .withFiscalYearId(FISCAL_YEAR_ID)
      .withSource(Transaction.Source.USER)
      .withTransactionType(type);
  }
}
