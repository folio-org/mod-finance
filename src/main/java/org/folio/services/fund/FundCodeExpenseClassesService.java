package org.folio.services.fund;

import org.apache.commons.lang3.StringUtils;
import org.folio.completablefuture.FolioVertxCompletableFuture;
import org.folio.models.FundCodeExpenseClassesHolder;
import org.folio.rest.acq.model.finance.FundCodeVsExpenseClassesTypeCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundCodeExpenseClassesCollection;
import org.folio.rest.jaxrs.model.FundCodeVsExpClassesType;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import static java.util.stream.Collectors.toList;


public class FundCodeExpenseClassesService {

  private final RestClient budgetExpenseClassRestClient;
  private final BudgetService budgetService;
  private final BudgetExpenseClassService budgetExpenseClassService;
  private final FundService fundService;
  private final LedgerService ledgerService;
  private final FiscalYearService fiscalYearService;
  private final LedgerDetailsService ledgerDetailsService;

  public FundCodeExpenseClassesService(BudgetService budgetService, BudgetExpenseClassService budgetExpenseClassService,
                            RestClient budgetExpenseClassRestClient, FundService fundService, LedgerService ledgerService,
                            FiscalYearService fiscalYearService, LedgerDetailsService ledgerDetailsService) {
    this.budgetService = budgetService;
    this.budgetExpenseClassService = budgetExpenseClassService;
    this.budgetExpenseClassRestClient = budgetExpenseClassRestClient;
    this.fundService = fundService;
    this.ledgerService = ledgerService;
    this.fiscalYearService = fiscalYearService;
    this.ledgerDetailsService = ledgerDetailsService;
  }

  public CompletableFuture<List<FundCodeVsExpClassesType>> retrieveCombinationFundCodeExpClasses(String fiscalYearCode,
                                                                                                 RequestContext requestContext) {
    FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder = new FundCodeExpenseClassesHolder();
    if (fiscalYearCode != null) {
      return getFundCodeVsExpenseClassesWithFiscalYear(fiscalYearCode, fundCodeExpenseClassesHolder, requestContext);
    } else {
      return ledgerService.retrieveLedgers(StringUtils.EMPTY, 0, Integer.MAX_VALUE, requestContext)
        // LedgersCollection
        // get ledgers и для каждого current fiscal year
        .thenApply(ledgersCollection -> ledgersCollection.getLedgers()
          .stream()
          .map(ledger -> ledgerDetailsService.getCurrentFiscalYear(ledger.getId(), requestContext))
          .collect(toList()))
        // List<FiscalYear>
        // .thenCompose(fiscalYears -> fiscalYears.stream().map(fiscalYear -> getFundCodeVsExpenseClassesWithFiscalYear(fiscalYear,
        // requestContext, fundCodeExpenseClassesHolder)));
        // .thenCompose(fiscalYears -> getFundCodeVsExpenseClassesTypeCollection(fiscalYears, requestContext,
        // fundCodeExpenseClassesHolder));

        // .thenCompose(json -> FolioVertxCompletableFuture.supplyBlockingAsync(requestContext.getContext(), () ->
        // getFundCodeVsExpenseClassesWithFiscalYear(fiscalYear, requestContext, fundCodeExpenseClassesHolder) ));
        // .thenCompose(json -> FolioVertxCompletableFuture.supplyBlockingAsync(requestContext.getContext(), () ->
        // getFundCodeVsExpenseClassesTypeCollection(json, requestContext)));
        // .thenCompose(fiscalYear -> getFundCodeVsExpenseClassesTypeCollection(fiscalYear, requestContext));

        .thenApply(v -> new ArrayList<>());
    }
  }

  public String queryBudgetStatusAndFiscalYearId(String budgetStatus, String fiscalYearId) {
    return String.format("budgetStatus=%s and fiscalYearId=%s", budgetStatus, fiscalYearId);
  }

  /*
   * public CompletableFuture<FundCodeVsExpenseClassesTypeCollection> getFundCodeVsExpenseClassesTypeCollection(List<FiscalYear>
   * fiscalYears, RequestContext requestContext, FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder) { //
   * FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder List<String> fiscalYearCodeList = new ArrayList<>(); for (FiscalYear
   * fiscalYear : fiscalYears) { fiscalYearCodeList.add(fiscalYear.getCode()); } return
   * collectResultsOnSuccess(ofSubLists(fiscalYearCodeList, MAX_IDS_FOR_GET_RQ) .map(fiscalYearCode -> fiscalYearCode.stream()
   * .map(fiscalYear -> retrieveFundCodeVsExpenseClasses(fiscalYear, requestContext, fundCodeExpenseClassesHolder))
   * .collect(toList())); //.map(ids1 -> getRestrictedLedgersChunk(fiscalYearCodeList, requestContext)).toList()) .thenApply(lists
   * -> lists.stream() .flatMap(Collection::stream) .collect(toList())); }
   */

  /*
   * public CompletableFuture<List<org.folio.rest.acq.model.finance.Ledger>> getRestrictedLedgersChunk(List<String> ids,
   * RequestContext requestContext) {
   *
   * String query = convertIdsToCqlQuery(ids) + " AND restrictExpenditures==true"; RequestEntry requestEntry = new
   * RequestEntry(LEDGERS_ENDPOINT) .withQuery(query) .withOffset(0) .withLimit(MAX_IDS_FOR_GET_RQ); return
   * restClient.get(requestEntry, requestContext, LedgerCollection.class) .thenApply(LedgerCollection::getLedgers); }
   */

  public CompletableFuture<List<FundCodeVsExpClassesType>> retrieveFundCodeVsExpenseClasses(String fiscalYearCode,
                                                                                            RequestContext requestContext, FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder) {
    return fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext)
      // CompletableFuture<FiscalYear>
      .thenCompose(fiscalYear -> getActiveBudgets(fiscalYear, requestContext))
      // CompletableFuture<BudgetsCollection>
      .thenApply(budgetsCollection -> budgetsCollection.getBudgets()
        .stream()
        .map(budget -> budget.getId())
        .collect(toList()))
      .thenApply(budgetIds -> budgetExpenseClassService.getBudgetExpensesClassByIds(budgetIds, requestContext)
        .thenApply(fundCodeExpenseClassesHolder::withBudgetExpenseClassList))
      .thenCompose(json -> FolioVertxCompletableFuture.supplyBlockingAsync(requestContext.getContext(),
        fundCodeExpenseClassesHolder::getFundCodeVsExpenseClassesTypeList));
  }

  private CompletableFuture<List<FundCodeVsExpClassesType>> getFundCodeVsExpenseClassesWithFiscalYear(String fiscalYearCode,
                                                                                                      FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder, RequestContext requestContext) {
    return fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext)
      .thenCompose(fiscalYear -> getActiveBudgets(fiscalYear, requestContext))
      .thenApply(fundCodeExpenseClassesHolder::withBudgetCollectionList)
      .thenApply(holder -> holder.getFundList().stream().map(Fund::getId).collect(toList()))
      .thenCompose(fundsId -> fundService.getFunds(fundsId, requestContext))
      .thenApply(fundCodeExpenseClassesHolder::withFundList)
      .thenApply(holder -> holder.getFundList().stream().map(Fund::getLedgerId).collect(toList()))
      .thenApply(ledgerId -> ledgerService.getLedgers(ledgerId, requestContext))
      .thenCompose(v -> retrieveFundCodeVsExpenseClasses(fiscalYearCode, requestContext, fundCodeExpenseClassesHolder));
  }

  private CompletableFuture<BudgetsCollection> getActiveBudgets(FiscalYear fiscalYear, RequestContext requestContext) {
    return budgetService.getBudgets(
      queryBudgetStatusAndFiscalYearId(Budget.BudgetStatus.ACTIVE.value(), fiscalYear.getId()), 0, Integer.MAX_VALUE,
      requestContext);
  }


  private void buildFundCodeVsExpenseClassesType(FundCodeExpenseClassesHolder holder) {
    Map<String, Ledger> ledgerIdVsLedgerMap = new HashMap<>();
    FundCodeVsExpClassesType fundCodeVsExpenseClassesType = new FundCodeVsExpClassesType();
    List<Ledger> ledgerList = holder.getLedgerList();
    for (Ledger ledger : ledgerList) {
      ledgerIdVsLedgerMap.put(ledger.getId(), ledger);
    }
    List<Fund> fundList = holder.getFundList();
    for (Ledger ledger : ledgerList) {
      for (Fund fund : fundList) {
        if (Objects.equals(ledger.getId(), fund.getLedgerId())) {
          fundCodeVsExpenseClassesType.setFundCode(fund.getCode());
          fundCodeVsExpenseClassesType.setLedgerCode(ledgerIdVsLedgerMap.get(fund.getLedgerId()).getCode());
        //  fundCodeVsExpenseClassesType.setActiveFundCodeVsExpClasses(holder.get(fund));
      //    fundCodeVsExpenseClassesType.setInactiveFundCodeVsExpClasses(getInActiveStatusBudgetExpenseClass(fund));
          holder.addFundCodeVsExpenseClassesType(fundCodeVsExpenseClassesType);
        }
      }
    }
  }

  private List<String> getActiveStatusBudgetExpenseClass(Fund fund, FundCodeExpenseClassesHolder holder) {
    List<BudgetExpenseClass> budgetExpenseClassList = holder.getBudgetExpenseClassList();
    List<Budget> budgetList = holder.getBudgetCollection().getBudgets();
    List<String> activeStatus = new ArrayList<>();
    List<Budget> budgetListByFundId = new ArrayList<>();
    for (Budget budget : budgetList) {
      if (budget.getFundId() == fund.getId()) {
        budgetListByFundId.add(budget);
      }
    }
    for (Budget budget : budgetListByFundId) {
      for (BudgetExpenseClass budgetExpenseClass : budgetExpenseClassList) {
        if (Objects.equals(budget.getId(), budgetExpenseClass.getBudgetId())) {
          if (budgetExpenseClass.getStatus().equals(BudgetExpenseClass.Status.fromValue("Active"))) {
            activeStatus.add(budgetExpenseClass.getStatus().toString());
          }
        }
      }
    }
    return activeStatus;
  }

  private List<String> getInActiveStatusBudgetExpenseClass(Fund fund, FundCodeExpenseClassesHolder holder) {
    List<BudgetExpenseClass> budgetExpenseClassList = holder.getBudgetExpenseClassList();
    List<Budget> budgetList = holder.getBudgetCollection().getBudgets();
    List<String> inActiveStatus = new ArrayList<>();
    List<Budget> budgetListByFundId = new ArrayList<>();
    for (Budget budget : budgetList) {
      if (budget.getFundId().equals(fund.getId())) {
        budgetListByFundId.add(budget);
      }
    }
    for (Budget budget : budgetListByFundId) {
      for (BudgetExpenseClass budgetExpenseClass : budgetExpenseClassList) {
        if (Objects.equals(budget.getId(), budgetExpenseClass.getBudgetId())) {
          if (budgetExpenseClass.getStatus().equals(BudgetExpenseClass.Status.fromValue("Inactive"))) {
            inActiveStatus.add(budgetExpenseClass.getStatus().toString());
          }
        }
      }
    }
    return inActiveStatus;
  }

  private FundCodeExpenseClassesCollection getFundCodeVsExpenseClassesTypeCollection(FundCodeExpenseClassesHolder holder) {
    FundCodeExpenseClassesCollection fundCodeVsExpenseClassesTypeCollection = new FundCodeExpenseClassesCollection();
    List<FundCodeVsExpClassesType> fundCodeVsExpenseClassesTypeList = holder.getFundCodeVsExpenseClassesTypeList();
    fundCodeVsExpenseClassesTypeCollection.setDelimiter(":");
    fundCodeVsExpenseClassesTypeCollection.setFundCodeVsExpClassesTypes(fundCodeVsExpenseClassesTypeList);
    return fundCodeVsExpenseClassesTypeCollection;
  }
}
