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

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LedgerRolloverServiceTest {

  @InjectMocks
  private LedgerRolloverService ledgerRolloverService;

  @Mock
  private RestClient ledgerRolloverRestClientMock;

  @Test
  void shouldCallPostForRestClientWhenCalledCreateLedgerRollover() {
    // Given
    LedgerFiscalYearRollover ledgerFiscalYearRollover = new LedgerFiscalYearRollover()
      .withId(UUID.randomUUID().toString())
      .withFromFiscalYearId(UUID.randomUUID().toString())
      .withLedgerId(UUID.randomUUID().toString())
      .withToFiscalYearId(UUID.randomUUID().toString());

    // When
    when(ledgerRolloverRestClientMock.post(any(LedgerFiscalYearRollover.class), any(), any()))
      .thenReturn(succeededFuture(ledgerFiscalYearRollover));

    LedgerFiscalYearRollover ledgerRollover = ledgerRolloverService
      .createLedgerFyRollover(ledgerFiscalYearRollover, mock(RequestContext.class)).join();
    // Then
    assertThat(ledgerRollover, hasProperty("id"));
    assertThat(ledgerRollover, hasProperty("ledgerId"));
    assertThat(ledgerRollover, hasProperty("toFiscalYearId"));
    assertThat(ledgerRollover, hasProperty("fromFiscalYearId"));
    verify(ledgerRolloverRestClientMock).post(eq(ledgerFiscalYearRollover), any(RequestContext.class), eq(LedgerFiscalYearRollover.class));
  }

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRollovers() {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    // When
    when(ledgerRolloverRestClientMock.get(anyString(), anyInt(), anyInt(), any(RequestContext.class), any()))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverCollection()));

    ledgerRolloverService.retrieveLedgerRollovers(query, offset, limit, mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverRestClientMock).get(eq(query), eq(offset), eq(limit), any(RequestContext.class), eq(LedgerFiscalYearRolloverCollection.class));
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverById() {
    // Given
    String id = UUID.randomUUID().toString();

    // When
    when(ledgerRolloverRestClientMock.getById(anyString(), any(RequestContext.class), any()))
      .thenReturn(succeededFuture(new LedgerFiscalYearRollover()));

    ledgerRolloverService.retrieveLedgerRolloverById(id,  mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverRestClientMock).getById(eq(id), any(RequestContext.class), eq(LedgerFiscalYearRollover.class));
  }
}
