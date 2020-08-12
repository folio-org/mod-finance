package org.folio.services;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GroupFundFiscalYearServiceTest {

  @InjectMocks
  private GroupFundFiscalYearService groupFundFiscalYearMockService;

  @Mock
  private RestClient groupFundFiscalYearRestClientMock;

  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void testUpdateBudgetIdForGroupFundFiscalYears() {
    Budget budget = new Budget()
      .withId(UUID.randomUUID().toString())
      .withFundId(UUID.randomUUID().toString())
      .withFiscalYearId(UUID.randomUUID().toString());
    GroupFundFiscalYearCollection groupFundFiscalYearCollection = new GroupFundFiscalYearCollection();
    GroupFundFiscalYear groupFundFiscalYear1= new GroupFundFiscalYear()
      .withId(UUID.randomUUID().toString())
      .withFiscalYearId(budget.getFiscalYearId())
      .withFundId(budget.getFundId())
      .withGroupId(UUID.randomUUID().toString());
    GroupFundFiscalYear groupFundFiscalYear2= new GroupFundFiscalYear()
      .withId(UUID.randomUUID().toString())
      .withFiscalYearId(budget.getFiscalYearId())
      .withFundId(budget.getFundId())
      .withGroupId(UUID.randomUUID().toString());

    groupFundFiscalYearCollection.withGroupFundFiscalYears(Arrays.asList(groupFundFiscalYear1, groupFundFiscalYear2))
      .withTotalRecords(2);

    when(groupFundFiscalYearRestClientMock.get(anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(groupFundFiscalYearCollection));
    when(groupFundFiscalYearRestClientMock.put(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    when(requestContext.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    CompletableFuture<Void> resultFuture = groupFundFiscalYearMockService.updateBudgetIdForGroupFundFiscalYears(budget, requestContext);

    resultFuture.join();

    String expected = String.format("fundId==%s AND fiscalYearId==%s", budget.getFundId(), budget.getFiscalYearId());

    verify(groupFundFiscalYearRestClientMock).get(eq(expected), eq(0), eq(Integer.MAX_VALUE), eq(requestContext), eq(GroupFundFiscalYearCollection.class));

    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<GroupFundFiscalYear> groupFundFiscalYearArgumentCaptor = ArgumentCaptor.forClass(GroupFundFiscalYear.class);
    verify(groupFundFiscalYearRestClientMock, times(2))
      .put(idArgumentCaptor.capture(), groupFundFiscalYearArgumentCaptor.capture(), eq(requestContext));

    List<String> ids = idArgumentCaptor.getAllValues();

    assertThat(ids, containsInAnyOrder(groupFundFiscalYear1.getId(), groupFundFiscalYear2.getId()));

    List<GroupFundFiscalYear> groupFundFiscalYears = groupFundFiscalYearArgumentCaptor.getAllValues();
    assertThat(groupFundFiscalYears, everyItem(hasProperty("budgetId", is(budget.getId()))));

  }

  @Test
  void testGetGroupFundFiscalYearsWithBudgetIds() {
    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    when(groupFundFiscalYearRestClientMock.get(anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(new GroupFundFiscalYearCollection()));

    CompletableFuture<List<GroupFundFiscalYear>> resultFuture = groupFundFiscalYearMockService.getGroupFundFiscalYearsWithBudgetId(groupId, fiscalYearId, requestContext);
    resultFuture.join();
    String expectedQuery = String.format("groupId==%s AND fiscalYearId==%s AND budgetId=*", groupId, fiscalYearId);
    verify(groupFundFiscalYearRestClientMock).get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContext), eq(GroupFundFiscalYearCollection.class));
  }
}
