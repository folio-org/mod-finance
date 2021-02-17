package org.folio.services.ledger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgress;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgressCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class LedgerRolloverProgressServiceTest {

  @InjectMocks
  private LedgerRolloverProgressService ledgerRolloverProgressService;

  @Mock
  private RestClient ledgerRolloverProgressRestClientMock;

  @Test
  void shouldCallPostForRestClientWhenCalledCreateLedgerRolloverProgress() {
    // Given
    LedgerFiscalYearRolloverProgress progress = new LedgerFiscalYearRolloverProgress()
      .withId(UUID.randomUUID().toString())
      .withLedgerRolloverId(UUID.randomUUID().toString());

    // When
    when(ledgerRolloverProgressRestClientMock.post(any(LedgerFiscalYearRolloverProgress.class), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(progress));

    LedgerFiscalYearRolloverProgress rolloverProgress = ledgerRolloverProgressService
      .createLedgerRolloverProgress(progress, mock(RequestContext.class)).join();
    // Then
    assertThat(rolloverProgress, hasProperty("id"));
    verify(ledgerRolloverProgressRestClientMock).post(eq(progress), any(RequestContext.class), eq(LedgerFiscalYearRolloverProgress.class));
  }

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRolloversProgress() {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    // When
    when(ledgerRolloverProgressRestClientMock.get(anyString(), anyInt(), anyInt(), any(RequestContext.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverProgressCollection()));

    ledgerRolloverProgressService.retrieveLedgerRolloverProgresses(query, offset, limit, mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverProgressRestClientMock).get(eq(query), eq(offset), eq(limit), any(RequestContext.class), eq(LedgerFiscalYearRolloverProgressCollection.class));
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverProgressById() {
    // Given
    String id = UUID.randomUUID().toString();

    // When
    when(ledgerRolloverProgressRestClientMock.getById(anyString(), any(RequestContext.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverProgress()));

    ledgerRolloverProgressService.retrieveLedgerRolloverProgressById(id,  mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverProgressRestClientMock).getById(eq(id), any(RequestContext.class), eq(LedgerFiscalYearRolloverProgress.class));
  }

  @Test
  void shouldCallPutForRestClientWhenCalledUpdateLedgerRolloverProgress() {
    // Given
    String id = UUID.randomUUID().toString();

    // When
    when(ledgerRolloverProgressRestClientMock.put(anyString(), any(LedgerFiscalYearRolloverProgress.class), any(RequestContext.class)))
      .thenReturn(CompletableFuture.completedFuture(null));

    LedgerFiscalYearRolloverProgress progress = new LedgerFiscalYearRolloverProgress()
      .withId(id)
      .withLedgerRolloverId(UUID.randomUUID().toString());
    ledgerRolloverProgressService.updateLedgerRolloverProgressById(id, progress, mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverProgressRestClientMock).put(eq(id), eq(progress), any(RequestContext.class));
  }
}
