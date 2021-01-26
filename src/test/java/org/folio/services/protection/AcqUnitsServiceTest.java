package org.folio.services.protection;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.finance.AcquisitionsUnit;
import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembership;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembershipCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class AcqUnitsServiceTest {
  private RequestContext requestContext;

  @InjectMocks
  AcqUnitsService acqUnitsService;
  @Mock
  private AcqUnitMembershipsService acqUnitMembershipsService;
  @Mock
  private RestClient acqUnitsStorageRestClient;

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
  void testShouldUseRestClientWhenRetrieveAcqUnitByNotEmptyQuery() {
    //Given
    String acqUnitId = UUID.randomUUID().toString();
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(List.of(new AcquisitionsUnit().withId(acqUnitId)))
      .withTotalRecords(1);

    doReturn(completedFuture(units)).when(acqUnitsStorageRestClient).get("(isDeleted==false) and (query)", 0,10, requestContext, AcquisitionsUnitCollection.class);
    //When
    AcquisitionsUnitCollection actUnits = acqUnitsService.getAcquisitionsUnits("query", 0, 10, requestContext).join();
    //Then
    assertThat(actUnits, equalTo(units));
    verify(acqUnitsStorageRestClient).get("(isDeleted==false) and (query)", 0,10, requestContext, AcquisitionsUnitCollection.class);
  }


  @Test
  void testShouldUseRestClientWhenRetrieveAcqUnitByEmptyQuery() {
    //Given
    String acqUnitId = UUID.randomUUID().toString();
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(List.of(new AcquisitionsUnit().withId(acqUnitId)))
      .withTotalRecords(1);

    doReturn(completedFuture(units)).when(acqUnitsStorageRestClient).get("isDeleted==false", 0,10, requestContext, AcquisitionsUnitCollection.class);
    //When
    AcquisitionsUnitCollection actUnits = acqUnitsService.getAcquisitionsUnits(StringUtils.EMPTY, 0, 10, requestContext).join();
    //Then
    assertThat(actUnits, equalTo(units));
    verify(acqUnitsStorageRestClient).get("isDeleted==false", 0,10, requestContext, AcquisitionsUnitCollection.class);
  }


  @Test
  void testShouldBuildCqlClauseWhenIdsIsEmpty() {
     //Given
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(Collections.emptyList()).withTotalRecords(0);
    AcquisitionsUnitMembershipCollection members = new AcquisitionsUnitMembershipCollection()
      .withAcquisitionsUnitMemberships(Collections.emptyList()).withTotalRecords(0);
    doReturn(completedFuture(units)).when(acqUnitsStorageRestClient).get(anyString(), anyInt(), anyInt(), eq(requestContext), eq(AcquisitionsUnitCollection.class));
    doReturn(completedFuture(members)).when(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(),  anyInt(), anyInt(), eq(requestContext));
    //When
    String actClause = acqUnitsService.buildAcqUnitsCqlClause(requestContext).join();
    //Then
    assertThat(actClause, equalTo(NO_ACQ_UNIT_ASSIGNED_CQL));
    verify(acqUnitsStorageRestClient).get("(isDeleted==false) and (protectRead==false)", 0, Integer.MAX_VALUE, requestContext, AcquisitionsUnitCollection.class);
    verify(acqUnitMembershipsService).getAcquisitionsUnitsMemberships("userId==" + X_OKAPI_USER_ID.getValue(), 0, Integer.MAX_VALUE, requestContext);
  }


  @Test
  void testShouldBuildCqlClauseWhenIdsIsNotEmpty() {
    //Given
    String unitId = UUID.randomUUID().toString();
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(Collections.emptyList()).withTotalRecords(0);
    String memberId = UUID.randomUUID().toString();
    AcquisitionsUnitMembershipCollection members = new AcquisitionsUnitMembershipCollection()
      .withAcquisitionsUnitMemberships(List.of(new AcquisitionsUnitMembership().withAcquisitionsUnitId(unitId).withId(memberId)))
      .withTotalRecords(1);
    doReturn(completedFuture(units)).when(acqUnitsStorageRestClient).get(anyString(), anyInt(), anyInt(), eq(requestContext), eq(AcquisitionsUnitCollection.class));
    doReturn(completedFuture(members)).when(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(),  anyInt(), anyInt(), eq(requestContext));
    //When
    String actClause = acqUnitsService.buildAcqUnitsCqlClause(requestContext).join();
    //Then
    assertThat(actClause, equalTo("acqUnitIds=(" +unitId+ ")" + " or " + "(" +NO_ACQ_UNIT_ASSIGNED_CQL + ")"));
    verify(acqUnitsStorageRestClient).get("(isDeleted==false) and (protectRead==false)", 0, Integer.MAX_VALUE, requestContext, AcquisitionsUnitCollection.class);
    verify(acqUnitMembershipsService).getAcquisitionsUnitsMemberships("userId==" + X_OKAPI_USER_ID.getValue(), 0, Integer.MAX_VALUE, requestContext);
  }
}
