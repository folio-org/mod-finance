package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.TestConfig.clearServiceInteractions;
import static org.folio.rest.util.TestConfig.deployVerticle;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.folio.rest.util.TestEntities.GROUP_FUND_FISCAL_YEAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummaryCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.resource.FinanceGroupFiscalYearSummaries;
import org.folio.rest.util.HelperUtils;
import org.folio.rest.util.RestTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class GroupFiscalYearSummariesTest {

  private static final Logger logger = LoggerFactory.getLogger(GroupFiscalYearSummariesTest.class);
  private static boolean runningOnOwn;

  @BeforeAll
  static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(ApplicationConfig.class);
  }

  @AfterEach
  void afterEach() {
    clearServiceInteractions();
  }

  @AfterAll
  static void after() {
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
  }

  @Test
  void testSummariesCollectionDifferentGroupAndFiscalYear() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection - different Group and Fiscal Year Ids ===");

    GroupFundFiscalYear firstGroupFundFiscalYear = buildGroupFundFiscalYear(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(firstGroupFundFiscalYear));

    Budget firstBudget = buildBudget(firstGroupFundFiscalYear.getFundId(), firstGroupFundFiscalYear.getFiscalYearId(), 100d, 50d, 0d);
    Budget secondBudget = buildBudget(firstGroupFundFiscalYear.getFundId(), firstGroupFundFiscalYear.getFiscalYearId(), null, 50.1111111111d, 100d);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(firstBudget));
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(secondBudget));

    GroupFundFiscalYear secondGroupFundFiscalYear = buildGroupFundFiscalYear(UUID.randomUUID().toString(), UUID.randomUUID().toString());

    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(secondGroupFundFiscalYear));
    Budget thirdBudget = buildBudget(secondGroupFundFiscalYear.getFundId(), secondGroupFundFiscalYear.getFiscalYearId(), 999d, 0d, 999d);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(thirdBudget));

    GroupFiscalYearSummaryCollection response = RestTestUtils.verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);

    List<GroupFiscalYearSummary> actualSummaries = response.getGroupFiscalYearSummaries();
    assertThat(actualSummaries, hasSize(2));
    assertThat(response.getTotalRecords(), is(2));

    Map<String, List<GroupFiscalYearSummary>> actualSummariesMap = actualSummaries.stream().collect(Collectors.groupingBy(GroupFiscalYearSummary::getGroupId));

    validateData(firstGroupFundFiscalYear, actualSummariesMap, 100d, 100.1111111111d, 100d);
    validateData(secondGroupFundFiscalYear, actualSummariesMap, 999d, 0d, 999d);

  }

  @Test
  void testSummariesCollectionSameGroupAndFiscalYear() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection - the same Group and Fiscal Year Ids ===");

    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();

    GroupFundFiscalYear firstGroupFundFiscalYear = buildGroupFundFiscalYear(groupId, fiscalYearId);
    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(firstGroupFundFiscalYear));

    Budget firstBudget = buildBudget(firstGroupFundFiscalYear.getFundId(), firstGroupFundFiscalYear.getFiscalYearId(), 100d, 50d, null);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(firstBudget));

    GroupFundFiscalYear secondGroupFundFiscalYear = buildGroupFundFiscalYear(groupId, fiscalYearId);

    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(secondGroupFundFiscalYear));
    Budget secondBudget = buildBudget(secondGroupFundFiscalYear.getFundId(), secondGroupFundFiscalYear.getFiscalYearId(), 400d, 450d, 500d);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(secondBudget));

    GroupFiscalYearSummaryCollection response = RestTestUtils.verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);

    List<GroupFiscalYearSummary> actualSummaries = response.getGroupFiscalYearSummaries();
    assertThat(actualSummaries, hasSize(1));
    assertThat(response.getTotalRecords(), is(1));

    Map<String, List<GroupFiscalYearSummary>> actualSummariesMap = actualSummaries.stream().collect(Collectors.groupingBy(GroupFiscalYearSummary::getGroupId));

    validateData(firstGroupFundFiscalYear, actualSummariesMap, 500d, 500d, 500d);

  }

  @Test
  void testGetCollectionGffyNotFound() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection - Group Fund Fiscal Years Not Found ===");

    GroupFiscalYearSummaryCollection collection = RestTestUtils.verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class) + RestTestUtils.buildQueryParam("id==(" + UUID.randomUUID().toString() + ")"), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);
    assertThat(collection.getTotalRecords(), is(0));
    assertThat(collection.getTotalRecords(), is(0));
  }

  @Test
  void testGetCollectionBudgetNotFound() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection - Budgets Not Found ===");
    GroupFundFiscalYear groupFundFiscalYear = buildGroupFundFiscalYear(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(groupFundFiscalYear));
    GroupFiscalYearSummaryCollection collection = RestTestUtils.verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class) + RestTestUtils.buildQueryParam("id==(" + groupFundFiscalYear.getId() +")"), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);

    assertThat(collection.getTotalRecords(), is(1));
    assertThat(collection.getGroupFiscalYearSummaries(), hasSize(1));

    GroupFiscalYearSummary summary = collection.getGroupFiscalYearSummaries().get(0);
    assertThat(summary.getAllocated(), is(0d));
    assertThat(summary.getAvailable(), is(0d));
    assertThat(summary.getUnavailable(), is(0d));
  }

  private GroupFundFiscalYear buildGroupFundFiscalYear(String groupId, String fiscalYearId) {
    return new GroupFundFiscalYear()
      .withId(UUID.randomUUID().toString())
      .withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withFundId(UUID.randomUUID().toString());
  }

  private Budget buildBudget(String fundId, String fiscalYearId, Double allocated, Double available, Double unavailable) {
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
