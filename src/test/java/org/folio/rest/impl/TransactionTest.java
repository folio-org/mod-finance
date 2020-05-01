package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ALLOCATION;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.TRANSFER;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestEntities.FUND;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.Test;

public class TransactionTest extends ApiTestBase {
  private static final Logger logger = LoggerFactory.getLogger(TransactionTest.class);

  private final String dollars = "USD";
  private final String FISCAL_YEAR_ID = "684b5dc5-92f6-4db7-b996-b549d88f5e4e";
  private final String HIST_ID = "fb7b70f1-b898-4924-a991-0e4b6312bb5f";
  private final String CANHIST_ID = "68872d8a-bf16-420b-829f-206da38f6c10";
  private final String LATHIST_ID = "e6d7e91a-4dbc-4a70-9b38-e000d2fbdc79";
  private final String ASIAHIST_ID = "55f48dc6-efa7-4cfe-bc7c-4786efe493e3";

  @Test
  public void testTransferAndAllocationWithMatchingAllocatedIds() throws Exception {
    logger.info("=== Test transfer and allocation toFund.allocatedFromIds matches fromFund.allocatedToIds) - Success ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/HIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(HIST_ID)
      .withToFundId(CANHIST_ID);
    verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);
  }

  @Test
  public void testTransferAndAllocationWithEmptyAllocatedIds() throws Exception {
    logger.info("=== Test transfer and allocation with empty toFund.allocatedFromIds and fromFund.allocatedToIds - Success ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/LATHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(CANHIST_ID)
      .withToFundId(LATHIST_ID);
    JsonObject body = JsonObject.mapFrom(transaction);
    verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    body = JsonObject.mapFrom(transaction);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);
  }

  @Test
  public void testTransferAndAllocationWithEmptyAllocatedFromIds() throws Exception {
    logger.info("=== Test transfer and allocation with empty toFund.allocatedFromIds) - Success ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/HIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/LATHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(HIST_ID)
      .withToFundId(LATHIST_ID);
    JsonObject body = JsonObject.mapFrom(transaction);
    verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    body = JsonObject.mapFrom(transaction);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);
  }

  @Test
  public void testTransferAndAllocationWithAllocatedIdsMismatch() throws Exception {
    logger.info("=== Test transfer and allocation with toFund.allocatedFromIds and fromFund.allocatedToIds mismatch - Unprocessable entity ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/ASIAHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(ASIAHIST_ID)
      .withToFundId(CANHIST_ID);
    verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  public void testTransferAndAllocationWithEmptyAllocatedFromIdsMismatch() throws Exception {
    logger.info("=== Test transfer and allocation with empty fromFund.allocatedToIds mismatch - Unprocessable entity ===");
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/CANHIST.json")));
    addMockEntry(FUND.name(), new JsonObject(getMockData("mockdata/funds/LATHIST.json")));

    // transfer
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(LATHIST_ID)
      .withToFundId(CANHIST_ID);
    verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);

    // allocation
    transaction.setTransactionType(ALLOCATION);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  public void testTransferMissingToOrFromFundId() {
    logger.info("=== Test transfer missing toFundId or fromFundId - Unprocessable entity ===");

    // missing toFundId
    Transaction transaction = createTransaction(TRANSFER)
      .withFromFundId(HIST_ID);
    verifyPostResponse(TestEntities.TRANSACTIONS_TRANSFER.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);

    // missing fromFundId
    transaction = createTransaction(TRANSFER)
      .withToFundId(HIST_ID);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
  }

  @Test
  public void testExternalAllocation() {
    logger.info("=== Test external allocation (missing fromFundId or toFundId) - Success ===");
    // external from allocation
    Transaction transaction = createTransaction(ALLOCATION)
      .withFromFundId(HIST_ID);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);

    // external to allocation
    transaction.setTransactionType(ALLOCATION);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 201);
  }

  @Test
  public void testAllocationMissingBothFundIds() {
    logger.info("=== Test allocation missing both FROM and TO fund ids - Unprocessable entity ===");
    Transaction transaction = createTransaction(ALLOCATION);
    verifyPostResponse(TestEntities.TRANSACTIONS_ALLOCATION.getEndpoint(), JsonObject.mapFrom(transaction), APPLICATION_JSON, 422);
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
