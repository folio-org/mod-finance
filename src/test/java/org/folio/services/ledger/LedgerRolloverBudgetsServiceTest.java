package org.folio.services.ledger;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudget;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudgetCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import io.vertx.core.Future;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LedgerRolloverBudgetsServiceTest {

  @InjectMocks
  private LedgerRolloverBudgetsService ledgerRolloverBudgetsService;

  @Mock
  private RestClient ledgerRolloverBudgetsRestClientMock;

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRolloversBudgets() {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    // When
    when(ledgerRolloverBudgetsRestClientMock.get(anyString(), anyInt(), anyInt(), any(RequestContext.class), any()))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverBudgetCollection()));

    ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgets(query, offset, limit, mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverBudgetsRestClientMock).get(eq(query), eq(offset), eq(limit), any(RequestContext.class), eq(LedgerFiscalYearRolloverBudgetCollection.class));
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverBudgetsById() {
    // Given
    String id = UUID.randomUUID().toString();

    // When
    when(ledgerRolloverBudgetsRestClientMock.getById(anyString(), any(RequestContext.class), any()))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverBudget()));

    ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(id,  mock(RequestContext.class)).join();

    // Then
    verify(ledgerRolloverBudgetsRestClientMock).getById(eq(id), any(RequestContext.class), eq(LedgerFiscalYearRolloverBudget.class));
  }
}
