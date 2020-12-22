package org.folio.services;

import org.folio.rest.acq.model.finance.LedgerFiscalYearRolloverErrorCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
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
    String contentType = "application/json";

    // When
    when(ledgerRolloverErrorsRestClientMock.get(anyString(), anyInt(), anyInt(), any(RequestContext.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverErrorCollection()));

    ledgerRolloverErrorsService.retrieveLedgersRolloverErrors(query, offset, limit, contentType, mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverErrorsRestClientMock).get(eq(query), eq(offset), eq(limit), any(RequestContext.class), eq(LedgerFiscalYearRolloverErrorCollection.class));
  }

  @Test
  void shouldThrowHttpExceptionWhenCalledRetrieveLedgerRolloversErrorsWithInvalidContentType() {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;
    String contentType = "application/xml";

    try {
      // When
      ledgerRolloverErrorsService.retrieveLedgersRolloverErrors(query, offset, limit, contentType, mock(RequestContext.class)).join();
      fail();
    } catch (HttpException e) {
      // Then
      assertEquals(415, e.getCode());
      assertEquals("Unsupported Media Type: " + contentType, e.getMessage());
    }
  }
}
