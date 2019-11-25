package org.folio.rest.impl;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.folio.rest.util.TestEntities.TRANSACTIONS;

import java.io.IOException;

import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.folio.rest.util.MockServer;
import org.folio.rest.util.MoneyUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class AwaitingPaymentTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(AwaitingPaymentTest.class);

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
    Double currentExpended = transaction.getEncumbrance().getAmountExpended();

    Double updatedAwaitingPayment = updatedTransaction.getEncumbrance().getAmountAwaitingPayment();
    Double updatedExpended = updatedTransaction.getEncumbrance().getAmountExpended();

    Assert.assertEquals(updatedAwaitingPayment,MoneyUtils.sumDoubleValues(currentAwaitingPayment, amount, transaction.getCurrency()), 2);
    Assert.assertEquals(updatedExpended, MoneyUtils.subtractDoubleValues(currentExpended, amount, transaction.getCurrency()), 2);
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
