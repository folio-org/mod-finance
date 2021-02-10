package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestConfig.clearServiceInteractions;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.BUDGET;
import static org.folio.rest.util.TestEntities.GROUP_FUND_FISCAL_YEAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class GroupFiscalYearSummariesTest {

  private static final Logger logger = LogManager.getLogger(GroupFiscalYearSummariesTest.class);
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

    Budget firstBudget = new Budget().withFundId(firstGroupFundFiscalYear.getFundId()).withFiscalYearId(firstGroupFundFiscalYear.getFiscalYearId())
            .withAllocated(100.01)
            .withAvailable(120d)
            .withNetTransfers(20d)
            .withUnavailable(0.01)
            .withInitialAllocation(100.01)
            .withAllocationTo(0d)
            .withAllocationFrom(0d)
            .withEncumbered(0.01d)
            .withAwaitingPayment(0d)
            .withExpenditures(0d)
            .withTotalFunding(120.01)
            .withCashBalance(120.01)
            .withOverEncumbrance(0d)
            .withOverExpended(0d);

    Budget secondBudget = new Budget().withFundId(firstGroupFundFiscalYear.getFundId()).withFiscalYearId(firstGroupFundFiscalYear.getFiscalYearId())
            .withAllocated(300d)
            .withAvailable(120.97)
            .withNetTransfers(-19.03)
            .withUnavailable(160d)
            .withInitialAllocation(150d)
            .withAllocationTo(200d)
            .withAllocationFrom(50d)
            .withEncumbered(40d)
            .withAwaitingPayment(20d)
            .withExpenditures(100d)
            .withTotalFunding(280.97)
            .withCashBalance(180.97)
            .withOverEncumbrance(0d)
            .withOverExpended(0d);

    addMockEntry(BUDGET.name(), JsonObject.mapFrom(firstBudget));
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(secondBudget));

    GroupFundFiscalYear secondGroupFundFiscalYear = buildGroupFundFiscalYear(UUID.randomUUID().toString(), UUID.randomUUID().toString());

    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(secondGroupFundFiscalYear));
    Budget thirdBudget = new Budget().withFundId(secondGroupFundFiscalYear.getFundId())
      .withFiscalYearId(secondGroupFundFiscalYear.getFiscalYearId())
      .withAllocated(0d)
      .withAvailable(120.55)
      .withNetTransfers(120.55)
      .withUnavailable(0d)
      .withInitialAllocation(150d)
      .withAllocationTo(0d)
      .withAllocationFrom(150d)
      .withEncumbered(0d)
      .withAwaitingPayment(0d)
      .withExpenditures(0d)
      .withTotalFunding(120.55)
      .withCashBalance(120.55)
      .withOverEncumbrance(0d)
      .withOverExpended(2d);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(thirdBudget));

    GroupFiscalYearSummaryCollection response = RestTestUtils.verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);

    List<GroupFiscalYearSummary> actualSummaries = response.getGroupFiscalYearSummaries();
    assertThat(actualSummaries, hasSize(2));
    assertThat(response.getTotalRecords(), is(2));

    Map<String, List<GroupFiscalYearSummary>> actualSummariesMap = actualSummaries.stream().collect(Collectors.groupingBy(GroupFiscalYearSummary::getGroupId));

    GroupFiscalYearSummary groupFiscalYearSummary1 = actualSummariesMap.get(firstGroupFundFiscalYear.getGroupId()).get(0);

    assertEquals(400.01, groupFiscalYearSummary1.getAllocated());
    assertEquals(240.97, groupFiscalYearSummary1.getAvailable());
    assertEquals(160.01, groupFiscalYearSummary1.getUnavailable());
    assertEquals(0.97, groupFiscalYearSummary1.getNetTransfers());
    assertEquals(250.01, groupFiscalYearSummary1.getInitialAllocation());
    assertEquals(200d, groupFiscalYearSummary1.getAllocationTo());
    assertEquals(50d, groupFiscalYearSummary1.getAllocationFrom());
    assertEquals(40.01, groupFiscalYearSummary1.getEncumbered());
    assertEquals(20d, groupFiscalYearSummary1.getAwaitingPayment());
    assertEquals(100d, groupFiscalYearSummary1.getExpenditures());
    assertEquals(400.98, groupFiscalYearSummary1.getTotalFunding());
    assertEquals(300.98, groupFiscalYearSummary1.getCashBalance());

    GroupFiscalYearSummary groupFiscalYearSummary2 = actualSummariesMap.get(secondGroupFundFiscalYear.getGroupId()).get(0);

    assertEquals(0d, groupFiscalYearSummary2.getAllocated());
    assertEquals(120.55, groupFiscalYearSummary2.getAvailable());
    assertEquals(0d, groupFiscalYearSummary2.getUnavailable());
    assertEquals(120.55, groupFiscalYearSummary2.getNetTransfers());
    assertEquals(150, groupFiscalYearSummary2.getInitialAllocation());
    assertEquals(0d, groupFiscalYearSummary2.getAllocationTo());
    assertEquals(150d, groupFiscalYearSummary2.getAllocationFrom());
    assertEquals(0d, groupFiscalYearSummary2.getEncumbered());
    assertEquals(0d, groupFiscalYearSummary2.getAwaitingPayment());
    assertEquals(0d, groupFiscalYearSummary2.getExpenditures());
    assertEquals(120.55, groupFiscalYearSummary2.getTotalFunding());
    assertEquals(120.55, groupFiscalYearSummary2.getCashBalance());
    assertEquals(0d, groupFiscalYearSummary2.getOverEncumbrance());
    assertEquals(2d, groupFiscalYearSummary2.getOverExpended());

  }

  @Test
  void testSummariesCollectionSameGroupAndFiscalYear() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection - the same Group and Fiscal Year Ids ===");

    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();

    GroupFundFiscalYear firstGroupFundFiscalYear = buildGroupFundFiscalYear(groupId, fiscalYearId);
    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(firstGroupFundFiscalYear));

    Budget firstBudget = new Budget().withFundId(firstGroupFundFiscalYear.getFundId()).withFiscalYearId(firstGroupFundFiscalYear.getFiscalYearId())
            .withAllocated(100d)
            .withAvailable(50d)
            .withNetTransfers(0d)
            .withUnavailable(0d)
            .withInitialAllocation(100d)
            .withAllocationTo(0d)
            .withAllocationFrom(0d)
            .withEncumbered(0d)
            .withAwaitingPayment(0d)
            .withExpenditures(50d)
            .withTotalFunding(100d)
            .withCashBalance(50d)
            .withOverEncumbrance(0d)
            .withOverExpended(0d);

    addMockEntry(BUDGET.name(), JsonObject.mapFrom(firstBudget));

    GroupFundFiscalYear secondGroupFundFiscalYear = buildGroupFundFiscalYear(groupId, fiscalYearId);

    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(secondGroupFundFiscalYear));
    Budget secondBudget = new Budget().withFundId(secondGroupFundFiscalYear.getFundId()).withFiscalYearId(secondGroupFundFiscalYear.getFiscalYearId())
            .withAllocated(400d)
            .withAvailable(450d)
            .withNetTransfers(550d)
            .withUnavailable(500d)
            .withInitialAllocation(100d)
            .withAllocationTo(300d)
            .withAllocationFrom(0d)
            .withEncumbered(200d)
            .withAwaitingPayment(250d)
            .withExpenditures(50d)
            .withTotalFunding(950d)
            .withCashBalance(900d)
            .withOverEncumbrance(0d)
            .withOverExpended(0d);

    addMockEntry(BUDGET.name(), JsonObject.mapFrom(secondBudget));

    GroupFiscalYearSummaryCollection response = RestTestUtils.verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);

    List<GroupFiscalYearSummary> actualSummaries = response.getGroupFiscalYearSummaries();
    assertThat(actualSummaries, hasSize(1));
    assertThat(response.getTotalRecords(), is(1));

    Map<String, List<GroupFiscalYearSummary>> actualSummariesMap = actualSummaries.stream().collect(Collectors.groupingBy(GroupFiscalYearSummary::getGroupId));

    GroupFiscalYearSummary groupFiscalYearSummary = actualSummariesMap.get(firstGroupFundFiscalYear.getGroupId()).get(0);

    assertEquals(500d, groupFiscalYearSummary.getAllocated());
    assertEquals(500d, groupFiscalYearSummary.getAvailable());
    assertEquals(500d, groupFiscalYearSummary.getUnavailable());
    assertEquals(550d, groupFiscalYearSummary.getNetTransfers());
    assertEquals(200d, groupFiscalYearSummary.getInitialAllocation());
    assertEquals(300d, groupFiscalYearSummary.getAllocationTo());
    assertEquals(0d, groupFiscalYearSummary.getAllocationFrom());
    assertEquals(200d, groupFiscalYearSummary.getEncumbered());
    assertEquals(250d, groupFiscalYearSummary.getAwaitingPayment());
    assertEquals(100d, groupFiscalYearSummary.getExpenditures());
    assertEquals(1050d, groupFiscalYearSummary.getTotalFunding());
    assertEquals(950d, groupFiscalYearSummary.getCashBalance());

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

}
