package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudget;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudgetCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class LedgerRolloverBudgetsServiceTest {

  @InjectMocks
  private LedgerRolloverBudgetsService ledgerRolloverBudgetsService;

  @Mock
  private RestClient restClient;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRolloversBudgets(VertxTestContext vertxTestContext) {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    when(restClient.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverBudgetCollection()));

    var future = ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgets(query, offset, limit, mock(RequestContext.class));
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());
          verify(restClient).get(assertQueryContains(query), eq(LedgerFiscalYearRolloverBudgetCollection.class), any(RequestContext.class));
          vertxTestContext.completeNow();
        });
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverBudgetsById(VertxTestContext vertxTestContext) {
    // Given
    String id = UUID.randomUUID().toString();

    when(restClient.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverBudget()));

    var future = ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(id,  mock(RequestContext.class));
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());

          verify(restClient).get(assertQueryContains(id), eq(LedgerFiscalYearRolloverBudget.class), any(RequestContext.class));
          vertxTestContext.completeNow();
        });
  }
}
