package org.folio.services.financedata;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.services.protection.AcqUnitsService;
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
public class FinanceDataServiceTest {

  @InjectMocks
  private FinanceDataService financeDataService;

  @Mock
  private RestClient restClient;

  @Mock
  private AcqUnitsService acqUnitsService;

  private RequestContext requestContextMock;
  private AutoCloseable closeable;

  @BeforeEach
  void initMocks() {
    closeable = MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put("x-okapi-url", "http://localhost:9130"); // Ensure this URL is correct
    okapiHeaders.put("x-okapi-token", "token");
    okapiHeaders.put("x-okapi-tenant", "tenant");
    okapiHeaders.put("x-okapi-user-id", "userId");
    requestContextMock = new RequestContext(context, okapiHeaders);
  }

  @AfterEach
  void closeMocks() throws Exception {
    closeable.close();
  }

  @Test
  void shouldCallGetForRestClientWhenCalledGetFinanceDataWithAcqUnitsRestriction(VertxTestContext vertxTestContext) {
    String query = "fiscalYearId==db9c1ad6-026e-4b1a-9a99-032f41e7099b";
    String acqUnitIdsQuery = "fundAcqUnitIds=(1ee4b4e5-d621-4e43-8a76-0d904b0f491b) and budgetAcqUnitIds=(1ee4b4e5-d621-4e43-8a76-0d904b0f491b) or (" + NO_ACQ_UNIT_ASSIGNED_CQL + ")";
    String expectedQuery = "(" + acqUnitIdsQuery + ") and (" + query + ")";
    int offset = 0;
    int limit = 10;
    FyFinanceDataCollection fyFinanceDataCollection = new FyFinanceDataCollection();

    when(acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(any())).thenReturn(succeededFuture(acqUnitIdsQuery));
    when(restClient.get(anyString(), eq(FyFinanceDataCollection.class), any())).thenReturn(succeededFuture(fyFinanceDataCollection));

    var future = financeDataService.getFinanceDataWithAcqUnitsRestriction(query, offset, limit, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(FyFinanceDataCollection.class), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

}
