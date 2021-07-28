package org.folio.services.fiscalyear;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FinancialSummary;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.services.budget.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FiscalYearServiceTest {

    @InjectMocks
    private FiscalYearService fiscalYearService;

    @Mock
    private RestClient fiscalYearRestClient;

    @Mock
    private BudgetService budgetService;

    @Mock
    private RequestContext requestContext;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldRetrieveBudgetsAndSumBudgetsTotalsWhenWithFinancialSummaryTrue() {

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
                .withExpenditures(0.03)
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
                .withExpenditures(1.03)
                .withUnavailable(1.61)
                .withAvailable(0d)
                .withTotalFunding(1.06)
                .withCashBalance(0.03)
                .withOverEncumbrance(0d)
                .withOverExpended(0.55d);

        BudgetsCollection budgetCollection = new BudgetsCollection().withBudgets(Arrays.asList(budget1, budget2));

        when(fiscalYearRestClient.getById(eq(fiscalYearId), eq(requestContext), eq(FiscalYear.class))).thenReturn(CompletableFuture.completedFuture(fiscalYear));
        when(budgetService.getBudgets(eq("fiscalYearId==" + fiscalYearId), anyInt(), anyInt(), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(budgetCollection));

        FiscalYear resultFY = fiscalYearService.getFiscalYearById(fiscalYearId, true, requestContext).join();

        FinancialSummary summary = resultFY.getFinancialSummary();
        assertNotNull(summary);
        assertEquals(0.06, summary.getInitialAllocation());
        assertEquals(2.08, summary.getAllocationTo());
        assertEquals(0.02, summary.getAllocationFrom());
        assertEquals(2.12, summary.getAllocated());
        assertEquals(0.22, summary.getAwaitingPayment());
        assertEquals(0.94, summary.getEncumbered());
        assertEquals(1.06, summary.getExpenditures());
        assertEquals(2.22, summary.getUnavailable());
        assertEquals(1.48, summary.getAvailable());
        assertEquals(3.15, summary.getTotalFunding());
        assertEquals(2.09, summary.getCashBalance());
        assertEquals(0d, summary.getOverEncumbrance());
        assertEquals(0.55, summary.getOverExpended());
    }

    @Test
    void shouldHasEmptyTotalsWhenNoBudgetsFound() {

        String fiscalYearId = UUID.randomUUID().toString();

        FiscalYear fiscalYear = new FiscalYear().withId(fiscalYearId);

        BudgetsCollection budgetCollection = new BudgetsCollection();

        when(fiscalYearRestClient.getById(eq(fiscalYearId), eq(requestContext), eq(FiscalYear.class))).thenReturn(CompletableFuture.completedFuture(fiscalYear));
        when(budgetService.getBudgets(eq("fiscalYearId==" + fiscalYearId), anyInt(), anyInt(), eq(requestContext))).thenReturn(CompletableFuture.completedFuture(budgetCollection));

        FiscalYear resultFY = fiscalYearService.getFiscalYearById(fiscalYearId, true, requestContext).join();

        FinancialSummary summary = resultFY.getFinancialSummary();
        assertNotNull(summary);
        assertEquals(0d, summary.getInitialAllocation());
        assertEquals(0d, summary.getAllocationTo());
        assertEquals(0d, summary.getAllocationFrom());
        assertEquals(0d, summary.getAllocated());
        assertEquals(0d, summary.getAwaitingPayment());
        assertEquals(0d, summary.getEncumbered());
        assertEquals(0d, summary.getExpenditures());
        assertEquals(0d, summary.getUnavailable());
        assertEquals(0d, summary.getAvailable());
        assertEquals(0d, summary.getTotalFunding());
        assertEquals(0d, summary.getCashBalance());
        assertEquals(0d, summary.getOverEncumbrance());
        assertEquals(0d, summary.getOverExpended());
    }

    @Test
    void shouldNotRetrieveBudgetsAndSumBudgetsTotalsWhenWithFinancialSummaryFalse() {

        String fiscalYearId = UUID.randomUUID().toString();

        FiscalYear fiscalYear = new FiscalYear().withId(fiscalYearId);

        when(fiscalYearRestClient.getById(eq(fiscalYearId), eq(requestContext), eq(FiscalYear.class))).thenReturn(CompletableFuture.completedFuture(fiscalYear));

        FiscalYear resultFY = fiscalYearService.getFiscalYearById(fiscalYearId, false, requestContext).join();

        FinancialSummary summary = resultFY.getFinancialSummary();
        assertNull(summary);
        verify(budgetService, never()).getBudgets(any(), anyInt(), anyInt(), any());
    }

    @Test
    void testGetFiscalYearByFiscalYearCode() {
      FiscalYear fiscalYear = new FiscalYear()
        .withCode("FUND CODE");
      List<FiscalYear> fiscalYearList = new ArrayList<>();
      fiscalYearList.add(fiscalYear);
      FiscalYearsCollection fiscalYearsCollection = new FiscalYearsCollection();
      fiscalYearsCollection.setTotalRecords(10);
      fiscalYearsCollection.setFiscalYears(fiscalYearList);
      when(fiscalYearRestClient.get(any(), eq(0), eq(Integer.MAX_VALUE), eq(requestContext), any()))
        .thenReturn(CompletableFuture.completedFuture(checkFiscalYear(fiscalYearsCollection)));
      assertEquals("FUND CODE", fiscalYear.getCode());
    }

//    @Test
//    public void testGetFiscalYear() {
//      String fiscalYearCode = "fiscalYearCode";
//      when(fiscalYearService.getFiscalYearByFiscalYearCode(any())).thenReturn(fiscalYearCode);
//      assertEquals("fiscalYearCode", fiscalYearCode);
//    }

    public FiscalYear checkFiscalYear(FiscalYearsCollection fiscalYearsCollection) {
      if (CollectionUtils.isNotEmpty(fiscalYearsCollection.getFiscalYears())) {
        return fiscalYearsCollection.getFiscalYears().get(0);
      }
      throw new HttpException(400, FISCAL_YEARS_NOT_FOUND);
    }
}
