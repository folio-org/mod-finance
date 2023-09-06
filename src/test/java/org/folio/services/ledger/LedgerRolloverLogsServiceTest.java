package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLog;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLogCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class LedgerRolloverLogsServiceTest {

  @InjectMocks
  private LedgerRolloverLogsService ledgerRolloverLogsService;

  @Mock
  private RestClient ledgerRolloverLogsRestClientMock;
  @
    BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRolloversLogs(VertxTestContext vertxTestContext) {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    // When
    when(ledgerRolloverLogsRestClientMock.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverLogCollection()));

    var future = ledgerRolloverLogsService.retrieveLedgerRolloverLogs(query, offset, limit, mock(RequestContext.class));
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        verify(ledgerRolloverLogsRestClientMock).get(assertQueryContains(query), eq(LedgerFiscalYearRolloverLogCollection.class), any(RequestContext.class));

        vertxTestContext.completeNow();
      });
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverLogsById(VertxTestContext vertxTestContext) {
    // Given
    String id = UUID.randomUUID().toString();

    // When
    when(ledgerRolloverLogsRestClientMock.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverLog()));

    var future = ledgerRolloverLogsService.retrieveLedgerRolloverLogById(id, mock(RequestContext.class));
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        verify(ledgerRolloverLogsRestClientMock).get(assertQueryContains(id), eq(LedgerFiscalYearRolloverLog.class), any(RequestContext.class));
        vertxTestContext.completeNow();
      });
  }

}
