package org.folio.services.ledger;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLog;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLogCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;

@ExtendWith(MockitoExtension.class)
public class LedgerRolloverLogsServiceTest {

  @InjectMocks
  private LedgerRolloverLogsService ledgerRolloverLogsService;

  @Mock
  private RestClient ledgerRolloverLogsRestClientMock;

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRolloversLogs() {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    // When
    when(ledgerRolloverLogsRestClientMock.get(anyString(), anyInt(), anyInt(), any(RequestContext.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverLogCollection()));

    ledgerRolloverLogsService.retrieveLedgerRolloverLogs(query, offset, limit, mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverLogsRestClientMock).get(eq(query), eq(offset), eq(limit), any(RequestContext.class), eq(LedgerFiscalYearRolloverLogCollection.class));
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverLogsById() {
    // Given
    String id = UUID.randomUUID().toString();

    // When
    when(ledgerRolloverLogsRestClientMock.getById(anyString(), any(RequestContext.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverLog()));

    ledgerRolloverLogsService.retrieveLedgerRolloverLogById(id,  mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverLogsRestClientMock).getById(eq(id), any(RequestContext.class), eq(LedgerFiscalYearRolloverLog.class));
  }

}
