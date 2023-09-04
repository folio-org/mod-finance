package org.folio.services.ledger;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
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
import io.vertx.core.Future;

import static io.vertx.core.Future.succeededFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  void serviceGetShouldCallRestClientGet(VertxTestContext vertxTestContext) {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;
    String contentType = "application/json";

    when(restClient.get(anyString(), any(), eq(requestContext)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverErrorCollection()));

    // When
    var future = ledgerRolloverErrorsService.getLedgerRolloverErrors(query, offset, limit, contentType, requestContext);
    // Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        verify(restClient).get(eq(query), eq(LedgerFiscalYearRolloverErrorCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });

  }

  @Test
  void shouldThrowHttpExceptionWhenGetIsCalledWithInvalidContentType(VertxTestContext vertxTestContext) {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;
    String contentType = "application/xml";

    var future = ledgerRolloverErrorsService.getLedgerRolloverErrors(query, offset, limit, contentType, requestContext);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        var exception = (HttpException) result.cause();
        assertEquals(415, exception.getCode());
        assertEquals("Unsupported Media Type: " + contentType, exception.getMessage());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void serviceCreateShouldCallRestClientPost(VertxTestContext vertxTestContext) {
    // Given
    LedgerFiscalYearRolloverError rolloverError = new LedgerFiscalYearRolloverError();
    when(restClient.post(anyString(), any(LedgerFiscalYearRolloverError.class), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(null));

    var future = ledgerRolloverErrorsService.createLedgerRolloverError(rolloverError, requestContext);

    vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());
          verify(restClient).post(anyString(), eq(rolloverError), eq(LedgerFiscalYearRolloverError.class), eq(requestContext));

          vertxTestContext.completeNow();
        });

    // Then
  }

  @Test
  void serviceDeleteShouldCallRestClientDelete(VertxTestContext vertxTestContext) {
    // Given
    String id = UUID.randomUUID().toString();
    when(restClient.delete(anyString(), any(RequestContext.class)))
      .thenReturn(succeededFuture(null));

    var future = ledgerRolloverErrorsService.deleteLedgerRolloverError(id, requestContext);
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());
          verify(restClient).delete(eq(id), eq(requestContext));
          vertxTestContext.completeNow();
        });

  }

}
