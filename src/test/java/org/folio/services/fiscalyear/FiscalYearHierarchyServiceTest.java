package org.folio.services.fiscalyear;

import static io.vertx.core.Future.succeededFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYearHierarchy;
import org.folio.rest.jaxrs.model.FiscalYearHierarchyCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class FiscalYearHierarchyServiceTest {
  private FiscalYearHierarchyService fiscalYearHierarchyService;
  private AutoCloseable mockitoMocks;

  @Mock
  private RestClient restClient;
  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
    fiscalYearHierarchyService = new FiscalYearHierarchyService(restClient);
  }

  @AfterEach
  public void afterEach() throws Exception {
    mockitoMocks.close();
  }

  @Test
  void testRetrieveFiscalYearHierarchyWithAllParams(VertxTestContext vertxTestContext) {
    String fiscalYearId = UUID.randomUUID().toString();
    FiscalYearHierarchyCollection collection = new FiscalYearHierarchyCollection()
      .withFiscalYearHierarchies(List.of(new FiscalYearHierarchy().withFiscalYearId(fiscalYearId)))
      .withTotalRecords(1);
    when(restClient.get(anyString(), eq(FiscalYearHierarchyCollection.class), eq(requestContext)))
      .thenReturn(succeededFuture(collection));

    var future = fiscalYearHierarchyService.retrieveFiscalYearHierarchy(fiscalYearId, "Active", "Active", "Active",
      "Active", "Inactive", requestContext);

    vertxTestContext.assertComplete(future)
      .onSuccess(result -> {
        assertEquals(collection, result);
        ArgumentCaptor<String> endpointCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClient).get(endpointCaptor.capture(), eq(FiscalYearHierarchyCollection.class), eq(requestContext));
        String endpoint = endpointCaptor.getValue();
        assertThat(endpoint, containsString("fiscalYearId=" + fiscalYearId));
        assertThat(endpoint, containsString("ledgerStatus=Active"));
        assertThat(endpoint, containsString("groupStatus=Active"));
        assertThat(endpoint, containsString("fundStatus=Active"));
        assertThat(endpoint, containsString("budgetStatus=Active"));
        assertThat(endpoint, containsString("expenseClassStatus=Inactive"));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testRetrieveFiscalYearHierarchySkipsBlankParams(VertxTestContext vertxTestContext) {
    String fiscalYearId = UUID.randomUUID().toString();
    FiscalYearHierarchyCollection collection = new FiscalYearHierarchyCollection().withTotalRecords(0);
    when(restClient.get(anyString(), eq(FiscalYearHierarchyCollection.class), eq(requestContext)))
      .thenReturn(succeededFuture(collection));

    var future = fiscalYearHierarchyService.retrieveFiscalYearHierarchy(fiscalYearId, null, "", " ", null, null,
      requestContext);

    vertxTestContext.assertComplete(future)
      .onSuccess(result -> {
        assertEquals(collection, result);
        ArgumentCaptor<String> endpointCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClient).get(endpointCaptor.capture(), eq(FiscalYearHierarchyCollection.class), eq(requestContext));
        String endpoint = endpointCaptor.getValue();
        assertThat(endpoint, containsString("fiscalYearId=" + fiscalYearId));
        assertThat(endpoint, not(containsString("ledgerStatus")));
        assertThat(endpoint, not(containsString("groupStatus")));
        assertThat(endpoint, not(containsString("fundStatus")));
        assertThat(endpoint, not(containsString("budgetStatus")));
        assertThat(endpoint, not(containsString("expenseClassStatus")));
        vertxTestContext.completeNow();
      });
  }
}
