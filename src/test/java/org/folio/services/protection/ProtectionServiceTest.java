package org.folio.services.protection;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.ErrorCodes.USER_HAS_NO_PERMISSIONS;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import io.vertx.core.Future;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.folio.HttpStatus;
import org.folio.rest.acq.model.finance.AcquisitionsUnit;
import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembership;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembershipCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.services.protection.models.ProtectedOperationType;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class ProtectionServiceTest {
  private RequestContext requestContext;

  @InjectMocks
  private ProtectionService protectionService;

  @Mock
  private AcqUnitsService acqUnitsService;
  @Mock
  private AcqUnitMembershipsService acqUnitMembershipsService;

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
  void testThatOperationAllowableIfUserHasUnitsTheSameWithObject() {
    //Given
    String acqUnitId = UUID.randomUUID().toString();
    List<String> unitIds = List.of(acqUnitId);
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(List.of(new AcquisitionsUnit().withId(acqUnitId).withProtectRead(true)))
      .withTotalRecords(1);
    String memberId = UUID.randomUUID().toString();
    AcquisitionsUnitMembershipCollection members = new AcquisitionsUnitMembershipCollection()
      .withAcquisitionsUnitMemberships(List.of(new AcquisitionsUnitMembership().withAcquisitionsUnitId(acqUnitId).withId(memberId)))
      .withTotalRecords(1);

    doReturn(completedFuture(members)).when(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(), anyInt(),anyInt(), eq(requestContext));
    doReturn(completedFuture(units)).when(acqUnitsService).getAcquisitionsUnits(anyString(), anyInt(),anyInt(), eq(requestContext));
    //When
    protectionService.checkOperationsRestrictions(unitIds, Set.of(ProtectedOperationType.READ), requestContext).join();
    //Then
    verify(acqUnitsService).getAcquisitionsUnits(anyString(), anyInt(),anyInt(), eq(requestContext));
    verify(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(), anyInt(),anyInt(), eq(requestContext));
  }

  @Test
  void testThatOperationAllowableIfObjectIsEmpty() {
    //When
    protectionService.checkOperationsRestrictions(Collections.emptyList(), Set.of(ProtectedOperationType.READ), requestContext).join();
    //Then
    verify(acqUnitsService, times(0)).getAcquisitionsUnits(anyString(), anyInt(),anyInt(), eq(requestContext));
    verify(acqUnitMembershipsService, times(0)).getAcquisitionsUnitsMemberships(anyString(), anyInt(),anyInt(), eq(requestContext));
  }

  @Test
  void testThatOperationForbiddenIfUserHasNotTheSameUnitsWithObject() {
    //Given
    String acqUnitId = UUID.randomUUID().toString();
    List<String> unitIds = List.of(acqUnitId);
    AcquisitionsUnitCollection units = new AcquisitionsUnitCollection()
      .withAcquisitionsUnits(List.of(new AcquisitionsUnit().withId(acqUnitId).withProtectRead(true)))
      .withTotalRecords(1);
    AcquisitionsUnitMembershipCollection members = new AcquisitionsUnitMembershipCollection().withTotalRecords(0);

    doReturn(completedFuture(members)).when(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(), anyInt(),anyInt(), eq(requestContext));
    doReturn(completedFuture(units)).when(acqUnitsService).getAcquisitionsUnits(anyString(), anyInt(),anyInt(), eq(requestContext));
    //When
    Future<Void> future = protectionService.checkOperationsRestrictions(unitIds, Set.of(ProtectedOperationType.READ), requestContext);
    ExecutionException executionException = assertThrows(ExecutionException.class, future::get);

    assertThat(executionException.getCause().getCause(), IsInstanceOf.instanceOf(HttpException.class));

    HttpException httpException = (HttpException) executionException.getCause().getCause();

    assertEquals(HttpStatus.HTTP_FORBIDDEN.toInt(), httpException.getCode());
    Error error = httpException.getErrors().getErrors().get(0);
    assertEquals(USER_HAS_NO_PERMISSIONS.getCode(), error.getCode());

    //Then
    verify(acqUnitsService).getAcquisitionsUnits(anyString(), anyInt(),anyInt(), eq(requestContext));
    verify(acqUnitMembershipsService).getAcquisitionsUnitsMemberships(anyString(), anyInt(),anyInt(), eq(requestContext));
  }

}
