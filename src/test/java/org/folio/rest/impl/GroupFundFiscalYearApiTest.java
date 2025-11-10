package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.ApiTestSuite;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearBatchRequest;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.folio.rest.util.RestTestUtils;
import org.folio.services.group.GroupFundFiscalYearService;
import org.folio.util.CopilotGenerated;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.vertx.core.Future;

@CopilotGenerated(model = "Claude Sonnet 4.5")
public class GroupFundFiscalYearApiTest {

  private static boolean runningOnOwn;

  @Autowired
  private GroupFundFiscalYearService groupFundFiscalYearServiceMock;

  public static final String GROUP_FUND_FISCAL_YEAR_ENDPOINT = "finance/group-fund-fiscal-years";
  public static final String GROUP_FUND_FISCAL_YEAR_BATCH_ENDPOINT = "finance/group-fund-fiscal-years/batch";

  @BeforeAll
  static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(GroupFundFiscalYearApiTest.ContextConfiguration.class);
  }

  @AfterAll
  static void after() {
    clearVertxContext();
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterEach
  void resetMocks() {
    reset(groupFundFiscalYearServiceMock);
  }

  @Test
  void testPostFinanceGroupFundFiscalYears() {
    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String id = UUID.randomUUID().toString();

    GroupFundFiscalYear requestEntity = new GroupFundFiscalYear()
      .withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withFundId(fundId);

    GroupFundFiscalYear responseEntity = new GroupFundFiscalYear()
      .withId(id)
      .withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withFundId(fundId);

    when(groupFundFiscalYearServiceMock.createGroupFundFiscalYear(any(GroupFundFiscalYear.class), any(RequestContext.class)))
      .thenReturn(succeededFuture(responseEntity));

    GroupFundFiscalYear result = RestTestUtils.verifyPostResponse(GROUP_FUND_FISCAL_YEAR_ENDPOINT,
      requestEntity, APPLICATION_JSON, CREATED.getStatusCode())
      .as(GroupFundFiscalYear.class);

    assertNotNull(result.getId());
    assertEquals(groupId, result.getGroupId());
    assertEquals(fiscalYearId, result.getFiscalYearId());
    assertEquals(fundId, result.getFundId());

    verify(groupFundFiscalYearServiceMock).createGroupFundFiscalYear(any(GroupFundFiscalYear.class), any(RequestContext.class));
  }

  @Test
  void testPostFinanceGroupFundFiscalYearsFailure() {
    GroupFundFiscalYear requestEntity = new GroupFundFiscalYear()
      .withGroupId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString());

    when(groupFundFiscalYearServiceMock.createGroupFundFiscalYear(any(GroupFundFiscalYear.class), any(RequestContext.class)))
      .thenReturn(Future.failedFuture(new RuntimeException("Internal server error")));

    RestTestUtils.verifyPostResponse(GROUP_FUND_FISCAL_YEAR_ENDPOINT,
      requestEntity, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode());

    verify(groupFundFiscalYearServiceMock).createGroupFundFiscalYear(any(GroupFundFiscalYear.class), any(RequestContext.class));
  }

  @Test
  void testGetFinanceGroupFundFiscalYears() {
    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();

    GroupFundFiscalYear groupFundFiscalYear = new GroupFundFiscalYear()
      .withId(UUID.randomUUID().toString())
      .withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withFundId(fundId);

    GroupFundFiscalYearCollection collection = new GroupFundFiscalYearCollection()
      .withGroupFundFiscalYears(List.of(groupFundFiscalYear))
      .withTotalRecords(1);

    when(groupFundFiscalYearServiceMock.getGroupFundFiscalYears(anyString(), anyInt(), anyInt(), any(RequestContext.class)))
      .thenReturn(succeededFuture(collection));

    Map<String, Object> params = new HashMap<>();
    params.put("query", "groupId==" + groupId);
    params.put("limit", 10);
    params.put("offset", 0);

    GroupFundFiscalYearCollection result = RestTestUtils.verifyGetWithParam(GROUP_FUND_FISCAL_YEAR_ENDPOINT,
      APPLICATION_JSON, OK.getStatusCode(), params)
      .as(GroupFundFiscalYearCollection.class);

    assertThat(result.getGroupFundFiscalYears(), hasSize(1));
    assertEquals(1, result.getTotalRecords());
    assertEquals(groupId, result.getGroupFundFiscalYears().getFirst().getGroupId());

    verify(groupFundFiscalYearServiceMock).getGroupFundFiscalYears(anyString(), anyInt(), anyInt(), any(RequestContext.class));
  }

  @Test
  void testGetFinanceGroupFundFiscalYearsFailure() {
    when(groupFundFiscalYearServiceMock.getGroupFundFiscalYears(anyString(), anyInt(), anyInt(), any(RequestContext.class)))
      .thenReturn(Future.failedFuture(new RuntimeException("Internal server error")));

    Map<String, Object> params = new HashMap<>();
    params.put("query", "groupId==123");
    params.put("limit", 10);
    params.put("offset", 0);

    RestTestUtils.verifyGetWithParam(GROUP_FUND_FISCAL_YEAR_ENDPOINT,
      APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode(), params);

    verify(groupFundFiscalYearServiceMock).getGroupFundFiscalYears(anyString(), anyInt(), anyInt(), any(RequestContext.class));
  }

  @Test
  void testPostFinanceGroupFundFiscalYearsBatch() {
    String fundId1 = UUID.randomUUID().toString();
    String fundId2 = UUID.randomUUID().toString();

    GroupFundFiscalYearBatchRequest batchRequest = new GroupFundFiscalYearBatchRequest()
      .withFundIds(List.of(fundId1, fundId2));

    GroupFundFiscalYear gffy1 = new GroupFundFiscalYear()
      .withId(UUID.randomUUID().toString())
      .withGroupId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString())
      .withFundId(fundId1);

    GroupFundFiscalYear gffy2 = new GroupFundFiscalYear()
      .withId(UUID.randomUUID().toString())
      .withGroupId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString())
      .withFundId(fundId2);

    GroupFundFiscalYearCollection collection = new GroupFundFiscalYearCollection()
      .withGroupFundFiscalYears(List.of(gffy1, gffy2))
      .withTotalRecords(2);

    when(groupFundFiscalYearServiceMock.getGroupFundFiscalYearsByFundIds(any(GroupFundFiscalYearBatchRequest.class), any(RequestContext.class)))
      .thenReturn(succeededFuture(collection));

    GroupFundFiscalYearCollection result = RestTestUtils.verifyPostResponse(GROUP_FUND_FISCAL_YEAR_BATCH_ENDPOINT,
      batchRequest, APPLICATION_JSON, OK.getStatusCode())
      .as(GroupFundFiscalYearCollection.class);

    assertNotNull(result);
    assertThat(result.getGroupFundFiscalYears(), hasSize(2));
    assertEquals(2, result.getTotalRecords());
    assertEquals(fundId1, result.getGroupFundFiscalYears().get(0).getFundId());
    assertEquals(fundId2, result.getGroupFundFiscalYears().get(1).getFundId());

    verify(groupFundFiscalYearServiceMock).getGroupFundFiscalYearsByFundIds(any(GroupFundFiscalYearBatchRequest.class), any(RequestContext.class));
  }

  @Test
  void testPostFinanceGroupFundFiscalYearsBatchWithEmptyFundIds() {
    GroupFundFiscalYearBatchRequest batchRequest = new GroupFundFiscalYearBatchRequest()
      .withFundIds(List.of());

    // Empty fundIds array violates the minItems: 1 constraint in the schema, so we expect 422
    RestTestUtils.verifyPostResponse(GROUP_FUND_FISCAL_YEAR_BATCH_ENDPOINT,
      batchRequest, APPLICATION_JSON, 422);
  }

  @Test
  void testPostFinanceGroupFundFiscalYearsBatchFailure() {
    GroupFundFiscalYearBatchRequest batchRequest = new GroupFundFiscalYearBatchRequest()
      .withFundIds(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

    when(groupFundFiscalYearServiceMock.getGroupFundFiscalYearsByFundIds(any(GroupFundFiscalYearBatchRequest.class), any(RequestContext.class)))
      .thenReturn(Future.failedFuture(new RuntimeException("Internal server error")));

    RestTestUtils.verifyPostResponse(GROUP_FUND_FISCAL_YEAR_BATCH_ENDPOINT,
      batchRequest, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode());

    verify(groupFundFiscalYearServiceMock).getGroupFundFiscalYearsByFundIds(any(GroupFundFiscalYearBatchRequest.class), any(RequestContext.class));
  }

  @Test
  void testDeleteFinanceGroupFundFiscalYearsById() {
    String id = UUID.randomUUID().toString();

    when(groupFundFiscalYearServiceMock.deleteGroupFundFiscalYear(anyString(), any(RequestContext.class)))
      .thenReturn(succeededFuture(null));

    RestTestUtils.verifyDeleteResponse(GROUP_FUND_FISCAL_YEAR_ENDPOINT + "/" + id,
      "", NO_CONTENT.getStatusCode());

    verify(groupFundFiscalYearServiceMock).deleteGroupFundFiscalYear(eq(id), any(RequestContext.class));
  }

  @Test
  void testDeleteFinanceGroupFundFiscalYearsByIdFailure() {
    String id = UUID.randomUUID().toString();

    when(groupFundFiscalYearServiceMock.deleteGroupFundFiscalYear(anyString(), any(RequestContext.class)))
      .thenReturn(Future.failedFuture(new RuntimeException("Internal server error")));

    RestTestUtils.verifyDeleteResponse(GROUP_FUND_FISCAL_YEAR_ENDPOINT + "/" + id,
      APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode());

    verify(groupFundFiscalYearServiceMock).deleteGroupFundFiscalYear(eq(id), any(RequestContext.class));
  }

  /**
   * Define unit test specific beans to override actual ones
   */
  static class ContextConfiguration {

    @Bean
    public GroupFundFiscalYearService groupFundFiscalYearService() {
      return mock(GroupFundFiscalYearService.class);
    }
  }
}
