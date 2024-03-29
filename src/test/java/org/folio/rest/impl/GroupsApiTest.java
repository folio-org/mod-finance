package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.ErrorCodes.MISSING_FISCAL_YEAR_ID;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.GROUP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupCollection;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotalsCollection;
import org.folio.rest.util.RestTestUtils;
import org.folio.services.group.GroupExpenseClassTotalsService;
import org.folio.services.group.GroupService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.vertx.core.json.JsonObject;

public class GroupsApiTest {

  private static boolean runningOnOwn;
  @Autowired
  private GroupExpenseClassTotalsService groupExpenseClassTotalsServiceMock;
  @Autowired
  private GroupService groupServiceMock;

  public static final String GROUP_ENDPOINT = "finance/groups";

  @BeforeAll
  static void before() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(GroupsApiTest.ContextConfiguration.class);
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
    reset(groupExpenseClassTotalsServiceMock);
    reset(groupServiceMock);
  }

  @Test
  void testGetFinanceGroupsExpenseClassesTotalsById() {
    String groupId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    GroupExpenseClassTotalsCollection groupExpenseClassTotalsCollection = new GroupExpenseClassTotalsCollection();

    when(groupExpenseClassTotalsServiceMock.getExpenseClassTotals(anyString(), anyString(), any())).thenReturn(succeededFuture(groupExpenseClassTotalsCollection));

    GroupExpenseClassTotalsCollection result = RestTestUtils.verifyGet(String.format("/finance/groups/%s/expense-classes-totals?fiscalYearId=%s", groupId, fiscalYearId), APPLICATION_JSON, 200)
      .as(GroupExpenseClassTotalsCollection.class);


    assertEquals(groupExpenseClassTotalsCollection, result);
    verify(groupExpenseClassTotalsServiceMock).getExpenseClassTotals(eq(groupId), eq(fiscalYearId) , any(RequestContext.class));
  }

  @Test
  void testGetGroups() {
    Group group = GROUP.getMockObject().mapTo(Group.class);
    GroupCollection groupCollection = new GroupCollection()
      .withGroups(List.of(group))
      .withTotalRecords(1);

    addMockEntry(GROUP.name(), JsonObject.mapFrom(groupCollection));
    when(groupServiceMock.getGroupsWithAcqUnitsRestriction(anyString(), anyInt(), anyInt(), any())).thenReturn(succeededFuture(groupCollection));

    Map<String, Object> params = new HashMap<>();
    params.put("query", "status=Active");
    params.put("limit", 10);
    params.put("offset", 10);

    RestTestUtils.verifyGetWithParam(GROUP_ENDPOINT, APPLICATION_JSON, 200, params).as(GroupCollection.class);
  }

  @Test
  void testGetFinanceGroupsExpenseClassesTotalsByIdWithoutFiscalYearIdParam() {
    String groupId = UUID.randomUUID().toString();

    Errors errors = RestTestUtils.verifyGet(String.format("/finance/groups/%s/expense-classes-totals", groupId), APPLICATION_JSON, 400).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertEquals(errors.getErrors().get(0), MISSING_FISCAL_YEAR_ID.toError());
    verify(groupExpenseClassTotalsServiceMock, never()).getExpenseClassTotals(any(), any() , any());
  }


  /**
   * Define unit test specific beans to override actual ones
   */

  static class ContextConfiguration {

    @Bean
    public GroupExpenseClassTotalsService groupExpenseClassTotalsService() {
      return mock(GroupExpenseClassTotalsService.class);
    }

    @Bean
    public GroupService groupServiceMock() {
      return mock(GroupService.class);
    }
  }
}
