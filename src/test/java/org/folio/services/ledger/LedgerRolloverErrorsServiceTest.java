package org.folio.services.ledger;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverError;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverErrorCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LedgerRolloverErrorsServiceTest {

  @InjectMocks
  private LedgerRolloverErrorsService ledgerRolloverErrorsService;

  @Mock
  private RestClient restClient;

  @Mock
  private RequestContext requestContext;


  @Test
  void serviceGetShouldCallRestClientGet() {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;
    String contentType = "application/json";

    when(restClient.get(anyString(), anyInt(), anyInt(), eq(requestContext), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverErrorCollection()));

    // When
    ledgerRolloverErrorsService.getLedgerRolloverErrors(query, offset, limit, contentType, requestContext).join();

    // Then
    verify(restClient).get(eq(query), eq(offset), eq(limit), eq(requestContext), eq(LedgerFiscalYearRolloverErrorCollection.class));
  }

  @Test
  void shouldThrowHttpExceptionWhenGetIsCalledWithInvalidContentType() {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;
    String contentType = "application/xml";

    try {
      // When
      ledgerRolloverErrorsService.getLedgerRolloverErrors(query, offset, limit, contentType, requestContext).join();
      fail();
    } catch (HttpException e) {
      // Then
      assertEquals(415, e.getCode());
      assertEquals("Unsupported Media Type: " + contentType, e.getMessage());
    }
  }

  @Test
  void serviceCreateShouldCallRestClientPost() {
    // Given
    LedgerFiscalYearRolloverError rolloverError = new LedgerFiscalYearRolloverError();
    when(restClient.post(any(LedgerFiscalYearRolloverError.class), any(RequestContext.class), any()))
      .thenReturn(CompletableFuture.completedFuture(null));

    // When
    ledgerRolloverErrorsService.createLedgerRolloverError(rolloverError, requestContext).join();

    // Then
    verify(restClient).post(eq(rolloverError), eq(requestContext), eq(LedgerFiscalYearRolloverError.class));
  }

  @Test
  void serviceDeleteShouldCallRestClientDelete() {
    // Given
    String id = UUID.randomUUID().toString();
    when(restClient.delete(anyString(), any(RequestContext.class)))
      .thenReturn(CompletableFuture.completedFuture(null));

    // When
    ledgerRolloverErrorsService.deleteLedgerRolloverError(id, requestContext).join();

    // Then
    verify(restClient).delete(eq(id), eq(requestContext));
  }

}
