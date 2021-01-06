package org.folio.services.fiscalyear;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.FinancialSummary;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.rest.util.HelperUtils;
import org.folio.services.ConfigurationService;
import org.folio.services.budget.BudgetService;

public class FiscalYearService {

  private final RestClient fiscalYearRestClient;
  private final ConfigurationService configurationService;
  private final BudgetService budgetService;

  public FiscalYearService(RestClient fiscalYearRestClient, ConfigurationService configurationService, BudgetService budgetService) {
    this.fiscalYearRestClient = fiscalYearRestClient;
    this.configurationService = configurationService;
    this.budgetService = budgetService;
  }

  public CompletableFuture<FiscalYear> createFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    return configurationService.getSystemCurrency(requestContext)
      .thenCompose(currency -> {
        fiscalYear.setCurrency(currency);
        return fiscalYearRestClient.post(fiscalYear, requestContext, FiscalYear.class);
      });
  }

  public CompletableFuture<FiscalYearsCollection> getFiscalYears(String query, int offset, int limit, RequestContext requestContext) {
    return fiscalYearRestClient.get(query, offset, limit, requestContext, FiscalYearsCollection.class);
  }

  public CompletableFuture<FiscalYear> getFiscalYearById(String id, boolean withFinancialSummary, RequestContext requestContext) {
    return fiscalYearRestClient.getById(id, requestContext, FiscalYear.class)
      .thenCompose(fiscalYear -> {
        if (withFinancialSummary) {
          return withFinancialSummary(fiscalYear, requestContext);
        }
        return CompletableFuture.completedFuture(fiscalYear);
      });
  }

  public CompletableFuture<FiscalYear> getFiscalYearById(String id, RequestContext requestContext) {
    return getFiscalYearById(id, false, requestContext);
  }

  public CompletableFuture<Void> updateFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    return configurationService.getSystemCurrency(requestContext)
      .thenCompose(currency -> {
        fiscalYear.setCurrency(currency);
        return fiscalYearRestClient.put(fiscalYear.getId(), fiscalYear, requestContext);
      });

  }

  public CompletableFuture<Void> deleteFiscalYear(String id, RequestContext requestContext) {
    return fiscalYearRestClient.delete(id, requestContext);
  }


  public CompletableFuture<FiscalYear> withFinancialSummary(FiscalYear fiscalYear, RequestContext requestContext) {
    String query = "fiscalYearId==" + fiscalYear.getId();
    return budgetService.getBudgets(query, 0, Integer.MAX_VALUE, requestContext)
            .thenApply(budgetsCollection -> populateFinancialSummary(fiscalYear, budgetsCollection.getBudgets()));
  }

  private FiscalYear populateFinancialSummary(FiscalYear fiscalYear, List<Budget> budgets) {
    FinancialSummary financialSummary = new FinancialSummary()
            .withAllocated(HelperUtils.calculateTotals(budgets, Budget::getAllocated))
            .withAvailable(HelperUtils.calculateTotals(budgets, Budget::getAvailable))
            .withUnavailable(HelperUtils.calculateTotals(budgets, Budget::getUnavailable))
            .withInitialAllocation(HelperUtils.calculateTotals(budgets, Budget::getInitialAllocation))
            .withAllocationTo(HelperUtils.calculateTotals(budgets, Budget::getAllocationTo))
            .withAllocationFrom(HelperUtils.calculateTotals(budgets, Budget::getAllocationFrom))
            .withAwaitingPayment(HelperUtils.calculateTotals(budgets, Budget::getAwaitingPayment))
            .withEncumbered(HelperUtils.calculateTotals(budgets, Budget::getEncumbered))
            .withExpenditures(HelperUtils.calculateTotals(budgets, Budget::getExpenditures))
            .withOverEncumbrance(HelperUtils.calculateTotals(budgets, Budget::getOverEncumbrance))
            .withOverExpended(HelperUtils.calculateTotals(budgets, Budget::getOverExpended))
            .withTotalFunding(HelperUtils.calculateTotals(budgets, Budget::getTotalFunding))
            .withCashBalance(HelperUtils.calculateTotals(budgets, Budget::getCashBalance));
    return fiscalYear.withFinancialSummary(financialSummary);
  }
}
