package org.folio.services.fund;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.completablefuture.FolioVertxCompletableFuture;
import org.folio.rest.acq.model.finance.FundCodeVsExpenseClassesTypeCollection;
import org.folio.rest.acq.model.finance.LedgerCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.impl.FundCodeExpenseClassesHolder;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.jaxrs.model.BudgetExpenseClass.Status;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.ledger.LedgerService;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.ErrorCodes.CURRENT_BUDGET_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;

public class FundDetailsService {
  private static final Logger logger = LogManager.getLogger(FundDetailsService.class);
  private static final String CURRENT_BUDGET_QUERY_WITH_STATUS = "fundId==%s and fiscalYearId==%s and budgetStatus==%s";
  private static final String CURRENT_BUDGET_QUERY = "fundId==%s and fiscalYearId==%s";

  private final BudgetService budgetService;
  private final ExpenseClassService expenseClassService;
  private final BudgetExpenseClassService budgetExpenseClassService;
  private final FundFiscalYearService fundFiscalYearService;
  private final RestClient budgetExpenseClassRestClient;
  private final FundService fundService;
  private final LedgerService ledgerService;
  private final FiscalYearService fiscalYearService;

  public FundDetailsService(BudgetService budgetService, ExpenseClassService expenseClassService, BudgetExpenseClassService budgetExpenseClassService,
                            FundFiscalYearService fundFiscalYearService, RestClient budgetExpenseClassRestClient, FundService fundService,
                            LedgerService ledgerService, FiscalYearService fiscalYearService) {
    this.budgetService = budgetService;
    this.expenseClassService = expenseClassService;
    this.budgetExpenseClassService = budgetExpenseClassService;
    this.fundFiscalYearService = fundFiscalYearService;
    this.budgetExpenseClassRestClient = budgetExpenseClassRestClient;
    this.fundService = fundService;
    this.ledgerService = ledgerService;
    this.fiscalYearService = fiscalYearService;
  }

  public CompletableFuture<Budget> retrieveCurrentBudget(String fundId, String budgetStatus, boolean skipThrowException, RequestContext rqContext) {
    CompletableFuture<Budget> future = new CompletableFuture<>();
    retrieveCurrentBudget(fundId, budgetStatus, rqContext)
      .thenApply(future::complete)
      .exceptionally(t -> {
        logger.error("Failed to retrieve current budget", t.getCause());
        if (skipThrowException) {
          future.complete(null);
        } else {
          future.completeExceptionally(t.getCause());
        }
        return null;
      });
    return future;
  }

  public CompletableFuture<Budget> retrieveCurrentBudget(String fundId, String budgetStatus, RequestContext rqContext) {
    return fundFiscalYearService.retrieveCurrentFiscalYear(fundId, rqContext)
      .thenApply(fundCurrFY -> buildCurrentBudgetQuery(fundId, budgetStatus, fundCurrFY.getId()))
      .thenCompose(activeBudgetQuery -> budgetService.getBudgets(activeBudgetQuery, 0, Integer.MAX_VALUE, rqContext))
      .thenApply(this::getFirstBudget);
  }

  public CompletableFuture<List<ExpenseClass>> retrieveCurrentExpenseClasses(String fundId, String status, RequestContext rqContext) {
    CompletableFuture<List<ExpenseClass>> future = new FolioVertxCompletableFuture<>(rqContext.getContext());
    retrieveCurrentBudget(fundId, null, rqContext)
      .thenCompose(currentBudget -> {
        logger.debug("Is Current budget for fund found : " + currentBudget.getId());
        return retrieveBudgetExpenseClasses(currentBudget, rqContext)
          .thenCombine(getBudgetExpenseClassIds(currentBudget.getId(), status, rqContext), (expenseClasses, budgetExpenseClassIds) ->
            expenseClasses.stream()
              .filter(expenseClass -> budgetExpenseClassIds.contains(expenseClass.getId()))
              .collect(Collectors.toList()));

      })
      .thenAccept(expenseClasses -> {
        logger.debug("Expense classes for fund size : " + expenseClasses.size());
        future.complete(expenseClasses);
      })
      .exceptionally(t -> {
        logger.error("Retrieve current expense classes for fund failed", t.getCause());
        future.completeExceptionally(t);
        return null;
      });
    return future;
  }

  private CompletableFuture<List<ExpenseClass>> retrieveBudgetExpenseClasses(Budget budget, RequestContext rqContext) {
    return Optional.ofNullable(budget)
      .map(budgetP -> expenseClassService.getExpenseClassesByBudgetId(budgetP.getId(), rqContext))
      .orElse(CompletableFuture.completedFuture(Collections.emptyList()));
  }

  private Budget getFirstBudget(BudgetsCollection budgetsCollection) {
    return Optional.ofNullable(budgetsCollection)
      .filter(budgetsCol -> !CollectionUtils.isEmpty(budgetsCol.getBudgets()))
      .map(BudgetsCollection::getBudgets)
      .map(budgets -> budgets.get(0))
      .orElseThrow(() -> new HttpException(404, CURRENT_BUDGET_NOT_FOUND.toError()));
  }

  private CompletableFuture<List<String>> getBudgetExpenseClassIds(String budgetId, String status, RequestContext rqContext){
    return budgetExpenseClassService.getBudgetExpenseClasses(budgetId, rqContext)
      .thenApply(expenseClasses ->
        expenseClasses.stream()
          .filter(budgetExpenseClass -> isBudgetExpenseClassWithStatus(budgetExpenseClass, status))
          .map(BudgetExpenseClass::getExpenseClassId)
          .collect(Collectors.toList())
      );
  }

  private String buildCurrentBudgetQuery(String fundId, String budgetStatus, String fundCurrFYId) {
    return StringUtils.isEmpty(budgetStatus) ? String.format(CURRENT_BUDGET_QUERY, fundId, fundCurrFYId)
      : String.format(CURRENT_BUDGET_QUERY_WITH_STATUS, fundId, fundCurrFYId, Budget.BudgetStatus.fromValue(budgetStatus).value());
  }

  private boolean isBudgetExpenseClassWithStatus(BudgetExpenseClass budgetExpenseClass, String status) {
    if (Objects.nonNull(status)) {
      return budgetExpenseClass.getStatus().equals(Status.fromValue(status));
    }
    return true;
  }

  public CompletableFuture<FundCodeVsExpenseClassesTypeCollection> retrieveCombinationFundCodeExpClasses(String fiscalYearCode, RequestContext requestContext) {
    FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder = new FundCodeExpenseClassesHolder();
    if (fiscalYearCode != null) {
      return getFundCodeVsExpenseClassesWithFiscalYear(fiscalYearCode, requestContext, fundCodeExpenseClassesHolder);
  } else {
    String queryRetrieveAllLedgers = "";
    return ledgerService.retrieveLedgers(queryRetrieveAllLedgers, 0, Integer.MAX_VALUE, requestContext)
      // LedgersCollection
      // достать все ledgers и для каждого current fiscal year
      .thenApply(ledgersCollection -> ledgersCollection.getLedgers().stream()
        .map(ledger -> fundFiscalYearService.getCurrentFiscalYearByLedgerId(ledger.getId(), requestContext))
        .collect(Collectors.toList()))
      // List<FiscalYear>
      //.thenCompose(fiscalYear -> getFundCodeVsExpenseClassesWithFiscalYear())
      //.thenCompose((json -> FolioVertxCompletableFuture.supplyBlockingAsync(requestContext.getContext(), () -> fundCodeExpenseClassesHolder::setFiscalYearList ));
      .thenCompose(json -> FolioVertxCompletableFuture.supplyBlockingAsync(requestContext.getContext(), () -> getFundCodeVsExpenseClassesTypeCollection(json, requestContext)));
      //.thenCompose(fiscalYear -> getFundCodeVsExpenseClassesTypeCollection(fiscalYear, requestContext));
      //(json -> FolioVertxCompletableFuture.supplyBlockingAsync(requestContext.getContext(), () -> fundCodeExpenseClassesHolder.getFundCodeVsExpenseClassesTypeCollection()));
      //.thenApply(fiscalYears -> fiscalYears.stream().forEach(f -> getFundCodeVsExpenseClassesTypeCollection(f, requestContext, fundCodeExpenseClassesHolder)));
      //.thenApply(v -> new FundCodeVsExpenseClassesTypeCollection());
  }
  }

  public String queryBudgetStatusAndFiscalYearId(String budgetStatus, String fiscalYearId) {
    return String.format("budgetStatus=%s and fiscalYearId=%s", budgetStatus, fiscalYearId);
  }

  public CompletableFuture<FundCodeVsExpenseClassesTypeCollection> getFundCodeVsExpenseClassesTypeCollection(List<FiscalYear> fiscalYears,
                                                                               RequestContext requestContext,
                                                                               FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder) {
          // FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder
    List<String> fiscalYearCodeList = new ArrayList<>();
    for (FiscalYear fiscalYear : fiscalYears) {
      fiscalYearCodeList.add(fiscalYear.getCode());
    }
    return collectResultsOnSuccess(ofSubLists(fiscalYearCodeList, MAX_IDS_FOR_GET_RQ)
      .map(fiscalYearCode -> fiscalYearCode.stream()
        .map(fiscalYear -> retrieveFundCodeVsExpenseClasses(fiscalYear, requestContext, fundCodeExpenseClassesHolder))
      //.map(ids1 -> getRestrictedLedgersChunk(fiscalYearCodeList, requestContext)).toList())
      .thenApply(lists -> lists.stream()
        .flatMap(Collection::stream)
        .collect(toList()));
  }

  /*public CompletableFuture<List<org.folio.rest.acq.model.finance.Ledger>> getRestrictedLedgersChunk(List<String> ids, RequestContext requestContext) {

    String query = convertIdsToCqlQuery(ids) + " AND restrictExpenditures==true";
    RequestEntry requestEntry = new RequestEntry(LEDGERS_ENDPOINT)
      .withQuery(query)
      .withOffset(0)
      .withLimit(MAX_IDS_FOR_GET_RQ);
    return restClient.get(requestEntry, requestContext, LedgerCollection.class)
      .thenApply(LedgerCollection::getLedgers);
  } */

  public CompletableFuture<FundCodeVsExpenseClassesTypeCollection> getFundCodeVsExpenseClassesWithFiscalYear(String fiscalYearCode,
                                                                                                             RequestContext requestContext,
                                                                                                             FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder) {
    return fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext)
      // CompletableFuture<FiscalYear>
      .thenCompose(fiscalYear -> budgetService.getBudgets(queryBudgetStatusAndFiscalYearId(Budget.BudgetStatus.ACTIVE.value(),
        fiscalYear.getId()), 0, Integer.MAX_VALUE, requestContext))
      // CompletableFuture<BudgetsCollection>
      .thenApply(fundCodeExpenseClassesHolder::setBudgetCollectionList)
      .thenApply(budgetsCollection -> budgetsCollection.getBudgets().stream().map(budget -> budget.getFundId())
        .collect(Collectors.toList()))
      .thenApply(fundsId -> fundService.getFundsByIds(fundsId, requestContext)
        .thenApply(fundCodeExpenseClassesHolder::setFundList)
        // CompletableFuture<List<Fund>>
        .thenApply(funds -> funds.stream().map(fund -> fund.getLedgerId()).collect(Collectors.toList()))
        // CompletableFuture<List<String>> ledgerId
        .thenApply(ledgerId -> ledgerService.getLedgersByIds(ledgerId, requestContext)
          .thenApply(fundCodeExpenseClassesHolder::setLedgerList)))
      .thenCompose(v -> retrieveFundCodeVsExpenseClasses(fiscalYearCode, requestContext, fundCodeExpenseClassesHolder));
  }

  public CompletableFuture<FundCodeVsExpenseClassesTypeCollection> retrieveFundCodeVsExpenseClasses(String fiscalYearCode,
                                                                                                    RequestContext requestContext,
                                                                                                    FundCodeExpenseClassesHolder fundCodeExpenseClassesHolder) {
    return fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext)
      // CompletableFuture<FiscalYear>
      .thenCompose(fiscalYear -> budgetService.getBudgets(queryBudgetStatusAndFiscalYearId(Budget.BudgetStatus.ACTIVE.value(),
        fiscalYear.getId()), 0, Integer.MAX_VALUE, requestContext))
      // CompletableFuture<BudgetsCollection>
      .thenApply(budgetsCollection -> budgetsCollection.getBudgets().stream()
        .map(budget -> budget.getId()).collect(Collectors.toList()))
      .thenApply(budgetIds -> budgetExpenseClassService.getBudgetExpensesClassByIds(budgetIds, requestContext)
          .thenApply(fundCodeExpenseClassesHolder::setBudgetExpenseClassList))
      .thenCompose(json -> FolioVertxCompletableFuture.supplyBlockingAsync(requestContext.getContext(), () -> fundCodeExpenseClassesHolder.getFundCodeVsExpenseClassesTypeCollection()));
      //.thenApply(fundCodeExpenseClassesHolder::getFundCodeVsExpenseClassesTypeCollection);
        //  .thenApply(fundCodeExpenseClassesHolder::getFundCodeVsExpenseClassesType);
        //.thenApply(v -> new FundCodeVsExpenseClassesTypeCollection());
  }
}
