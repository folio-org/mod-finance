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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.ApiTestSuite;
import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummaryCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.resource.FinanceGroupFiscalYearSummaries;
import org.folio.rest.util.HelperUtils;
import org.folio.rest.util.RestTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;


public class GroupFiscalYearSummariesTest {

  private static final Logger logger = LogManager.getLogger(GroupFiscalYearSummariesTest.class);
  public static final String TO_ALLOCATION_FIRST_DIF_GROUP = "toAllocationFirstDifGroup";
  public static final String TO_ALLOCATION_SECOND_DIF_GROUP = "toAllocationSecondDifGroup";
  public static final String FUND_ID_FIRST_SAME_GROUP = UUID.randomUUID().toString();
  public static final String FUND_ID_SECOND_SAME_GROUP = UUID.randomUUID().toString();

  public static final String FUND_ID_FIRST_DIFFERENT_GROUP = UUID.randomUUID().toString();
  public static final String FUND_ID_SECOND_DIFFERENT_GROUP = UUID.randomUUID().toString();

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
    firstGroupFundFiscalYear.withFundId(FUND_ID_FIRST_DIFFERENT_GROUP);
    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(firstGroupFundFiscalYear));

    Budget firstBudget = new Budget().withFundId(firstGroupFundFiscalYear.getFundId()).withFiscalYearId(firstGroupFundFiscalYear.getFiscalYearId())
            .withAllocated(100.01)
            .withAvailable(120d)
            .withNetTransfers(0d)
            .withUnavailable(0.01)
            .withInitialAllocation(100.01)
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
            .withNetTransfers(0d)
            .withUnavailable(160d)
            .withInitialAllocation(150d)
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
    secondGroupFundFiscalYear.withFundId(FUND_ID_SECOND_DIFFERENT_GROUP);

    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(secondGroupFundFiscalYear));
    Budget thirdBudget = new Budget().withFundId(secondGroupFundFiscalYear.getFundId())
      .withFiscalYearId(secondGroupFundFiscalYear.getFiscalYearId())
      .withAllocated(0d)
      .withAvailable(120.55)
      .withNetTransfers(0d)
      .withUnavailable(0d)
      .withInitialAllocation(150d)
      .withEncumbered(0d)
      .withAwaitingPayment(0d)
      .withExpenditures(0d)
      .withTotalFunding(120.55)
      .withCashBalance(120.55)
      .withOverEncumbrance(0d)
      .withOverExpended(2d);
    addMockEntry(BUDGET.name(), JsonObject.mapFrom(thirdBudget));

    Transaction transactionFirst = new Transaction().withId(UUID.randomUUID().toString()).withTransactionType(Transaction.TransactionType.ALLOCATION)
      .withAmount(30d).withCurrency("USD").withToFundId(secondGroupFundFiscalYear.getFundId())
      .withFromFundId(firstGroupFundFiscalYear.getFundId())
      .withSourceFiscalYearId(secondGroupFundFiscalYear.getFiscalYearId())
      .withMetadata(new Metadata().withCreatedDate(new Date()));
    addMockEntry(TO_ALLOCATION_FIRST_DIF_GROUP, JsonObject.mapFrom(transactionFirst));

    Transaction transactionSecond = new Transaction().withId(UUID.randomUUID().toString()).withTransactionType(Transaction.TransactionType.ALLOCATION)
      .withAmount(120d).withCurrency("USD").withToFundId(secondGroupFundFiscalYear.getFundId())
      .withFromFundId(firstGroupFundFiscalYear.getFundId())
      .withSourceFiscalYearId(secondGroupFundFiscalYear.getFiscalYearId())
      .withMetadata(new Metadata().withCreatedDate(new Date()));
    addMockEntry(TO_ALLOCATION_SECOND_DIF_GROUP, JsonObject.mapFrom(transactionSecond));

    GroupFiscalYearSummaryCollection response = RestTestUtils.verifyGet(HelperUtils.getEndpoint(FinanceGroupFiscalYearSummaries.class), APPLICATION_JSON, OK.getStatusCode()).as(GroupFiscalYearSummaryCollection.class);

    List<GroupFiscalYearSummary> actualSummaries = response.getGroupFiscalYearSummaries();
    assertThat(actualSummaries, hasSize(2));
    assertThat(response.getTotalRecords(), is(2));

    Map<String, List<GroupFiscalYearSummary>> actualSummariesMap = actualSummaries.stream().collect(Collectors.groupingBy(GroupFiscalYearSummary::getGroupId));

    GroupFiscalYearSummary groupFiscalYearSummary1 = actualSummariesMap.get(firstGroupFundFiscalYear.getGroupId()).get(0);

    assertEquals(100.01, groupFiscalYearSummary1.getAllocated());
    assertEquals(0d, groupFiscalYearSummary1.getNetTransfers());
    assertEquals(100.01, groupFiscalYearSummary1.getTotalFunding());
    assertEquals(160.01, groupFiscalYearSummary1.getUnavailable());
    assertEquals(-60.0, groupFiscalYearSummary1.getAvailable());
    assertEquals(250.01, groupFiscalYearSummary1.getInitialAllocation());
    assertEquals(0d, groupFiscalYearSummary1.getAllocationTo());
    assertEquals(150d, groupFiscalYearSummary1.getAllocationFrom());
    assertEquals(40.01, groupFiscalYearSummary1.getEncumbered());
    assertEquals(20d, groupFiscalYearSummary1.getAwaitingPayment());
    assertEquals(100d, groupFiscalYearSummary1.getExpenditures());
    assertEquals(0.01, groupFiscalYearSummary1.getCashBalance());

    GroupFiscalYearSummary groupFiscalYearSummary2 = actualSummariesMap.get(secondGroupFundFiscalYear.getGroupId()).get(0);

    assertEquals(270d, groupFiscalYearSummary2.getAllocated());
    assertEquals(0.0, groupFiscalYearSummary2.getNetTransfers());
    assertEquals(270.0, groupFiscalYearSummary2.getTotalFunding());
    assertEquals(0d, groupFiscalYearSummary2.getUnavailable());
    assertEquals(270.0, groupFiscalYearSummary2.getAvailable());
    assertEquals(150, groupFiscalYearSummary2.getInitialAllocation());
    assertEquals(120d, groupFiscalYearSummary2.getAllocationTo());
    assertEquals(0d, groupFiscalYearSummary2.getAllocationFrom());
    assertEquals(0d, groupFiscalYearSummary2.getEncumbered());
    assertEquals(0d, groupFiscalYearSummary2.getAwaitingPayment());
    assertEquals(0d, groupFiscalYearSummary2.getExpenditures());
    assertEquals(0d, groupFiscalYearSummary2.getOverExpended());
    assertEquals(270.0, groupFiscalYearSummary2.getCashBalance());
    assertEquals(0d, groupFiscalYearSummary2.getOverEncumbrance());
  }

  @Test
  void testSummariesCollectionSameGroupAndFiscalYear() {
    logger.info("=== Test Get Group Fiscal Year Summaries Collection - the same Group and Fiscal Year Ids ===");

    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();

    GroupFundFiscalYear firstGroupFundFiscalYear = buildGroupFundFiscalYear(groupId, fiscalYearId);
    firstGroupFundFiscalYear.withFundId(FUND_ID_FIRST_SAME_GROUP);
    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(firstGroupFundFiscalYear));

    Budget firstBudget = new Budget().withFundId(firstGroupFundFiscalYear.getFundId()).withFiscalYearId(firstGroupFundFiscalYear.getFiscalYearId())
            .withAllocated(100d)
            .withAvailable(50d)
            .withNetTransfers(0d)
            .withUnavailable(0d)
            .withInitialAllocation(100d)
            .withAllocationTo(0d)
            .withAllocationFrom(300d)
            .withEncumbered(0d)
            .withAwaitingPayment(0d)
            .withExpenditures(50d)
            .withTotalFunding(100d)
            .withCashBalance(50d)
            .withOverEncumbrance(0d)
            .withOverExpended(0d);

    addMockEntry(BUDGET.name(), JsonObject.mapFrom(firstBudget));

    GroupFundFiscalYear secondGroupFundFiscalYear = buildGroupFundFiscalYear(groupId, fiscalYearId);
    secondGroupFundFiscalYear.withFundId(FUND_ID_SECOND_SAME_GROUP);

    addMockEntry(GROUP_FUND_FISCAL_YEAR.name(), JsonObject.mapFrom(secondGroupFundFiscalYear));
    Budget secondBudget = new Budget().withFundId(secondGroupFundFiscalYear.getFundId()).withFiscalYearId(secondGroupFundFiscalYear.getFiscalYearId())
            .withAllocated(400d)
            .withAvailable(450d)
            .withNetTransfers(0d)
            .withUnavailable(500d)
            .withInitialAllocation(100d)
            .withAllocationTo(0d)
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
    assertEquals(200d, groupFiscalYearSummary.getAllocated());
    assertEquals(200d, groupFiscalYearSummary.getTotalFunding());
    assertEquals(-300d, groupFiscalYearSummary.getAvailable());
    assertEquals(500d, groupFiscalYearSummary.getUnavailable());
    assertEquals(0d, groupFiscalYearSummary.getNetTransfers());
    assertEquals(200d, groupFiscalYearSummary.getInitialAllocation());
    assertEquals(0d, groupFiscalYearSummary.getAllocationTo());
    assertEquals(0d, groupFiscalYearSummary.getAllocationFrom());
    assertEquals(200d, groupFiscalYearSummary.getEncumbered());
    assertEquals(250d, groupFiscalYearSummary.getAwaitingPayment());
    assertEquals(100d, groupFiscalYearSummary.getExpenditures());
    assertEquals(100d, groupFiscalYearSummary.getCashBalance());

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
