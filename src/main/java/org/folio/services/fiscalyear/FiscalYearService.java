package org.folio.services.fiscalyear;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.FinancialSummary;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.rest.util.HelperUtils;
import org.folio.services.budget.BudgetService;
import org.folio.services.configuration.ConfigurationEntriesService;
import org.folio.services.protection.AcqUnitsService;

import io.vertx.core.Future;

public class FiscalYearService {

  private static final Logger log = LogManager.getLogger();

  private final RestClient restClient;
  private final ConfigurationEntriesService configurationEntriesService;
  private final BudgetService budgetService;
  private final AcqUnitsService acqUnitsService;

  public FiscalYearService(RestClient fiscalYearRestClient, ConfigurationEntriesService configurationEntriesService, BudgetService budgetService, AcqUnitsService acqUnitsService) {
    this.restClient = fiscalYearRestClient;
    this.configurationEntriesService = configurationEntriesService;
    this.budgetService = budgetService;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<FiscalYear> createFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    log.debug("createFiscalYear:: Creating fiscal year: {}", fiscalYear.getId());
    return configurationEntriesService.getSystemCurrency(requestContext)
      .compose(currency -> {
        fiscalYear.setCurrency(currency);
        return restClient.post(resourcesPath(FISCAL_YEARS_STORAGE), fiscalYear, FiscalYear.class, requestContext);
      });
  }

  public Future<FiscalYearsCollection> getFiscalYearsWithoutAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FISCAL_YEARS_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FiscalYearsCollection.class, requestContext);
  }

  public Future<FiscalYearsCollection> getFiscalYearsWithAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .map(effectiveQuery -> new RequestEntry(resourcesPath(FISCAL_YEARS_STORAGE))
        .withOffset(offset)
        .withLimit(limit)
        .withQuery(effectiveQuery)
      )
      .compose(requestEntry -> restClient.get(requestEntry.buildEndpoint(), FiscalYearsCollection.class, requestContext));
  }

  public Future<FiscalYear> getFiscalYearById(String id, boolean withFinancialSummary, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(FISCAL_YEARS_STORAGE, id), FiscalYear.class, requestContext)
      .compose(fiscalYear -> {
        if (withFinancialSummary) {
          return withFinancialSummary(fiscalYear, requestContext);
        }
        return succeededFuture(fiscalYear);
      });
  }

  public Future<FiscalYear> getFiscalYearByFiscalYearCode(String fiscalYearCode, RequestContext requestContext) {
    String query = getFiscalYearByFiscalYearCode(fiscalYearCode);
    var requestEntry = new RequestEntry(resourcesPath(FISCAL_YEARS_STORAGE))
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FiscalYearsCollection.class, requestContext)
      .map(collection -> {
        if (CollectionUtils.isNotEmpty(collection.getFiscalYears())) {
          return collection.getFiscalYears().get(0);
        }
        log.error("getFiscalYearByFiscalYearCode:: Error to get fiscal year by fiscal year code: {}", fiscalYearCode);
        throw new HttpException(400, FISCAL_YEARS_NOT_FOUND);
      });
  }

  public String getFiscalYearByFiscalYearCode(String fiscalYearCode) {
    return String.format("code=%s", fiscalYearCode);
  }

  public Future<FiscalYear> getFiscalYearById(String id, RequestContext requestContext) {
    return getFiscalYearById(id, false, requestContext);
  }

  public Future<Void> updateFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    return configurationEntriesService.getSystemCurrency(requestContext)
      .compose(currency -> {
        fiscalYear.setCurrency(currency);
        return restClient.put(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYear.getId()), fiscalYear, requestContext);
      });

  }

  public Future<Void> deleteFiscalYear(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(FISCAL_YEARS_STORAGE, id), requestContext);
  }


  public Future<FiscalYear> withFinancialSummary(FiscalYear fiscalYear, RequestContext requestContext) {
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
