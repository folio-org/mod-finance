package org.folio.services.protection;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.acq.model.finance.AcquisitionsUnitMembership;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembershipCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
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
public class AcqUnitMembershipsServiceTest {
  private RequestContext requestContext;

  @InjectMocks
  private AcqUnitMembershipsService acqUnitMembershipsService;
  @Mock
  private RestClient acqUnitMembershipsRestClient;


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
  void testShouldUseRestClientWhenRetrieveAcqUnitMembershipsByQuery(VertxTestContext vertxTestContext) {
    //Given
    String unitId = UUID.randomUUID().toString();
    String memberId = UUID.randomUUID().toString();
    AcquisitionsUnitMembershipCollection members = new AcquisitionsUnitMembershipCollection()
      .withAcquisitionsUnitMemberships(List.of(new AcquisitionsUnitMembership().withAcquisitionsUnitId(unitId).withId(memberId)))
      .withTotalRecords(1);

    doReturn(succeededFuture(members)).when(acqUnitMembershipsRestClient).get(anyString(), eq(AcquisitionsUnitMembershipCollection.class), eq(requestContext));

    var future = acqUnitMembershipsService.getAcquisitionsUnitsMemberships("query", 0, 10, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actMembers = result.result();
        assertThat(actMembers, equalTo(members));
        verify(acqUnitMembershipsRestClient).get(anyString(), eq(AcquisitionsUnitMembershipCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });

  }

}
