package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class LedgerRolloverServiceTest {

  @InjectMocks
  private LedgerRolloverService ledgerRolloverService;

  @Mock
  private RestClient restClient;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldCallPostForRestClientWhenCalledCreateLedgerRollover(VertxTestContext vertxTestContext) {
    // Given
    LedgerFiscalYearRollover ledgerFiscalYearRollover = new LedgerFiscalYearRollover()
      .withId(UUID.randomUUID().toString())
      .withFromFiscalYearId(UUID.randomUUID().toString())
      .withLedgerId(UUID.randomUUID().toString())
      .withToFiscalYearId(UUID.randomUUID().toString());

    // When
    when(restClient.post(anyString(), any(LedgerFiscalYearRollover.class), any(), any()))
      .thenReturn(succeededFuture(ledgerFiscalYearRollover));

    var future = ledgerRolloverService.createLedgerFyRollover(ledgerFiscalYearRollover, new RequestContext(Vertx.currentContext(), new HashMap<>()));
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var ledgerRollover = result.result();
        assertThat(ledgerRollover, hasProperty("id"));
        assertThat(ledgerRollover, hasProperty("ledgerId"));
        assertThat(ledgerRollover, hasProperty("toFiscalYearId"));
        assertThat(ledgerRollover, hasProperty("fromFiscalYearId"));
        verify(restClient).post(anyString(), eq(ledgerFiscalYearRollover), eq(LedgerFiscalYearRollover.class), any(RequestContext.class));

        vertxTestContext.completeNow();
      });

  }

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRollovers(VertxTestContext vertxTestContext) {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    // When
    when(restClient.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverCollection()));

    var future = ledgerRolloverService.retrieveLedgerRollovers(query, offset, limit, new RequestContext(Vertx.currentContext(), new HashMap<>()));
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        verify(restClient).get(assertQueryContains(query), eq(LedgerFiscalYearRolloverCollection.class),
            any(RequestContext.class));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverById(VertxTestContext vertxTestContext) {
    // Given
    String id = UUID.randomUUID().toString();

    when(restClient.get(anyString(), eq(LedgerFiscalYearRollover.class), any()))
      .thenReturn(succeededFuture(new LedgerFiscalYearRollover()));

    var future = ledgerRolloverService.retrieveLedgerRolloverById(id, new RequestContext(Vertx.currentContext(), new HashMap<>()));
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        verify(restClient).get(assertQueryContains(id), eq(LedgerFiscalYearRollover.class), any());
        vertxTestContext.completeNow();
      });
  }
}
