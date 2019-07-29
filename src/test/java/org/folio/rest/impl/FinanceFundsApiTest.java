package org.folio.rest.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.Test;

import java.io.IOException;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

public class FinanceFundsApiTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(FinanceFundsApiTest.class);
  public static final String FINANCE_PATH = "/finance/funds";
  public static final String FINANCE_PATH_ID = "/finance/funds" + "/%s";

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
    verifyPostResponse(FINANCE_PATH, body, prepareHeaders(X_OKAPI_TENANT), "", 500);
  }

  @Test
  public void testUpdateFinanceFund() throws IOException {
    logger.info("=== Test update Finance Fund ===");

    String jsonBody = "{}";
    verifyPut(String.format(FINANCE_PATH_ID, VALID_UUID), jsonBody, TEXT_PLAIN, 500);
  }

  @Test
  public void testDeleteFinanceFund() {
    logger.info("=== Test delete Finance Fund ===");

    verifyDeleteResponse(String.format(FINANCE_PATH_ID, VALID_UUID), "", 500);
  }
}
