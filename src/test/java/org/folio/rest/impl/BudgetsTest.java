package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.hamcrest.Matchers.containsString;

import org.folio.rest.jaxrs.model.Budget;
import org.junit.jupiter.api.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BudgetsTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(BudgetsTest.class);

  @Test
  public void testUpdateBudgetWithExceededAllowableAmounts() {

    logger.info("=== Test Update budget with exceeded limit of allowable encumbrance and expenditures ===");

    Budget budget = verifyGet(BUDGET.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode()).as(Budget.class);

    // set budget allowable amounts to 1%
    budget.setAllowableEncumbrance(1d);
    budget.setAllowableExpenditure(1d);

    budget.setAwaitingPayment((double) Integer.MAX_VALUE);

    verifyPut(BUDGET.getEndpointWithDefaultId(), budget, APPLICATION_JSON, 422).then()
      .body(containsString(ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED.getCode()), containsString(ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED.getCode()));
  }

}
