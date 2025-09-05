package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
import org.folio.services.configuration.CommonSettingsService;
import org.junit.jupiter.api.AfterEach;
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
  @Mock
  private CommonSettingsService commonSettingsService;

  private AutoCloseable mockitoMocks;

  @BeforeEach
  public void initMocks() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void afterEach() throws Exception {
    mockitoMocks.close();
  }

  @Test
  void shouldCallGetForRestClientWhenCalledRetrieveLedgerRolloversBudgets(VertxTestContext vertxTestContext) {
    // Given
    String query = "query";
    int offset = 0;
    int limit = 0;

    when(restClient.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverBudgetCollection()));
    when(commonSettingsService.getSystemCurrency(any(RequestContext.class)))
      .thenReturn(succeededFuture("USD"));

    var future = ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgets(query, offset, limit, mock(RequestContext.class));
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());
          verify(restClient).get(assertQueryContains(query), eq(LedgerFiscalYearRolloverBudgetCollection.class), any(RequestContext.class));
          verify(commonSettingsService).getSystemCurrency(any(RequestContext.class));
          vertxTestContext.completeNow();
        });
  }

  @Test
  void shouldCallGetByIdForRestClientWhenCalledRetrieveLedgerRolloverBudgetsById(VertxTestContext vertxTestContext) {
    // Given
    String id = UUID.randomUUID().toString();

    when(restClient.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverBudget()));
    when(commonSettingsService.getSystemCurrency(any(RequestContext.class)))
      .thenReturn(succeededFuture("USD"));

    var future = ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(id,  mock(RequestContext.class));
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());

          verify(restClient).get(assertQueryContains(id), eq(LedgerFiscalYearRolloverBudget.class), any(RequestContext.class));
          verify(commonSettingsService).getSystemCurrency(any(RequestContext.class));
          vertxTestContext.completeNow();
        });
  }

  @Test
  void shouldReturnBudgetWithRoundedAmountsWhenCalledRetrieveLedgerRolloverBudgetById(VertxTestContext vertxTestContext) {
    // Given
    String id = UUID.randomUUID().toString();
    LedgerFiscalYearRolloverBudget budgetWithUnroundedAmounts = new LedgerFiscalYearRolloverBudget()
      .withId(id)
      .withInitialAllocation(100.123456)
      .withAllocationTo(200.987654)
      .withAllocationFrom(50.555555)
      .withAllocated(150.777777)
      .withNetTransfers(25.777777)
      .withEncumbered(75.333333)
      .withAwaitingPayment(125.666666)
      .withExpenditures(175.888888)
      .withCredits(225.111111)
      .withUnavailable(275.444444)
      .withAvailable(325.222222)
      .withCashBalance(375.999999)
      .withOverEncumbrance(425.123456)
      .withOverExpended(475.987654)
      .withTotalFunding(525.555555);

    when(restClient.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(budgetWithUnroundedAmounts));
    when(commonSettingsService.getSystemCurrency(any(RequestContext.class)))
      .thenReturn(succeededFuture("USD"));

    var future = ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(id, mock(RequestContext.class));
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        LedgerFiscalYearRolloverBudget roundedBudget = result.result();

        // Verify amounts are rounded to 2 decimal places (default for USD)
        assertThat(roundedBudget.getInitialAllocation(), is(100.12));
        assertThat(roundedBudget.getAllocationTo(), is(200.99));
        assertThat(roundedBudget.getAllocationFrom(), is(50.56));
        assertThat(roundedBudget.getAllocated(), is(150.78));
        assertThat(roundedBudget.getNetTransfers(), is(25.78));
        assertThat(roundedBudget.getEncumbered(), is(75.33));
        assertThat(roundedBudget.getAwaitingPayment(), is(125.67));
        assertThat(roundedBudget.getExpenditures(), is(175.89));
        assertThat(roundedBudget.getCredits(), is(225.11));
        assertThat(roundedBudget.getUnavailable(), is(275.44));
        assertThat(roundedBudget.getAvailable(), is(325.22));
        assertThat(roundedBudget.getCashBalance(), is(376.00));
        assertThat(roundedBudget.getOverEncumbrance(), is(425.12));
        assertThat(roundedBudget.getOverExpended(), is(475.99));
        assertThat(roundedBudget.getTotalFunding(), is(525.56));

        verify(restClient).get(assertQueryContains(id), eq(LedgerFiscalYearRolloverBudget.class), any(RequestContext.class));
        verify(commonSettingsService).getSystemCurrency(any(RequestContext.class));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void shouldHandleNullAmountsWhenCalledRetrieveLedgerRolloverBudgetById(VertxTestContext vertxTestContext) {
    // Given
    String id = UUID.randomUUID().toString();
    LedgerFiscalYearRolloverBudget budgetWithNullAmounts = new LedgerFiscalYearRolloverBudget()
      .withId(id)
      .withInitialAllocation(100.123456)
      .withAllocationTo(null)  // null amount
      .withAllocationFrom(50.555555)
      .withNetTransfers(null)  // null amount
      .withEncumbered(75.333333)
      .withAwaitingPayment(null)  // null amount
      .withExpenditures(175.888888)
      .withCredits(225.111111)
      .withUnavailable(null)  // null amount - this is the one mentioned in the requirement
      .withAvailable(325.222222)
      .withCashBalance(null)  // null amount
      .withOverEncumbrance(425.123456)
      .withOverExpended(null)  // null amount
      .withTotalFunding(525.555555);

    when(restClient.get(anyString(), any(), any(RequestContext.class)))
      .thenReturn(succeededFuture(budgetWithNullAmounts));
    when(commonSettingsService.getSystemCurrency(any(RequestContext.class)))
      .thenReturn(succeededFuture("USD"));

    var future = ledgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(id, mock(RequestContext.class));
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        LedgerFiscalYearRolloverBudget roundedBudget = result.result();

        // Verify non-null amounts are rounded
        assertThat(roundedBudget.getInitialAllocation(), is(100.12));
        assertThat(roundedBudget.getAllocationFrom(), is(50.56));
        assertThat(roundedBudget.getEncumbered(), is(75.33));
        assertThat(roundedBudget.getExpenditures(), is(175.89));
        assertThat(roundedBudget.getCredits(), is(225.11));
        assertThat(roundedBudget.getAvailable(), is(325.22));
        assertThat(roundedBudget.getOverEncumbrance(), is(425.12));
        assertThat(roundedBudget.getTotalFunding(), is(525.56));

        // Verify null amounts remain null
        assertThat(roundedBudget.getAllocationTo(), is(nullValue()));
        assertThat(roundedBudget.getNetTransfers(), is(nullValue()));
        assertThat(roundedBudget.getAwaitingPayment(), is(nullValue()));
        assertThat(roundedBudget.getUnavailable(), is(nullValue()));
        assertThat(roundedBudget.getCashBalance(), is(nullValue()));
        assertThat(roundedBudget.getOverExpended(), is(nullValue()));

        verify(restClient).get(assertQueryContains(id), eq(LedgerFiscalYearRolloverBudget.class), any(RequestContext.class));
        verify(commonSettingsService).getSystemCurrency(any(RequestContext.class));
        vertxTestContext.completeNow();
      });
  }
}
