package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.ErrorCodes.BUDGET_IS_INACTIVE;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.folio.rest.util.TestEntities.TRANSACTIONS_ENCUMBRANCE;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TransactionsTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(TransactionsTest.class);

  @Test
  public void testCreateTransactionFromInactiveBudget() {

    logger.info("=== Test Get Composite Fund record by id, current Fiscal Year not found ===");

    Budget budget = BUDGET.getMockObject().mapTo(Budget.class);
    budget.setBudgetStatus(Budget.BudgetStatus.INACTIVE);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(budget));

    Transaction transaction = TRANSACTIONS_ENCUMBRANCE.getMockObject().mapTo(Transaction.class);

    Errors errors = verifyPostResponse(TRANSACTIONS_ENCUMBRANCE.getEndpoint(), transaction, APPLICATION_JSON, 422).as(Errors.class);

    Assertions.assertEquals(errors.getErrors().get(0).getCode(), BUDGET_IS_INACTIVE.getCode());
  }

}
