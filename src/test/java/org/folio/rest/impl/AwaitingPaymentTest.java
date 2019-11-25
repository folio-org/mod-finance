package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.folio.rest.util.HelperUtils.getFiscalYearDuration;
import static org.folio.rest.util.MockServer.getQueryParams;
import static org.folio.rest.util.TestEntities.FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.TRANSACTIONS_ENCUMBRANCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Transaction;
import org.junit.jupiter.api.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class AwaitingPaymentTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(AwaitingPaymentTest.class);

  @Test
   void testPostAwaitingPayment() {
    logger.info("=== Test POST Awaiting Payment ===");
    Transaction transaction = TRANSACTIONS_ENCUMBRANCE.getMockObject().mapTo(Transaction.class);
    Transaction compositeFund = verifyPostResponse(TRANSACTIONS_ENCUMBRANCE.getEndpoint(), transaction, APPLICATION_JSON, CREATED.getStatusCode()).as(Transaction.class);
  }

  private void verifyCurrentFYQuery(FiscalYear fiscalYearOne) {
    String query = getQueryParams(FISCAL_YEAR.name()).get(0);
    String now = LocalDate.now(Clock.systemUTC()).toString();
    String next = LocalDateTime.now(Clock.systemUTC()).plus(getFiscalYearDuration(fiscalYearOne)).toLocalDate().toString();
    assertThat(query, containsString(fiscalYearOne.getSeries()));
    assertThat(query, containsString(now));
    assertThat(query, containsString(next));
  }
}
