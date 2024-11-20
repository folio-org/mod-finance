package org.folio.services.protection;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.services.protection.AcqUnitConstants.FD_NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.AfterEach;
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
public class AcqUnitsServiceTest {
  private RequestContext requestContext;

  @InjectMocks
  AcqUnitsService acqUnitsService;
  @Mock
  private AcqUnitMembershipsService acqUnitMembershipsService;
  @Mock
  private RestClient restClient;

  private AutoCloseable closeable;

  @BeforeEach
  public void initMocks() {
    closeable = MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @AfterEach
  void clearContext() throws Exception {
    closeable.close();
  }

  @Test
  void testShouldUseRestClientWhenRetrieveAcqUnitByNotEmptyQuery(VertxTestContext vertxTestContext) {
    //Given
    String acqUnitId = UUID.randomUUID().toString();
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(List.of(new AcquisitionsUnit().withId(acqUnitId)))
      .withTotalRecords(1);

    doReturn(succeededFuture(units)).when(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
    //When
    var future = acqUnitsService.getAcquisitionsUnits("query", 0, 10, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var actUnits = result.result();
        assertThat(actUnits, equalTo(units));
        verify(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldUseRestClientWhenRetrieveAcqUnitByEmptyQuery(VertxTestContext vertxTestContext) {
    //Given
    String acqUnitId = UUID.randomUUID().toString();
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(List.of(new AcquisitionsUnit().withId(acqUnitId)))
      .withTotalRecords(1);

    doReturn(succeededFuture(units)).when(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
    //When
    var future = acqUnitsService.getAcquisitionsUnits(StringUtils.EMPTY, 0, 10, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var actUnits = result.result();
        assertThat(actUnits, equalTo(units));
        verify(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });

  }


  @Test
  void testShouldBuildCqlClauseWhenIdsIsEmpty(VertxTestContext vertxTestContext) {
     //Given
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(Collections.emptyList()).withTotalRecords(0);
    AcquisitionsUnitMembershipCollection members = new AcquisitionsUnitMembershipCollection()
      .withAcquisitionsUnitMemberships(Collections.emptyList()).withTotalRecords(0);
    doReturn(succeededFuture(units)).when(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
    doReturn(succeededFuture(members)).when(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(), anyInt(), anyInt(), eq(requestContext));
    //When
    var future = acqUnitsService.buildAcqUnitsCqlClause(requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var actClause = result.result();
        assertThat(actClause, equalTo(NO_ACQ_UNIT_ASSIGNED_CQL));
        verify(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
        verify(acqUnitMembershipsService).getAcquisitionsUnitsMemberships("userId==" + X_OKAPI_USER_ID.getValue(), 0, Integer.MAX_VALUE, requestContext);

        vertxTestContext.completeNow();
      });
  }


  @Test
  void testShouldBuildCqlClauseWhenIdsIsNotEmpty(VertxTestContext vertxTestContext) {
    //Given
    String unitId = UUID.randomUUID().toString();
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(Collections.emptyList()).withTotalRecords(0);
    String memberId = UUID.randomUUID().toString();
    AcquisitionsUnitMembershipCollection members = new AcquisitionsUnitMembershipCollection()
      .withAcquisitionsUnitMemberships(List.of(new AcquisitionsUnitMembership().withAcquisitionsUnitId(unitId).withId(memberId)))
      .withTotalRecords(1);
    doReturn(succeededFuture(units)).when(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
    doReturn(succeededFuture(members)).when(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(), anyInt(), anyInt(), eq(requestContext));
    //When
    var future = acqUnitsService.buildAcqUnitsCqlClause(requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var actClause = result.result();
        assertThat(actClause, equalTo("acqUnitIds=(" + unitId + ")" + " or " + "(" + NO_ACQ_UNIT_ASSIGNED_CQL + ")"));
        verify(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
        verify(acqUnitMembershipsService).getAcquisitionsUnitsMemberships("userId==" + X_OKAPI_USER_ID.getValue(), 0, Integer.MAX_VALUE, requestContext);
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testShouldBuildCqlClauseForFinanceDataWhenIdsIsEmpty(VertxTestContext vertxTestContext) {
    var units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(Collections.emptyList()).withTotalRecords(0);
    var members = new AcquisitionsUnitMembershipCollection()
      .withAcquisitionsUnitMemberships(Collections.emptyList()).withTotalRecords(0);
    doReturn(succeededFuture(units)).when(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
    doReturn(succeededFuture(members)).when(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(), anyInt(), anyInt(), eq(requestContext));

    var future = acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var actClause = result.result();
        assertThat(actClause, equalTo(NO_ACQ_UNIT_ASSIGNED_CQL));
        verify(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
        verify(acqUnitMembershipsService).getAcquisitionsUnitsMemberships("userId==" + X_OKAPI_USER_ID.getValue(), 0, Integer.MAX_VALUE, requestContext);

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldBuildCqlClauseForFinanceDataWhenIdsIsNotEmpty(VertxTestContext vertxTestContext) {
    var unitId = UUID.randomUUID().toString();
    var units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(Collections.emptyList()).withTotalRecords(0);
    var memberId = UUID.randomUUID().toString();
    var members = new AcquisitionsUnitMembershipCollection()
      .withAcquisitionsUnitMemberships(List.of(new AcquisitionsUnitMembership().withAcquisitionsUnitId(unitId).withId(memberId)))
      .withTotalRecords(1);
    doReturn(succeededFuture(units)).when(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
    doReturn(succeededFuture(members)).when(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(), anyInt(), anyInt(), eq(requestContext));

    var future = acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var actClause = result.result();
        assertThat(actClause, equalTo("(fundAcqUnitIds=(" + unitId + ") and budgetAcqUnitIds=(" + unitId + ")) or (" + FD_NO_ACQ_UNIT_ASSIGNED_CQL + ")"));
        verify(restClient).get(anyString(), eq(AcquisitionsUnitCollection.class), eq(requestContext));
        verify(acqUnitMembershipsService).getAcquisitionsUnitsMemberships("userId==" + X_OKAPI_USER_ID.getValue(), 0, Integer.MAX_VALUE, requestContext);

        vertxTestContext.completeNow();
      });
  }
}
