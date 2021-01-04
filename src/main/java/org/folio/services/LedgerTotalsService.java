package org.folio.services;

import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.HelperUtils;
import org.folio.services.budget.BudgetService;

public class LedgerTotalsService {

  public static final String LEDGER_ID_AND_FISCAL_YEAR_ID = "ledger.id==%s AND fiscalYearId==%s";

  private final FiscalYearService fiscalYearService;
  private final BudgetService budgetService;

  public LedgerTotalsService(FiscalYearService fiscalYearService, BudgetService budgetService) {
    this.fiscalYearService = fiscalYearService;
    this.budgetService = budgetService;
  }

  public CompletableFuture<Ledger> populateLedgerTotals(Ledger ledger, String fiscalYearId, RequestContext requestContext) {
    return getFiscalYear(fiscalYearId, requestContext)
      .thenCompose(fiscalYear -> populateLedgerTotals(ledger, fiscalYear, requestContext));
  }

  private CompletableFuture<FiscalYear> getFiscalYear(String fiscalYearId, RequestContext requestContext) {
    return fiscalYearService.getFiscalYear(fiscalYearId, requestContext)
      .exceptionally(t -> {
        Throwable cause = t.getCause() == null ? t : t.getCause();
        if (cause instanceof HttpException && ((HttpException) cause).getCode() == 404) {
          throw new HttpException(400, ErrorCodes.FISCAL_YEAR_NOT_FOUND);
        } else {
          throw new CompletionException(t);
        }
      });
  }

  public CompletableFuture<Ledger> populateLedgerTotals(Ledger ledger, FiscalYear fiscalYear, RequestContext requestContext) {
    return getBudgetsByLedgerIdFiscalYearId(ledger.getId(), fiscalYear.getId(), requestContext)
        .thenApply(budgets -> populateLedgerSummary(ledger, budgets));
  }

  public CompletableFuture<LedgersCollection> populateLedgersTotals(LedgersCollection ledgersCollection, String fiscalYearId, RequestContext requestContext) {
    return getFiscalYear(fiscalYearId, requestContext)
      .thenCompose(fiscalYear -> collectResultsOnSuccess(ledgersCollection.getLedgers().stream()
        .map(ledger -> populateLedgerTotals(ledger, fiscalYear, requestContext))
        .collect(Collectors.toList()))
      .thenApply(ledgers -> new LedgersCollection().withLedgers(ledgers).withTotalRecords(ledgers.size())));
  }

  private CompletableFuture<List<Budget>> getBudgetsByLedgerIdFiscalYearId(String ledgerId, String fiscalYearId , RequestContext requestContext) {
    String query = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledgerId, fiscalYearId);
    return budgetService.getBudgets(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(BudgetsCollection::getBudgets);
  }

  private Ledger populateLedgerSummary(Ledger ledger, List<Budget> budgets) {

    return ledger.withAllocated(HelperUtils.calculateTotals(budgets, Budget::getAllocated))
      .withAvailable(HelperUtils.calculateTotals(budgets, Budget::getAvailable))
      .withUnavailable(HelperUtils.calculateTotals(budgets, Budget::getUnavailable))
      .withNetTransfers(HelperUtils.calculateTotals(budgets, Budget::getNetTransfers))
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
  }

}
