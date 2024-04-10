package org.folio.services.fiscalyear;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.FinancialSummary;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.rest.util.HelperUtils;
import org.folio.services.budget.BudgetService;
import org.folio.services.configuration.ConfigurationEntriesService;
import org.folio.services.protection.AcqUnitsService;

import io.vertx.core.Future;

public class FiscalYearApiService {

  private static final Logger log = LogManager.getLogger();

  private final FiscalYearService fiscalYearService;
  private final ConfigurationEntriesService configurationEntriesService;
  private final BudgetService budgetService;
  private final AcqUnitsService acqUnitsService;

  /**
   * This class is only used by FiscalYearApi
   */
  public FiscalYearApiService(FiscalYearService fiscalYearService, ConfigurationEntriesService configurationEntriesService,
      BudgetService budgetService, AcqUnitsService acqUnitsService) {
    this.fiscalYearService = fiscalYearService;
    this.configurationEntriesService = configurationEntriesService;
    this.budgetService = budgetService;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<FiscalYear> createFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    log.debug("createFiscalYear:: Creating fiscal year: {}", fiscalYear.getId());
    return configurationEntriesService.getSystemCurrency(requestContext)
      .compose(currency -> {
        fiscalYear.setCurrency(currency);
        return fiscalYearService.createFiscalYear(fiscalYear, requestContext);
      });
  }

  public Future<FiscalYearsCollection> getFiscalYearsWithAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .compose(effectiveQuery -> fiscalYearService.getFiscalYearByQuery(effectiveQuery, offset, limit, requestContext));
  }

  public Future<FiscalYear> getFiscalYearById(String id, boolean withFinancialSummary, RequestContext requestContext) {
    return fiscalYearService.getFiscalYearById(id, requestContext)
      .compose(fiscalYear -> {
        if (withFinancialSummary) {
          return withFinancialSummary(fiscalYear, requestContext);
        }
        return succeededFuture(fiscalYear);
      });
  }

  public Future<Void> updateFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    return configurationEntriesService.getSystemCurrency(requestContext)
      .compose(currency -> {
        fiscalYear.setCurrency(currency);
        return fiscalYearService.updateFiscalYear(fiscalYear, requestContext);
      });
  }

  public Future<Void> deleteFiscalYear(String id, RequestContext requestContext) {
    return fiscalYearService.deleteFiscalYear(id, requestContext);
  }

  private Future<FiscalYear> withFinancialSummary(FiscalYear fiscalYear, RequestContext requestContext) {
    String query = "fiscalYearId==" + fiscalYear.getId();
    return budgetService.getBudgets(query, 0, Integer.MAX_VALUE, requestContext)
      .map(budgetsCollection -> populateFinancialSummary(fiscalYear, budgetsCollection.getBudgets()));
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
