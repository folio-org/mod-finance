package org.folio.services.fund;

import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.folio.models.FundCodeExpenseClassesHolder;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundCodeExpenseClassesCollection;
import org.folio.rest.jaxrs.model.FundCodeVsExpClassesType;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerService;

import io.vertx.core.Future;


public class FundCodeExpenseClassesService {

  private final BudgetService budgetService;
  private final BudgetExpenseClassService budgetExpenseClassService;
  private final FundService fundService;
  private final LedgerService ledgerService;
  private final FiscalYearService fiscalYearService;
  private final LedgerDetailsService ledgerDetailsService;
  private final ExpenseClassService expenseClassService;

  public FundCodeExpenseClassesService(BudgetService budgetService, BudgetExpenseClassService budgetExpenseClassService,
                                       FundService fundService, LedgerService ledgerService,
                                       FiscalYearService fiscalYearService, LedgerDetailsService ledgerDetailsService,
                                       ExpenseClassService expenseClassService) {
    this.budgetService = budgetService;
    this.budgetExpenseClassService = budgetExpenseClassService;
    this.fundService = fundService;
    this.ledgerService = ledgerService;
    this.fiscalYearService = fiscalYearService;
    this.ledgerDetailsService = ledgerDetailsService;
    this.expenseClassService = expenseClassService;
  }

  public Future<FundCodeExpenseClassesCollection> retrieveCombinationFundCodeExpClasses(String fiscalYearCode,
                                                                                        RequestContext requestContext) {
    FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder = new FundCodeExpenseClassesHolder();
    if (fiscalYearCode != null) {
      FiscalYear fiscalYearUnit = new FiscalYear();
      fiscalYearUnit.setCode(fiscalYearCode);
      return getFundCodeVsExpenseClassesWithFiscalYear(fiscalYearUnit, fundCodeExpenseClassesHolder, requestContext);
    } else {
      return ledgerService.retrieveLedgers(StringUtils.EMPTY, 0, Integer.MAX_VALUE, requestContext)
        // LedgersCollection
        .compose(ledgersCollection -> getFiscalYearList(ledgersCollection.getLedgers(), requestContext))
        .compose(fiscalYearList -> buildFundCodeExpenseClassesCollection(fiscalYearList, fundCodeExpenseClassesHolder, requestContext))
        .map(this::buildCollection);
    }
  }

  public Future<List<FiscalYear>> getFiscalYearList(List<Ledger> ledgerList, RequestContext requestContext) {
    List<Future<FiscalYear>> fiscalYearsList = ledgerList.stream()
      .map(ledger -> ledgerDetailsService.getCurrentFiscalYear(ledger.getId(), requestContext))
      .collect(Collectors.toList());
    return collectResultsOnSuccess(fiscalYearsList);
  }

  public Future<List<FundCodeExpenseClassesCollection>> buildFundCodeExpenseClassesCollection(List<FiscalYear> fiscalYearList,
                                                                                              FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder,
                                                                                              RequestContext requestContext) {
    List<FiscalYear> separatedFiscalYears = fiscalYearList.stream().distinct().collect(Collectors.toList());
    List<Future<FundCodeExpenseClassesCollection>> completeFutures = separatedFiscalYears.stream()
      .map(fiscalYr -> getFundCodeVsExpenseClassesWithFiscalYear(fiscalYr, fundCodeExpenseClassesHolder, requestContext))
      .collect(Collectors.toList());
    return collectResultsOnSuccess(completeFutures);
  }

  public FundCodeExpenseClassesCollection buildCollection(List<FundCodeExpenseClassesCollection> fundCodeExpenseClassesCollectionList) {
    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollectionObject = new FundCodeExpenseClassesCollection();
    fundCodeExpenseClassesCollectionObject.setDelimiter(":");
    for (FundCodeExpenseClassesCollection fundCodeExpenseClasses : fundCodeExpenseClassesCollectionList) {
      List<FundCodeVsExpClassesType> fundCodeVsExpClassesTypesObject = fundCodeExpenseClassesCollectionObject.getFundCodeVsExpClassesTypes();
      List<FundCodeVsExpClassesType> fundCodeVsExpClassesTypes = fundCodeExpenseClasses.getFundCodeVsExpClassesTypes();
      fundCodeVsExpClassesTypesObject.addAll(fundCodeVsExpClassesTypes);
      fundCodeExpenseClassesCollectionObject.setFundCodeVsExpClassesTypes(fundCodeVsExpClassesTypesObject);
    }
    return fundCodeExpenseClassesCollectionObject;
  }

  public Future<FundCodeExpenseClassesCollection> getFundCodeVsExpenseClassesWithFiscalYear(FiscalYear fiscalYearUnit,
                                                                                            FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder,
                                                                                            RequestContext requestContext) {
    String fiscalYearCode = fiscalYearUnit.getCode();
    return fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext)
      .map(fundCodeExpenseClassesHolder::setFiscalYear)
      .compose(fcecHolder -> getActiveBudgetsByFiscalYear(fcecHolder.getFiscalYear(), requestContext))
      .map(fundCodeExpenseClassesHolder::withBudgetCollectionList)
      .map(holder -> holder.getBudgetCollection().getBudgets().stream().map(Budget::getFundId).distinct().collect(Collectors.toList()))
      .compose(fundsId -> fundService.getFunds(fundsId, requestContext))
      .map(fundCodeExpenseClassesHolder::withFundList)
      .map(fcecHolder -> fcecHolder.getFundList().stream().map(Fund::getLedgerId).collect(Collectors.toList()))
      .compose(ledgerIds -> ledgerService.getLedgers(ledgerIds, requestContext))
      .map(fundCodeExpenseClassesHolder::withLedgerList)
      .compose(fcecHolder -> retrieveFundCodeVsExpenseClasses(requestContext, fundCodeExpenseClassesHolder));
  }

  public Future<FundCodeExpenseClassesCollection> retrieveFundCodeVsExpenseClasses(RequestContext requestContext,
                                                                                   FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder) {
    return getActiveBudgetsByFiscalYear(fundCodeExpenseClassesHolder.getFiscalYear(), requestContext)
      // Future<BudgetsCollection>
      .map(budgetsCollection -> budgetsCollection.getBudgets()
        .stream()
        .map(Budget::getId)
        .collect(Collectors.toList()))
      .map(fundCodeExpenseClassesHolder::withBudgetIds)
      .compose(holder -> budgetExpenseClassService.getBudgetExpenseClasses(holder.getBudgetIds(), requestContext))
      .map(fundCodeExpenseClassesHolder::withBudgetExpenseClassList)
      .compose(holder -> expenseClassService.getExpenseClassesByBudgetIds(holder.getBudgetIds(), requestContext))
      .map(fundCodeExpenseClassesHolder::withExpenseClassList)
      .map(FundCodeExpenseClassesHolder::buildFundCodeVsExpenseClassesTypeCollection);
  }

  public Future<BudgetsCollection> getActiveBudgetsByFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    return budgetService.getBudgets(queryBudgetStatusAndFiscalYearId(Budget.BudgetStatus.ACTIVE.value(),
       fiscalYear.getId()), 0, Integer.MAX_VALUE, requestContext);
  }

  public String queryBudgetStatusAndFiscalYearId(String budgetStatus, String fiscalYearId) {
    return String.format("budgetStatus=%s and fiscalYearId=%s", budgetStatus, fiscalYearId);
  }
}
