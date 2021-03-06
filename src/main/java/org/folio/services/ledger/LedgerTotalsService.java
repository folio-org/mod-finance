package org.folio.services.ledger;

import static java.util.stream.Collectors.groupingBy;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.models.LedgerFiscalYearTransactionsHolder;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.HelperUtils;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.transactions.BaseTransactionService;

public class LedgerTotalsService {
  public static final String LEDGER_ID_AND_FISCAL_YEAR_ID = "ledger.id==%s AND fiscalYearId==%s";

  private final FiscalYearService fiscalYearService;
  private final BudgetService budgetService;
  private final BaseTransactionService baseTransactionService;

  public LedgerTotalsService(FiscalYearService fiscalYearService, BudgetService budgetService, BaseTransactionService baseTransactionService) {
    this.fiscalYearService = fiscalYearService;
    this.budgetService = budgetService;
    this.baseTransactionService = baseTransactionService;
  }

  public CompletableFuture<Ledger> populateLedgerTotals(Ledger ledger, String fiscalYearId, RequestContext requestContext) {
    return getFiscalYear(fiscalYearId, requestContext)
      .thenCompose(fiscalYear -> populateLedgerTotals(ledger, fiscalYear, requestContext));
  }

  private CompletableFuture<FiscalYear> getFiscalYear(String fiscalYearId, RequestContext requestContext) {
    return fiscalYearService.getFiscalYearById(fiscalYearId, requestContext)
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
        .thenApply(budgets -> buildHolderSkeleton(fiscalYear.getId(), ledger,  budgets))
        .thenCompose(holder -> updateHolderWithAllocations(holder, requestContext)
                                    .thenAccept(holderParam -> updateLedgerWithAllocation(holder))
                                    .thenCompose(v -> updateHolderWithTransfers(holder, requestContext))
                                    .thenAccept(v -> updateLedgerWithCalculatedFields(holder))
                                    .thenApply(v -> holder.getLedger())
        );
  }

  private void removeInitialAllocationByFunds(LedgerFiscalYearTransactionsHolder holder) {
    Map<String, List<Transaction>> fundToTransactions = holder.getToAllocations().stream().collect(groupingBy(Transaction::getToFundId));
    fundToTransactions.forEach((fundToId, transactions) -> {
      transactions.sort(Comparator.comparing(tr -> tr.getMetadata().getCreatedDate()));
      if (CollectionUtils.isNotEmpty(transactions)) {
        holder.getToAllocations().remove(transactions.get(0));
      }
    });
  }

  private void updateLedgerWithAllocation(LedgerFiscalYearTransactionsHolder holder) {
      removeInitialAllocationByFunds(holder);
      Ledger ledger = holder.getLedger();
      ledger.withAllocationTo(HelperUtils.calculateTotals(holder.getToAllocations(), Transaction::getAmount))
        .withAllocationFrom(HelperUtils.calculateTotals(holder.getFromAllocations(), Transaction::getAmount));
  }

  private CompletableFuture<LedgerFiscalYearTransactionsHolder> updateHolderWithAllocations(LedgerFiscalYearTransactionsHolder holder,
                                                                                            RequestContext requestContext) {
    List<String> ledgerFundIds = holder.getLedgerFundIds();
    String fiscalYearId = holder.getFiscalYearId();
    return baseTransactionService.retrieveToTransactions(ledgerFundIds, fiscalYearId, Transaction.TransactionType.ALLOCATION, requestContext)
      .thenCombine(baseTransactionService.retrieveFromTransactions(ledgerFundIds, fiscalYearId, Transaction.TransactionType.ALLOCATION, requestContext),
        (toAllocations, fromAllocations) -> holder.withToAllocations(toAllocations).withFromAllocations(fromAllocations)
      );
  }

  private CompletableFuture<LedgerFiscalYearTransactionsHolder> updateHolderWithTransfers(LedgerFiscalYearTransactionsHolder holder,
                                                                                          RequestContext requestContext) {
    List<String> ledgerFundIds = holder.getLedgerFundIds();
    String fiscalYearId = holder.getFiscalYearId();
    return baseTransactionService.retrieveToTransactions(ledgerFundIds, fiscalYearId, Transaction.TransactionType.TRANSFER, requestContext)
      .thenCombine(baseTransactionService.retrieveFromTransactions(ledgerFundIds, fiscalYearId, Transaction.TransactionType.TRANSFER, requestContext),
        (toAllocations, fromAllocations) -> holder.withToTransfers(toAllocations).withFromTransfers(fromAllocations)
      );
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

  private LedgerFiscalYearTransactionsHolder buildHolderSkeleton(String fiscalYearId, Ledger ledger, List<Budget> budgets) {
    ledger.withUnavailable(HelperUtils.calculateTotals(budgets, Budget::getUnavailable))
      .withInitialAllocation(HelperUtils.calculateTotals(budgets, Budget::getInitialAllocation))
      .withAwaitingPayment(HelperUtils.calculateTotals(budgets, Budget::getAwaitingPayment))
      .withEncumbered(HelperUtils.calculateTotals(budgets, Budget::getEncumbered))
      .withExpenditures(HelperUtils.calculateTotals(budgets, Budget::getExpenditures));
    return new LedgerFiscalYearTransactionsHolder(fiscalYearId, ledger, budgets);
  }

  //    #allocated = initialAllocation.add(allocationTo).subtract(allocationFrom)
  //    #totalFunding = allocated.add(netTransfers)
  //    #available = totalFunding.subtract(unavailable).max(BigDecimal.ZERO)
  //    #cashBalance = totalFunding.subtract(expended)
  //    #overEncumbered = encumbered.subtract(totalFunding.max(BigDecimal.ZERO)).max(BigDecimal.ZERO)
  //    #overExpended = expended.add(awaitingPayment).subtract(totalFunding.max(BigDecimal.ZERO)).max(BigDecimal.ZERO)
  private void updateLedgerWithCalculatedFields(LedgerFiscalYearTransactionsHolder holder) {
      Ledger ledger = holder.getLedger();
      double toTransfer = HelperUtils.calculateTotals(holder.getToTransfers(), Transaction::getAmount);
      double fromTransfer = HelperUtils.calculateTotals(holder.getFromTransfers(), Transaction::getAmount);
      BigDecimal netTransfers = BigDecimal.valueOf(toTransfer).subtract(BigDecimal.valueOf(fromTransfer));
      ledger.withNetTransfers(netTransfers.doubleValue());

      BigDecimal initialAllocation = BigDecimal.valueOf(ledger.getInitialAllocation());
      BigDecimal allocationTo = BigDecimal.valueOf(ledger.getAllocationTo());
      BigDecimal allocationFrom = BigDecimal.valueOf(ledger.getAllocationFrom());
      BigDecimal allocated = initialAllocation.add(allocationTo).subtract(allocationFrom);
      ledger.withAllocated(allocated.doubleValue());

      BigDecimal totalFunding = allocated.add(netTransfers);
      ledger.withTotalFunding(totalFunding.doubleValue());

      BigDecimal unavailable = BigDecimal.valueOf(ledger.getUnavailable());
      BigDecimal available = totalFunding.subtract(unavailable).max(BigDecimal.ZERO);
      ledger.withAvailable(available.doubleValue());

      BigDecimal expended = BigDecimal.valueOf(ledger.getExpenditures());
      ledger.withCashBalance(totalFunding.subtract(expended).doubleValue());

      BigDecimal encumbered = BigDecimal.valueOf(ledger.getEncumbered());
      BigDecimal overEncumbered = encumbered.subtract(totalFunding.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
      ledger.withOverEncumbrance(overEncumbered.doubleValue());

      BigDecimal awaitingPayment = BigDecimal.valueOf(ledger.getAwaitingPayment());
      BigDecimal overExpended = expended.add(awaitingPayment).subtract(totalFunding.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
      ledger.withOverExpended(overExpended.doubleValue());
  }
}
