package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.folio.rest.util.ErrorCodes.MISSING_FISCAL_YEAR_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.util.TestEntities;
import org.folio.services.GroupExpenseClassTotalsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GroupsApiTest extends ApiTestBase {

  public static GroupExpenseClassTotalsService groupExpenseClassTotalsServiceMock = mock(GroupExpenseClassTotalsService.class);

  @AfterEach
  void clearMocks() {
    Mockito.reset(groupExpenseClassTotalsServiceMock);
  }

  @Test
  void testGetFinanceGroupsExpenseClassesTotalsById() {
    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    GroupExpenseClassTotalsCollection groupExpenseClassTotalsCollection = new GroupExpenseClassTotalsCollection();

    when(groupExpenseClassTotalsServiceMock.getExpenseClassTotals(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(groupExpenseClassTotalsCollection));

    GroupExpenseClassTotalsCollection result = verifyGet(String.format("/finance/groups/%s/expense-classes-totals?fiscalYearId=%s", groupId, fiscalYearId), APPLICATION_JSON, 200)
      .as(GroupExpenseClassTotalsCollection.class);


    assertEquals(groupExpenseClassTotalsCollection, result);
    verify(groupExpenseClassTotalsServiceMock).getExpenseClassTotals(eq(groupId), eq(fiscalYearId) , any(RequestContext.class));
  }

  @Test
  void testGetFinanceGroupsExpenseClassesTotalsByIdWithoutFiscalYearIdParam() {
    String groupId = UUID.randomUUID().toString();

    Errors errors = verifyGet(String.format("/finance/groups/%s/expense-classes-totals", groupId), APPLICATION_JSON, 400).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals(errors.getErrors().get(0), MISSING_FISCAL_YEAR_ID.toError());
    verify(groupExpenseClassTotalsServiceMock, never()).getExpenseClassTotals(any(), any() , any());
  }


  /**
   * Define unit test specific beans to override actual ones
   */
  @Configuration
  static class ContextConfiguration {

    @Bean("groupExpenseClassTotalsMockService")
    @Primary
    public GroupExpenseClassTotalsService groupExpenseClassTotalsService() {
      return groupExpenseClassTotalsServiceMock;
    }
  }
}
