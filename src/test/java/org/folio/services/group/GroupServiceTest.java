package org.folio.services.group;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.finance.Group;
import org.folio.rest.acq.model.finance.GroupCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class GroupServiceTest {

  private RequestContext requestContext;
  @InjectMocks
  private GroupService groupService;
  @Mock
  private RestClient groupStorageRestClient;
  @Mock
  private AcqUnitsService acqUnitsService;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @Test
  void testShouldRetrieveFundsWithAcqUnits() {
    String groupId = UUID.randomUUID().toString();
    Group group = new Group().withId(groupId);
    GroupCollection groupsCollection = new GroupCollection().withGroups(List.of(group)).withTotalRecords(1);
    doReturn(completedFuture(NO_ACQ_UNIT_ASSIGNED_CQL)).when(acqUnitsService).buildAcqUnitsCqlClause(requestContext);
    doReturn(completedFuture(groupsCollection)).when(groupStorageRestClient).get(NO_ACQ_UNIT_ASSIGNED_CQL, 0, 10, requestContext, GroupCollection.class);

    GroupCollection actGroups = groupService.getGroupsWithAcqUnitsRestriction(StringUtils.EMPTY, 0,10, requestContext).join();

    assertThat(groupsCollection, equalTo(actGroups));
    verify(groupStorageRestClient).get(NO_ACQ_UNIT_ASSIGNED_CQL, 0, 10, requestContext, GroupCollection.class);
  }
}
