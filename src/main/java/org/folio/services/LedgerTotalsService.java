package org.folio.services;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.util.ErrorCodes;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;

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
        .thenApply(budgets -> populateLedgerSummary(ledger, budgets, fiscalYear.getCurrency()));
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

  private Ledger populateLedgerSummary(Ledger ledger, List<Budget> budgets, String currencyCode) {
    CurrencyUnit currency = Monetary.getCurrency(currencyCode);

    double allocatedTotal = calculateBudgetTotals(budgets, currency, Budget::getAllocated);
    double availableTotal = calculateBudgetTotals(budgets, currency, Budget::getAvailable);
    double unavailableTotal = calculateBudgetTotals(budgets, currency, Budget::getUnavailable);
    double netTransfersTotal = calculateBudgetTotals(budgets, currency, Budget::getNetTransfers);

    return ledger.withAllocated(allocatedTotal)
      .withAvailable(availableTotal)
      .withUnavailable(unavailableTotal)
      .withNetTransfers(netTransfersTotal);
  }

  private double calculateBudgetTotals(List<Budget> budgets, CurrencyUnit currency, ToDoubleFunction<Budget> getBudgetTotal) {
    return budgets.stream()
      .map(budget -> (MonetaryAmount) Money.of(getBudgetTotal.applyAsDouble(budget), currency))
      .reduce(MonetaryFunctions.sum()).orElse(Money.zero(currency))
      .getNumber().doubleValue();
  }

}
