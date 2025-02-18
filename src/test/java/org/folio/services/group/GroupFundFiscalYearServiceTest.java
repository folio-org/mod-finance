package org.folio.services.group;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
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
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
