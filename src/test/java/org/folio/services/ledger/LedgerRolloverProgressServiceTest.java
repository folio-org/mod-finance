package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgress;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgressCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class LedgerRolloverProgressServiceTest {

  @InjectMocks
  private LedgerRolloverProgressService ledgerRolloverProgressService;

  @Mock
  private RestClient restClient;
  @Mock
  RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldCallPostForRestClientWhenCalledCreateLedgerRolloverProgress(VertxTestContext vertxTestContext) {
    // Given
    LedgerFiscalYearRolloverProgress progress = new LedgerFiscalYearRolloverProgress()
      .withId(UUID.randomUUID().toString())
      .withLedgerRolloverId(UUID.randomUUID().toString());

    // When
    when(restClient.post(anyString() ,any(LedgerFiscalYearRolloverProgress.class), any(), any()))
      .thenReturn(succeededFuture(progress));

    var future = ledgerRolloverProgressService.createLedgerRolloverProgress(progress, requestContext);
    // Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var rolloverProgress = result.result();
        assertThat(rolloverProgress, hasProperty("id"));
        verify(restClient).post(anyString(), eq(progress), eq(LedgerFiscalYearRolloverProgress.class), any(RequestContext.class));

        vertxTestContext.completeNow();
      });

  }

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRolloversProgress(VertxTestContext vertxTestContext) {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    // When
    when(restClient.get(anyString(), eq(LedgerFiscalYearRolloverProgressCollection.class), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverProgressCollection()));

    var future = ledgerRolloverProgressService.retrieveLedgerRolloverProgresses(query, offset, limit, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        verify(restClient).get(assertQueryContains(query), eq(LedgerFiscalYearRolloverProgressCollection.class), any(RequestContext.class));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverProgressById(VertxTestContext vertxTestContext) {
    String id = UUID.randomUUID().toString();

    when(restClient.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverProgress()));

    var future = ledgerRolloverProgressService.retrieveLedgerRolloverProgressById(id, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        verify(restClient).get(assertQueryContains(id), eq(LedgerFiscalYearRolloverProgress.class), any(RequestContext.class));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void shouldCallPutForRestClientWhenCalledUpdateLedgerRolloverProgress(VertxTestContext vertxTestContext) {
    // Given
    String id = UUID.randomUUID().toString();

    // When
    when(restClient.put(anyString(), any(LedgerFiscalYearRolloverProgress.class), any(RequestContext.class)))
      .thenReturn(succeededFuture(null));

    LedgerFiscalYearRolloverProgress progress = new LedgerFiscalYearRolloverProgress()
      .withId(id)
      .withLedgerRolloverId(UUID.randomUUID().toString());

    var future = ledgerRolloverProgressService.updateLedgerRolloverProgressById(id, progress, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        verify(restClient).put(assertQueryContains(id), eq(progress), any(RequestContext.class));
        vertxTestContext.completeNow();
      });
  }
}
