package org.folio.rest.impl;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestEntities.TRANSACTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.MoneyUtils;
import org.junit.jupiter.api.Test;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class EncumbrancesTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(EncumbrancesTest.class);

  @Test
  void testPostAwaitingPayment() throws IOException {
    logger.info("=== Test POST Awaiting Payment ===");

    String encumbranceID = "5c9f769c-5fe2-4a6e-95fa-021f0d8834a0";
    Transaction transaction = getTransactionMockById(encumbranceID);

    AwaitingPayment awaitingPayment = new JsonObject(getMockData("mockdata/awating_payment/awaiting_payment_1.json")).mapTo(AwaitingPayment.class);
    verifyPostResponse("/finance/awaiting-payment", awaitingPayment, "", NO_CONTENT.getStatusCode());

    Transaction updatedTransaction = MockServer.getRqRsEntries(HttpMethod.PUT, TRANSACTIONS.name()).get(0).mapTo(Transaction.class);

    Double amount = awaitingPayment.getAmountAwaitingPayment();
    Double currentAwaitingPayment = transaction.getEncumbrance().getAmountAwaitingPayment();

    Double updatedAwaitingPayment = updatedTransaction.getEncumbrance().getAmountAwaitingPayment();

    assertEquals(updatedAwaitingPayment, MoneyUtils.sumDoubleValues(currentAwaitingPayment, amount, transaction.getCurrency()), 2);
    assertEquals(updatedTransaction.getEncumbrance().getStatus(), awaitingPayment.getReleaseEncumbrance() ? Encumbrance.Status.RELEASED : Encumbrance.Status.UNRELEASED);
  }

  @Test
  void testPostReleaseEncumbrance() throws IOException {
    logger.info("=== Test POST Release Encumbrance ===");

    String encumbranceID = "5c9f769c-5fe2-4a6e-95fa-021f0d8834a0";

    verifyPostResponse("/finance/release-encumbrance/" + encumbranceID , null, "", NO_CONTENT.getStatusCode());

    Transaction updatedEncumbrance = MockServer.getRqRsEntries(HttpMethod.PUT, TRANSACTIONS.name()).get(0).mapTo(Transaction.class);
    assertEquals(Encumbrance.Status.RELEASED, updatedEncumbrance.getEncumbrance().getStatus());
  }

  @Test
  void testPostReleaseNonEncumbrance() throws IOException {
    logger.info("=== Test POST Release non Encumbrance transaction ===");

    String transactionID = "a0b1e290-c42f-435a-b9d7-4ae7f77eb4ef";
    Transaction allocation = new JsonObject(getMockData("mockdata/transactions/allocations.json")).mapTo(TransactionCollection.class).getTransactions().get(0);

    addMockEntry(TRANSACTIONS.name(), JsonObject.mapFrom(allocation));
    Errors errors = verifyPostResponse("/finance/release-encumbrance/" + transactionID, null, "", BAD_REQUEST.getStatusCode()).then()
      .extract()
      .as(Errors.class);

    assertEquals("Transaction type mismatch. Encumbrance expected", errors.getErrors().get(0).getMessage());

  }

  private Transaction getTransactionMockById(String id) throws IOException {
    return new JsonObject(getMockData("mockdata/transactions/transactions.json"))
      .mapTo(TransactionCollection.class)
      .getTransactions()
      .stream()
      .filter(tr -> tr.getId().equals(id))
      .findFirst()
      .get();
  }

}
