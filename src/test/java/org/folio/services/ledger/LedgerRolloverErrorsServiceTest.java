package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverError;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverErrorCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class LedgerRolloverErrorsServiceTest {

  @InjectMocks
  private LedgerRolloverErrorsService ledgerRolloverErrorsService;

  @Mock
  private RestClient restClient;

  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void serviceGetShouldCallRestClientGet(VertxTestContext vertxTestContext) {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;
    String contentType = "application/json";

    when(restClient.get(anyString(), eq(LedgerFiscalYearRolloverErrorCollection.class), eq(requestContext)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverErrorCollection()));

    var future = ledgerRolloverErrorsService.getLedgerRolloverErrors(query, offset, limit, contentType, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        verify(restClient).get(assertQueryContains(query), eq(LedgerFiscalYearRolloverErrorCollection.class), eq(requestContext));
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
    when(restClient.post(anyString(), any(LedgerFiscalYearRolloverError.class), eq(LedgerFiscalYearRolloverError.class), any(RequestContext.class)))
      .thenReturn(succeededFuture(null));

    var future = ledgerRolloverErrorsService.createLedgerRolloverError(rolloverError, requestContext);

    vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());
          verify(restClient).post(anyString(), eq(rolloverError), eq(LedgerFiscalYearRolloverError.class), eq(requestContext));

          vertxTestContext.completeNow();
        });
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
          verify(restClient).delete(assertQueryContains(id), eq(requestContext));
          vertxTestContext.completeNow();
        });

  }

}
