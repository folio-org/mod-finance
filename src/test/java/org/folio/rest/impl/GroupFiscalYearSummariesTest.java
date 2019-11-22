package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.folio.rest.util.TestEntities.GROUP_FUND_FISCAL_YEAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummaryCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.resource.FinanceGroupFiscalYearSummaries;
import org.folio.rest.util.HelperUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class GroupFiscalYearSummariesTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(GroupFiscalYearSummariesTest.class);

  @Test
  public void testGetGroupFiscalYearSummariesCollection() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection ===");

    GroupFundFiscalYear firstGroupFundFiscalYear = buildGroupFundFiscalYear();
    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(firstGroupFundFiscalYear));

    Budget firstBudget = buildBudget(firstGroupFundFiscalYear.getFundId(), firstGroupFundFiscalYear.getFiscalYearId(), 100, 50, 0);
    Budget secondBudget = buildBudget(firstGroupFundFiscalYear.getFundId(), firstGroupFundFiscalYear.getFiscalYearId(), 0, 50, 100);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(firstBudget));
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(secondBudget));

    GroupFundFiscalYear secondGroupFundFiscalYear = buildGroupFundFiscalYear();

    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(secondGroupFundFiscalYear));
    Budget thirdBudget = buildBudget(secondGroupFundFiscalYear.getFundId(), secondGroupFundFiscalYear.getFiscalYearId(), 999, 0, 999);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(thirdBudget));

    GroupFiscalYearSummaryCollection response = verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);

    List<GroupFiscalYearSummary> actualSummaries = response.getGroupFiscalYearSummaries();
    assertThat(actualSummaries, hasSize(2));
    assertThat(response.getTotalRecords(), is(2));

    Map<String, List<GroupFiscalYearSummary>> actualSummariesMap = actualSummaries.stream().collect(Collectors.groupingBy(GroupFiscalYearSummary::getGroupId));

    validateData(firstGroupFundFiscalYear, actualSummariesMap, firstBudget.getAllocated() + secondBudget.getAllocated(), firstBudget.getAvailable() + secondBudget.getAvailable(), firstBudget.getUnavailable() + secondBudget.getUnavailable());
    validateData(secondGroupFundFiscalYear, actualSummariesMap, thirdBudget.getAllocated(), thirdBudget.getAvailable(), thirdBudget.getUnavailable());

  }

  @Test
  public void testGetCollectionGffyNotFound() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection - Group Fund Fiscal Years Not Found ===");

    GroupFiscalYearSummaryCollection collection = verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class) + buildQueryParam("id==(" + UUID.randomUUID().toString() + ")"), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);
    assertThat(collection.getTotalRecords(), is(0));
    assertThat(collection.getTotalRecords(), is(0));
  }

  @Test
  public void testGetCollectionBudgetNotFound() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection - Budgets Not Found ===");
    GroupFundFiscalYear groupFundFiscalYear = buildGroupFundFiscalYear();
    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(groupFundFiscalYear));
    GroupFiscalYearSummaryCollection collection = verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class) + buildQueryParam("id==(" + groupFundFiscalYear.getId() +")"), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);

    assertThat(collection.getTotalRecords(), is(1));
    assertThat(collection.getGroupFiscalYearSummaries(), hasSize(1));

    GroupFiscalYearSummary summary = collection.getGroupFiscalYearSummaries().get(0);
    assertThat(summary.getAllocated(), is(0d));
    assertThat(summary.getAvailable(), is(0d));
    assertThat(summary.getUnavailable(), is(0d));
  }

  private GroupFundFiscalYear buildGroupFundFiscalYear() {
    return new GroupFundFiscalYear()
      .withId(UUID.randomUUID().toString())
      .withGroupId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString());
  }

  private Budget buildBudget(String fundId, String fiscalYearId, double allocated, double available, double unavailable) {
    return new Budget()
      .withFundId(fundId)
      .withFiscalYearId(fiscalYearId)
      .withAllocated(allocated)
      .withAvailable(available)
      .withUnavailable(unavailable);
  }

  private void validateData(GroupFundFiscalYear firstGroupFundFiscalYear, Map<String, List<GroupFiscalYearSummary>> actualSummariesMap, double allocated, double available, double unavailable) {
    assertThat(actualSummariesMap.get(firstGroupFundFiscalYear.getGroupId()).get(0).getAllocated(), is(allocated));
    assertThat(actualSummariesMap.get(firstGroupFundFiscalYear.getGroupId()).get(0).getAvailable(), is(available));
    assertThat(actualSummariesMap.get(firstGroupFundFiscalYear.getGroupId()).get(0).getUnavailable(), is(unavailable));
  }
}
