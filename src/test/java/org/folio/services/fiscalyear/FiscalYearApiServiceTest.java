package org.folio.services.fiscalyear;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FinancialSummary;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.services.budget.BudgetService;
import org.folio.services.configuration.CommonSettingsService;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class FiscalYearApiServiceTest {
  private FiscalYearApiService fiscalYearApiService;
  private AutoCloseable mockitoMocks;

  @Mock
  private RestClient restClient;
  @Mock
  private BudgetService budgetService;
  @Mock
  private RequestContext requestContext;
  @Mock
  private CommonSettingsService commonSettingsService;
  @Mock
  private AcqUnitsService acqUnitsService;

  @BeforeEach
  public void initMocks() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
    FiscalYearService fiscalYearService = new FiscalYearService(restClient);
    fiscalYearApiService = new FiscalYearApiService(fiscalYearService, commonSettingsService, budgetService, acqUnitsService);
  }

  @AfterEach
  public void afterEach() throws Exception {
    mockitoMocks.close();
  }

  @Test
  void shouldRetrieveBudgetsAndSumBudgetsTotalsWhenWithFinancialSummaryTrue(VertxTestContext vertxTestContext) {
    String fiscalYearId = UUID.randomUUID().toString();
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalYearId);

    Budget budget1 = new Budget()
      .withInitialAllocation(0.03)
      .withAllocationTo(1.04)
      .withAllocationFrom(0.01)
      .withAllocated(1.06)
      .withNetTransfers(1.03)
      .withAwaitingPayment(0.11)
      .withEncumbered(0.47)
      .withExpenditures(0.04)
      .withCredits(0.01)
      .withUnavailable(0.61)
      .withAvailable(1.48)
      .withTotalFunding(2.09)
      .withCashBalance(2.06)
      .withOverEncumbrance(0d)
      .withOverExpended(0d);

    Budget budget2 = new Budget()
      .withInitialAllocation(0.03)
      .withAllocationTo(1.04)
      .withAllocationFrom(0.01)
      .withAllocated(1.06)
      .withNetTransfers(0d)
      .withAwaitingPayment(0.11)
      .withEncumbered(0.47)
      .withExpenditures(2.04)
      .withCredits(1.01)
      .withUnavailable(1.61)
      .withAvailable(0d)
      .withTotalFunding(1.06)
      .withCashBalance(0.03)
      .withOverEncumbrance(0d)
      .withOverExpended(0.55d);

    BudgetsCollection budgetCollection = new BudgetsCollection().withBudgets(Arrays.asList(budget1, budget2));

    when(restClient.get(eq(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYearId)), eq(FiscalYear.class), eq(requestContext)))
      .thenReturn(succeededFuture(fiscalYear));
    when(budgetService.getBudgets(eq("fiscalYearId==" + fiscalYearId), anyInt(), anyInt(), eq(requestContext))).thenReturn(succeededFuture(budgetCollection));

    var future = fiscalYearApiService.getFiscalYearById(fiscalYearId, true, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        FinancialSummary summary = result.result().getFinancialSummary();
        assertNotNull(summary);
        assertEquals(0.06, summary.getInitialAllocation());
        assertEquals(2.08, summary.getAllocationTo());
        assertEquals(0.02, summary.getAllocationFrom());
        assertEquals(2.12, summary.getAllocated());
        assertEquals(0.22, summary.getAwaitingPayment());
        assertEquals(0.94, summary.getEncumbered());
        assertEquals(2.08, summary.getExpenditures());
        assertEquals(1.02, summary.getCredits());
        assertEquals(2.22, summary.getUnavailable());
        assertEquals(1.48, summary.getAvailable());
        assertEquals(3.15, summary.getTotalFunding());
        assertEquals(2.09, summary.getCashBalance());
        assertEquals(0d, summary.getOverEncumbrance());
        assertEquals(0.55, summary.getOverExpended());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void shouldHasEmptyTotalsWhenNoBudgetsFound(VertxTestContext vertxTestContext) {
    String fiscalYearId = UUID.randomUUID().toString();
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalYearId);
    BudgetsCollection budgetCollection = new BudgetsCollection();

    when(restClient.get(eq(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYearId)), eq(FiscalYear.class), eq(requestContext)))
      .thenReturn(succeededFuture(fiscalYear));
    when(budgetService.getBudgets(eq("fiscalYearId==" + fiscalYearId), anyInt(), anyInt(), eq(requestContext))).thenReturn(succeededFuture(budgetCollection));

    var future = fiscalYearApiService.getFiscalYearById(fiscalYearId, true, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        FinancialSummary summary = result.result().getFinancialSummary();
        assertNotNull(summary);
        assertEquals(0d, summary.getInitialAllocation());
        assertEquals(0d, summary.getAllocationTo());
        assertEquals(0d, summary.getAllocationFrom());
        assertEquals(0d, summary.getAllocated());
        assertEquals(0d, summary.getAwaitingPayment());
        assertEquals(0d, summary.getEncumbered());
        assertEquals(0d, summary.getExpenditures());
        assertEquals(0d, summary.getCredits());
        assertEquals(0d, summary.getUnavailable());
        assertEquals(0d, summary.getAvailable());
        assertEquals(0d, summary.getTotalFunding());
        assertEquals(0d, summary.getCashBalance());
        assertEquals(0d, summary.getOverEncumbrance());
        assertEquals(0d, summary.getOverExpended());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void shouldNotRetrieveBudgetsAndSumBudgetsTotalsWhenWithFinancialSummaryFalse(VertxTestContext vertxTestContext) {
    String fiscalYearId = UUID.randomUUID().toString();
    FiscalYear fiscalYear = new FiscalYear().withId(fiscalYearId);

    when(restClient.get(eq(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYearId)), eq(FiscalYear.class), eq(requestContext)))
      .thenReturn(succeededFuture(fiscalYear));

    var future = fiscalYearApiService.getFiscalYearById(fiscalYearId, false, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        FinancialSummary summary = result.result().getFinancialSummary();
        assertNull(summary);
        verify(budgetService, never()).getBudgets(any(), anyInt(), anyInt(), any());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldRetrieveFiscalYearsWithAcqUnits(VertxTestContext vertxTestContext) {
    //Given
    FiscalYear fiscalYear = new FiscalYear().withId(UUID.randomUUID().toString()).withCode("TST");
    FiscalYearsCollection fiscalYearsCollection = new FiscalYearsCollection().withFiscalYears(List.of(fiscalYear)).withTotalRecords(1);
    doReturn(succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL)).when(acqUnitsService).buildAcqUnitsCqlClause(requestContext);
    doReturn(succeededFuture(fiscalYearsCollection))
      .when(restClient).get(any(), eq(FiscalYearsCollection.class), eq(requestContext));
    //When
    var future = fiscalYearApiService.getFiscalYearsWithAcqUnitsRestriction(StringUtils.EMPTY, 0,10, requestContext);
    //Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertThat(fiscalYearsCollection, equalTo(result.result()));
        verify(restClient).get(anyString(), eq(FiscalYearsCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }

}
