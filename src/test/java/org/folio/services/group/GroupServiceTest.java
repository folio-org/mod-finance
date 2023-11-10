package org.folio.services.group;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupCollection;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class GroupServiceTest {

  private RequestContext requestContext;
  @InjectMocks
  private GroupService groupService;
  @Mock
  private RestClient restClient;
  @Mock
  private AcqUnitsService acqUnitsService;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @Test
  void testShouldRetrieveFundsWithAcqUnits(VertxTestContext vertxTestContext) {
    String groupId = UUID.randomUUID().toString();
    Group group = new Group().withId(groupId);
    GroupCollection groupsCollection = new GroupCollection().withGroups(List.of(group)).withTotalRecords(1);

    when(acqUnitsService.buildAcqUnitsCqlClause(any())).thenReturn(succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL));
    doReturn(succeededFuture(groupsCollection)).when(restClient).get(anyString(), eq(GroupCollection.class), eq(requestContext));

    var future = groupService.getGroupsWithAcqUnitsRestriction(StringUtils.EMPTY, 0,10, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var actGroups = result.result();
        assertThat(groupsCollection, equalTo(actGroups));
        verify(restClient).get(assertQueryContains(NO_ACQ_UNIT_ASSIGNED_CQL), eq(GroupCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }
}
