package org.folio.services.group;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearBatchRequest;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class GroupFundFiscalYearServiceTest {

  @InjectMocks
  private GroupFundFiscalYearService groupFundFiscalYearMockService;

  @Mock
  private RestClient restClient;

  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetGroupFundFiscalYearsWithBudgetIds(VertxTestContext vertxTestContext) {
    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    when(restClient.get(anyString(), any(), any()))
      .thenReturn(succeededFuture(new GroupFundFiscalYearCollection()));

    Future<List<GroupFundFiscalYear>> future = groupFundFiscalYearMockService.getGroupFundFiscalYearsWithBudgetId(groupId, fiscalYearId, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        String expectedQuery = String.format("groupId==%s AND fiscalYearId==%s AND budgetId=*", groupId, fiscalYearId);
        verify(restClient).get(assertQueryContains(expectedQuery), eq(GroupFundFiscalYearCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetGroupFundFiscalYearsByFundIds_Success(VertxTestContext vertxTestContext) {
    // Given
    String fundId1 = UUID.randomUUID().toString();
    String fundId2 = UUID.randomUUID().toString();
    String fundId3 = UUID.randomUUID().toString();
    List<String> fundIds = List.of(fundId1, fundId2, fundId3);

    GroupFundFiscalYearBatchRequest batchRequest = new GroupFundFiscalYearBatchRequest()
      .withFundIds(fundIds);

    GroupFundFiscalYear gffy1 = new GroupFundFiscalYear().withId(UUID.randomUUID().toString()).withFundId(fundId1);
    GroupFundFiscalYear gffy2 = new GroupFundFiscalYear().withId(UUID.randomUUID().toString()).withFundId(fundId2);
    GroupFundFiscalYear gffy3 = new GroupFundFiscalYear().withId(UUID.randomUUID().toString()).withFundId(fundId3);

    GroupFundFiscalYearCollection expectedCollection = new GroupFundFiscalYearCollection()
      .withGroupFundFiscalYears(List.of(gffy1, gffy2, gffy3))
      .withTotalRecords(3);

    when(restClient.postBatch(anyString(), any(GroupFundFiscalYearBatchRequest.class), eq(GroupFundFiscalYearCollection.class), eq(requestContext)))
      .thenReturn(succeededFuture(expectedCollection));

    // When
    Future<GroupFundFiscalYearCollection> future = groupFundFiscalYearMockService.getGroupFundFiscalYearsByFundIds(batchRequest, requestContext);

    // Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        ArgumentCaptor<String> endpointCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<GroupFundFiscalYearBatchRequest> requestCaptor = ArgumentCaptor.forClass(GroupFundFiscalYearBatchRequest.class);

        verify(restClient).postBatch(endpointCaptor.capture(), requestCaptor.capture(), eq(GroupFundFiscalYearCollection.class), eq(requestContext));

        // Verify endpoint
        String capturedEndpoint = endpointCaptor.getValue();
        assertThat(capturedEndpoint, equalTo("/finance-storage/group-fund-fiscal-years/batch"));

        // Verify request
        GroupFundFiscalYearBatchRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getFundIds(), hasSize(3));
        assertThat(capturedRequest.getFundIds(), equalTo(fundIds));

        // Verify response
        GroupFundFiscalYearCollection response = result.result();
        assertThat(response.getGroupFundFiscalYears(), hasSize(3));
        assertThat(response.getTotalRecords(), equalTo(3));

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetGroupFundFiscalYearsByFundIds_WithFilters(VertxTestContext vertxTestContext) {
    // Given
    String fundId1 = UUID.randomUUID().toString();
    String fundId2 = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    String groupId = UUID.randomUUID().toString();
    List<String> fundIds = List.of(fundId1, fundId2);

    GroupFundFiscalYearBatchRequest batchRequest = new GroupFundFiscalYearBatchRequest()
      .withFundIds(fundIds)
      .withFiscalYearId(fiscalYearId)
      .withGroupId(groupId);

    GroupFundFiscalYear gffy1 = new GroupFundFiscalYear()
      .withId(UUID.randomUUID().toString())
      .withFundId(fundId1)
      .withFiscalYearId(fiscalYearId)
      .withGroupId(groupId);

    GroupFundFiscalYearCollection expectedCollection = new GroupFundFiscalYearCollection()
      .withGroupFundFiscalYears(List.of(gffy1))
      .withTotalRecords(1);

    when(restClient.postBatch(anyString(), any(GroupFundFiscalYearBatchRequest.class), eq(GroupFundFiscalYearCollection.class), eq(requestContext)))
      .thenReturn(succeededFuture(expectedCollection));

    // When
    Future<GroupFundFiscalYearCollection> future = groupFundFiscalYearMockService.getGroupFundFiscalYearsByFundIds(batchRequest, requestContext);

    // Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        ArgumentCaptor<GroupFundFiscalYearBatchRequest> requestCaptor = ArgumentCaptor.forClass(GroupFundFiscalYearBatchRequest.class);

        verify(restClient).postBatch(anyString(), requestCaptor.capture(), eq(GroupFundFiscalYearCollection.class), eq(requestContext));

        // Verify request includes filters
        GroupFundFiscalYearBatchRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getFundIds(), hasSize(2));
        assertEquals(fiscalYearId, capturedRequest.getFiscalYearId());
        assertEquals(groupId, capturedRequest.getGroupId());

        // Verify response
        GroupFundFiscalYearCollection response = result.result();
        assertThat(response.getGroupFundFiscalYears(), hasSize(1));
        assertEquals(fiscalYearId, response.getGroupFundFiscalYears().get(0).getFiscalYearId());
        assertEquals(groupId, response.getGroupFundFiscalYears().get(0).getGroupId());

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetGroupFundFiscalYearsByFundIds_EmptyFundIds(VertxTestContext vertxTestContext) {
    // Given
    GroupFundFiscalYearBatchRequest batchRequest = new GroupFundFiscalYearBatchRequest()
      .withFundIds(List.of());

    // When
    Future<GroupFundFiscalYearCollection> future = groupFundFiscalYearMockService.getGroupFundFiscalYearsByFundIds(batchRequest, requestContext);

    // Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        GroupFundFiscalYearCollection response = result.result();
        assertThat(response.getGroupFundFiscalYears(), hasSize(0));
        assertThat(response.getTotalRecords(), equalTo(0));

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetGroupFundFiscalYearsByFundIds_NullFundIds(VertxTestContext vertxTestContext) {
    // Given
    GroupFundFiscalYearBatchRequest batchRequest = new GroupFundFiscalYearBatchRequest()
      .withFundIds(null);

    // When
    Future<GroupFundFiscalYearCollection> future = groupFundFiscalYearMockService.getGroupFundFiscalYearsByFundIds(batchRequest, requestContext);

    // Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        GroupFundFiscalYearCollection response = result.result();
        assertThat(response.getGroupFundFiscalYears(), hasSize(0));
        assertThat(response.getTotalRecords(), equalTo(0));

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetGroupFundFiscalYearsByFundIds_LargeNumberOfFundIds(VertxTestContext vertxTestContext) {
    // Given - simulate 1000 fund IDs
    List<String> fundIds = java.util.stream.IntStream.range(0, 1000)
      .mapToObj(i -> UUID.randomUUID().toString())
      .toList();

    GroupFundFiscalYearBatchRequest batchRequest = new GroupFundFiscalYearBatchRequest()
      .withFundIds(fundIds);

    GroupFundFiscalYearCollection expectedCollection = new GroupFundFiscalYearCollection()
      .withGroupFundFiscalYears(List.of())
      .withTotalRecords(0);

    when(restClient.postBatch(anyString(), any(GroupFundFiscalYearBatchRequest.class), eq(GroupFundFiscalYearCollection.class), eq(requestContext)))
      .thenReturn(succeededFuture(expectedCollection));

    // When
    Future<GroupFundFiscalYearCollection> future = groupFundFiscalYearMockService.getGroupFundFiscalYearsByFundIds(batchRequest, requestContext);

    // Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        ArgumentCaptor<GroupFundFiscalYearBatchRequest> requestCaptor = ArgumentCaptor.forClass(GroupFundFiscalYearBatchRequest.class);

        verify(restClient).postBatch(anyString(), requestCaptor.capture(), eq(GroupFundFiscalYearCollection.class), eq(requestContext));

        // Verify all 1000 fundIds are sent in single request
        GroupFundFiscalYearBatchRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getFundIds(), hasSize(1000));

        vertxTestContext.completeNow();
      });
  }
}
