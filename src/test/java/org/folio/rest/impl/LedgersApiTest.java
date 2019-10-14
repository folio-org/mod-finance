package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

import java.util.UUID;

import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class LedgersApiTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(LedgersApiTest.class);

  @Test
  void testGetLedgerGroupsById() {
    logger.info("=== Test Get collection of groups records by ledgerId ===");

    String query = buildQueryParam("fund.id==" + UUID.randomUUID().toString());

    verifyGet(TestEntities.LEDGER.getEndpoint() + "/" + VALID_UUID + "/groups" + query, APPLICATION_JSON, OK.getStatusCode())
      .as(GroupFundFiscalYearCollection.class);
  }

  @Test
  void testGetLedgerGroupsByIdWithEmptyQuery() {
    logger.info("=== Test Get collection of groups records by ledgerId with empty query ===");

    verifyGet(TestEntities.LEDGER.getEndpoint() + "/" + VALID_UUID + "/groups", APPLICATION_JSON, OK.getStatusCode())
      .as(GroupFundFiscalYearCollection.class);
  }
}
