package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.MockServer.getRqRsEntries;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Errors;
import org.junit.jupiter.api.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class BudgetsTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(BudgetsTest.class);
  public static final String BUDGET_WITH_BOUNDED_TRANSACTION_ID = "34fe0c8b-2b99-4fe2-81a5-4ed6872a32e8";

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

  @Test
  public void testUpdateBudgetTotalsValuesFromRequestShouldBeIgnored() {

    Budget newBudget = BUDGET.getMockObject().mapTo(Budget.class)
      .withAllocated(0d)
      .withAvailable(0d)
      .withUnavailable(0d)
      .withAwaitingPayment(0d)
      .withEncumbered(0d)
      .withExpenditures(0d)
      .withOverEncumbrance(0d)
      .withOverExpended(0d);

    Budget budgetFromStorage = new Budget()
      .withId(newBudget.getId())
      .withAllocated(100d)
      .withAvailable(70d)
      .withUnavailable(30d)
      .withAwaitingPayment(10d)
      .withEncumbered(15d)
      .withExpenditures(15d)
      .withOverEncumbrance(5d)
      .withOverExpended(5d);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(budgetFromStorage));


    // set budget allowable amounts to 1%
    newBudget.setAllowableEncumbrance(1000d);
    newBudget.setAllowableExpenditure(1000d);


    verifyPut(BUDGET.getEndpointWithDefaultId(), newBudget, "", 204);

    List<JsonObject> jsonObjects = getRqRsEntries(HttpMethod.PUT, BUDGET.name());
    assertThat(jsonObjects, hasSize(1));
    Budget budgetFromUpdate = jsonObjects.get(0).mapTo(Budget.class);

    assertEquals(budgetFromStorage.getAllocated(), budgetFromUpdate.getAllocated());
    assertEquals(budgetFromStorage.getAvailable(), budgetFromUpdate.getAvailable());
    assertEquals(budgetFromStorage.getUnavailable(), budgetFromUpdate.getUnavailable());
    assertEquals(budgetFromStorage.getEncumbered(), budgetFromUpdate.getEncumbered());
    assertEquals(budgetFromStorage.getExpenditures(), budgetFromUpdate.getExpenditures());
    assertEquals(budgetFromStorage.getAwaitingPayment(), budgetFromUpdate.getAwaitingPayment());
    assertEquals(budgetFromStorage.getOverExpended(), budgetFromUpdate.getOverExpended());
    assertEquals(budgetFromStorage.getOverEncumbrance(), budgetFromUpdate.getOverEncumbrance());
  }

  @Test
  public void testDeleteShouldFailIfThereAreTransactionBoundedToBudget() {
    logger.info("=== Test Delete of the budget is forbidden, if budget related transactions found ===");

    Errors errors = verifyDeleteResponse(BUDGET.getEndpointWithId(BUDGET_WITH_BOUNDED_TRANSACTION_ID), APPLICATION_JSON, 400).then()
      .extract()
      .as(Errors.class);

    assertEquals(TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR.getDescription(), errors.getErrors().get(0).getMessage());
    assertEquals(TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR.getCode(), errors.getErrors().get(0).getCode());
  }

  @Test
  public void testDeleteShouldSuccessIfNoTransactionBoundedToBudget() {
    logger.info("=== Test Delete of the budget, if no transactions were found. ===");

    verifyDeleteResponse(BUDGET.getEndpointWithDefaultId(), EMPTY, 204);
  }
}
