package org.folio.services.group;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.vertx.junit5.VertxExtension;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
  void testUpdateBudgetIdForGroupFundFiscalYears(VertxTestContext vertxTestContext) {
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

    when(restClient.get(anyString(), any(), any()))
      .thenReturn(succeededFuture(groupFundFiscalYearCollection));
    when(restClient.put(anyString(), any(), any())).thenReturn(succeededFuture(null));
    when(requestContext.getContext()).thenReturn(Vertx.vertx().getOrCreateContext());

    Future<Void> future = groupFundFiscalYearMockService.updateBudgetIdForGroupFundFiscalYears(budget, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        String expected = String.format("fundId==%s AND fiscalYearId==%s", budget.getFundId(), budget.getFiscalYearId());

        verify(restClient).get(assertQueryContains(expected), eq(GroupFundFiscalYearCollection.class), eq(requestContext));

        ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<GroupFundFiscalYear> groupFundFiscalYearArgumentCaptor = ArgumentCaptor.forClass(GroupFundFiscalYear.class);
        verify(restClient, times(2))
          .put(idArgumentCaptor.capture(), groupFundFiscalYearArgumentCaptor.capture(), eq(requestContext));

        List<String> ids = idArgumentCaptor.getAllValues();

        assertThat(ids, containsInAnyOrder(
          resourceByIdPath(GROUP_FUND_FISCAL_YEARS, groupFundFiscalYear1.getId()),
          resourceByIdPath(GROUP_FUND_FISCAL_YEARS, groupFundFiscalYear2.getId()))
        );

        List<GroupFundFiscalYear> groupFundFiscalYears = groupFundFiscalYearArgumentCaptor.getAllValues();
        assertThat(groupFundFiscalYears, everyItem(hasProperty("budgetId", is(budget.getId()))));

        vertxTestContext.completeNow();
      });
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
        verify(restClient).get(anyString(), eq(GroupFundFiscalYearCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }
}
