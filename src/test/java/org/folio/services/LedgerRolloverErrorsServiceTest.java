package org.folio.services;

import org.folio.rest.acq.model.finance.LedgerFiscalYearRolloverErrorCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class LedgerRolloverErrorsServiceTest {

  @InjectMocks
  private LedgerRolloverErrorsService ledgerRolloverErrorsService;

  @Mock
  private RestClient ledgerRolloverErrorsRestClientMock;

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRolloversErrors() {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    // When
    when(ledgerRolloverErrorsRestClientMock.get(anyString(), anyInt(), anyInt(), any(RequestContext.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverErrorCollection()));

    ledgerRolloverErrorsService.retrieveLedgersRolloverErrors(query, offset, limit, mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverErrorsRestClientMock).get(eq(query), eq(offset), eq(limit), any(RequestContext.class), eq(LedgerFiscalYearRolloverErrorCollection.class));
  }
}
