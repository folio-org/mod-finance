package org.folio.services.fund;

import org.apache.commons.lang3.StringUtils;
import org.folio.completablefuture.FolioVertxCompletableFuture;
import org.folio.models.FundCodeExpenseClassesHolder;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.*;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;


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

  public CompletableFuture<FundCodeExpenseClassesCollection> retrieveCombinationFundCodeExpClasses(String fiscalYearCode,
                                                                                                   RequestContext requestContext) {
    FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder = new FundCodeExpenseClassesHolder();
    if (fiscalYearCode != null) {
      FiscalYear fiscalYearUnit = new FiscalYear();
      fiscalYearUnit.setCode(fiscalYearCode);
      return getFundCodeVsExpenseClassesWithFiscalYear(fiscalYearUnit, fundCodeExpenseClassesHolder, requestContext);
    } else {
      return ledgerService.retrieveLedgers(StringUtils.EMPTY, 0, Integer.MAX_VALUE, requestContext)
        // LedgersCollection
        .thenCompose(ledgersCollection -> getFiscalYearList(ledgersCollection.getLedgers(), requestContext))
        .thenCompose(fiscalYearList -> buildCollectionList(fiscalYearList, fundCodeExpenseClassesHolder, requestContext))
        .thenApply(fundCodeExpenseClassesCollectionList -> buildCollection(fundCodeExpenseClassesCollectionList));
    }
  }

  private CompletableFuture<List<FiscalYear>> getFiscalYearList(List<Ledger> ledgerList, RequestContext requestContext) {
    List<CompletableFuture<FiscalYear>> fiscalYearsList = ledgerList.stream()
      .map(ledger -> ledgerDetailsService.getCurrentFiscalYear(ledger.getId(), requestContext))
      .collect(toList());
    return collectResultsOnSuccess(fiscalYearsList);
  }

  private CompletableFuture<List<FundCodeExpenseClassesCollection>> buildCollectionList(List<FiscalYear> fiscalYearList,
                                                                                        FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder, RequestContext requestContext) {
    List<FiscalYear> separatedFiscalYears = new ArrayList<>();
    separatedFiscalYears.add(fiscalYearList.get(0));
    for (FiscalYear fiscalYear : fiscalYearList) {
      for (FiscalYear separatedFiscalYear : separatedFiscalYears) {
        if (!Objects.equals(separatedFiscalYear.getId(), fiscalYear.getId())) {
          separatedFiscalYears.add(fiscalYear);
        }
      }
    }
    List<CompletableFuture<FundCodeExpenseClassesCollection>> completeFutures = separatedFiscalYears.stream()
      .map(fiscalYr -> getFundCodeVsExpenseClassesWithFiscalYear(fiscalYr, fundCodeExpenseClassesHolder, requestContext))
      .collect(Collectors.toList());
    return collectResultsOnSuccess(completeFutures);
  }

  private FundCodeExpenseClassesCollection buildCollection(List<FundCodeExpenseClassesCollection> fundCodeExpenseClassesCollectionList) {
    FundCodeExpenseClassesCollection fundCodeExpenseClassesCollectionObject = new FundCodeExpenseClassesCollection();
    for (FundCodeExpenseClassesCollection fundCodeExpenseClasses : fundCodeExpenseClassesCollectionList) {
      List<FundCodeVsExpClassesType> fundCodeVsExpClassesTypesObject = fundCodeExpenseClassesCollectionObject.getFundCodeVsExpClassesTypes();
      List<FundCodeVsExpClassesType> fundCodeVsExpClassesTypes = fundCodeExpenseClasses.getFundCodeVsExpClassesTypes();
      fundCodeVsExpClassesTypesObject.addAll(fundCodeVsExpClassesTypes);
      fundCodeExpenseClassesCollectionObject.setFundCodeVsExpClassesTypes(fundCodeVsExpClassesTypesObject);
    }
    return fundCodeExpenseClassesCollectionObject;
  }

  private CompletableFuture<FundCodeExpenseClassesCollection> getFundCodeVsExpenseClassesWithFiscalYear(FiscalYear fiscalYearUnit,
                                                                                                        FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder, RequestContext requestContext) {
    String fiscalYearCode = fiscalYearUnit.getCode();
    return fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext)
      .thenCompose(fiscalYear -> getActiveBudgetsByFiscalYear(fiscalYear, requestContext))
      .thenApply(fundCodeExpenseClassesHolder::withBudgetCollectionList)
      .thenApply(holder -> holder.getBudgetCollection().getBudgets().stream().map(budget -> budget.getFundId()).collect(Collectors.toList()))
      .thenCompose(fundsId -> fundService.getFunds(fundsId, requestContext))
      .thenApply(fundCodeExpenseClassesHolder::withFundList)
      .thenApply(holder -> holder.getFundList().stream().map(Fund::getLedgerId).collect(toList()))
      .thenCompose(ledgerIds -> ledgerService.getLedgers(ledgerIds, requestContext))
      .thenApply(fundCodeExpenseClassesHolder::withLedgerList)
      .thenCompose(v -> retrieveFundCodeVsExpenseClasses(fiscalYearCode, requestContext, fundCodeExpenseClassesHolder));
  }

  public CompletableFuture<FundCodeExpenseClassesCollection> retrieveFundCodeVsExpenseClasses(String fiscalYearCode,
                                                                                              RequestContext requestContext, FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder) {
    return fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext)
      // CompletableFuture<FiscalYear>
      .thenCompose(fiscalYear -> getActiveBudgetsByFiscalYear(fiscalYear, requestContext))
      // CompletableFuture<BudgetsCollection>
      .thenApply(budgetsCollection -> budgetsCollection.getBudgets()
        .stream()
        .map(Budget::getId)
        .collect(toList()))
      .thenApply(fundCodeExpenseClassesHolder::withBudgetIds)
      .thenCompose(holder -> budgetExpenseClassService.getBudgetExpensesClass(holder.getBudgetIds(), requestContext))
      .thenApply(fundCodeExpenseClassesHolder::withBudgetExpenseClassList)
      .thenCompose(holder -> expenseClassService.getExpenseClassesByBudgetIds(holder.getBudgetIds(), requestContext))
      .thenApply(fundCodeExpenseClassesHolder::withExpenseClassList)
      .thenCompose(json -> FolioVertxCompletableFuture.supplyBlockingAsync(requestContext.getContext(),
        fundCodeExpenseClassesHolder::buildFundCodeVsExpenseClassesTypeCollection));
  }

  private CompletableFuture<BudgetsCollection> getActiveBudgetsByFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    return budgetService.getBudgets(
      queryBudgetStatusAndFiscalYearId(Budget.BudgetStatus.ACTIVE.value(), fiscalYear.getId()), 0, Integer.MAX_VALUE,
      requestContext);
  }

  public String queryBudgetStatusAndFiscalYearId(String budgetStatus, String fiscalYearId) {
    return String.format("budgetStatus=%s and fiscalYearId=%s", budgetStatus, fiscalYearId);
  }
}
