package org.folio.rest.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.Test;

import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class FinanceFundsApiTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(FinanceFundsApiTest.class);
  private static final String FINANCE_PATH = "/finance/funds";
  private static final String FINANCE_PATH_ID = FINANCE_PATH + "/%s";

  @Test
  public void testGetFinanceFunds() {
    logger.info("=== Test Get Finances ===");

    verifyGet(FINANCE_PATH, "", 500);
  }

  @Test
  public void testGetFinanceFundById() {
    logger.info("=== Test Get Finances by id ===");

    verifyGet(String.format(FINANCE_PATH_ID, VALID_UUID), "", 500);
  }

  @Test
  public void testPostFinanceFund() throws Exception {
    logger.info("=== Test create Finance Fund ===");

    String body = "{}";
    verifyPostResponse(FINANCE_PATH, body, prepareHeaders(X_OKAPI_TENANT), "", 422);
  }

  @Test
  public void testUpdateFinanceFund() throws IOException {
    logger.info("=== Test update Finance Fund ===");

    String jsonBody = "{}";
    verifyPut(String.format(FINANCE_PATH_ID, VALID_UUID), jsonBody, APPLICATION_JSON, 422);
  }

  @Test
  public void testDeleteFinanceFund() {
    logger.info("=== Test delete Finance Fund ===");

    verifyDeleteResponse(String.format(FINANCE_PATH_ID, VALID_UUID), "", 500);
  }
}
