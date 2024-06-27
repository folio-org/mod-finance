package org.folio.services.ledger;

import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.removeInitialAllocationByFunds;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.models.LedgerFiscalYearTransactionsHolder;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.TransactionType;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.HelperUtils;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;

import io.vertx.core.Future;
import org.folio.services.transactions.TransactionService;

public class LedgerTotalsService {

  private static final Logger log = LogManager.getLogger();
  public static final String LEDGER_ID_AND_FISCAL_YEAR_ID = "ledger.id==%s AND fiscalYearId==%s";

  private final FiscalYearService fiscalYearService;
  private final BudgetService budgetService;
  private final TransactionService transactionService;

  public LedgerTotalsService(FiscalYearService fiscalYearService, BudgetService budgetService, TransactionService transactionService) {
    this.fiscalYearService = fiscalYearService;
    this.budgetService = budgetService;
    this.transactionService = transactionService;
  }

  public Future<Ledger> populateLedgerTotals(Ledger ledger, String fiscalYearId, RequestContext requestContext) {
    return getFiscalYear(fiscalYearId, requestContext)
      .compose(fiscalYear -> populateLedgerTotals(ledger, fiscalYear, requestContext));
  }

  private Future<FiscalYear> getFiscalYear(String fiscalYearId, RequestContext requestContext) {
    return fiscalYearService.getFiscalYearById(fiscalYearId, requestContext)
      .recover(t -> {
        log.error("Failed to get fiscal year", t);
        if (t instanceof HttpException httpException && httpException.getCode() == 404) {
          return Future.failedFuture(new HttpException(400, ErrorCodes.FISCAL_YEAR_NOT_FOUND));
        } else {
          return Future.failedFuture(t);
        }
      });
  }

  public Future<Ledger> populateLedgerTotals(Ledger ledger, FiscalYear fiscalYear, RequestContext requestContext) {
    return getBudgetsByLedgerIdFiscalYearId(ledger.getId(), fiscalYear.getId(), requestContext)
      .map(budgets -> buildHolderSkeleton(fiscalYear.getId(), ledger,  budgets))
      .compose(holder -> updateHolderWithAllocations(holder, requestContext)
        .map(holderParam -> {
          updateLedgerWithAllocation(holder);
          return null;
        })
        .compose(v -> updateHolderWithTransfers(holder, requestContext))
        .map(v -> {
          updateLedgerWithCalculatedFields(holder);
          return null;
        })
        .map(v -> holder.getLedger()));
  }

  private void updateLedgerWithAllocation(LedgerFiscalYearTransactionsHolder holder) {
    holder.withToAllocations(new ArrayList<>(holder.getToAllocations()));
    removeInitialAllocationByFunds(holder.getToAllocations());
    Ledger ledger = holder.getLedger();
    ledger.withAllocationTo(HelperUtils.calculateTotals(holder.getToAllocations(), Transaction::getAmount))
      .withAllocationFrom(HelperUtils.calculateTotals(holder.getFromAllocations(), Transaction::getAmount));
  }

  private Future<LedgerFiscalYearTransactionsHolder> updateHolderWithAllocations(LedgerFiscalYearTransactionsHolder holder,
                                                                                 RequestContext requestContext) {
    List<String> ledgerFundIds = holder.getLedgerFundIds();
    String fiscalYearId = holder.getFiscalYearId();
    var fromAllocations = transactionService.getTransactionsFromFunds(ledgerFundIds, fiscalYearId, List.of(TransactionType.ALLOCATION), requestContext);
    var toAllocations = transactionService.getTransactionsToFunds(ledgerFundIds, fiscalYearId, List.of(TransactionType.ALLOCATION), requestContext);
    return GenericCompositeFuture.join(List.of(fromAllocations, toAllocations))
      .map(cf -> holder.withToAllocations(toAllocations.result()).withFromAllocations(fromAllocations.result()));
  }

  private Future<LedgerFiscalYearTransactionsHolder> updateHolderWithTransfers(LedgerFiscalYearTransactionsHolder holder,
                                                                                          RequestContext requestContext) {
    List<String> ledgerFundIds = holder.getLedgerFundIds();
    String fiscalYearId = holder.getFiscalYearId();
    List<TransactionType> trTypes = List.of(TransactionType.TRANSFER, TransactionType.ROLLOVER_TRANSFER);
    var fromTransfer = transactionService.getTransactionsFromFunds(ledgerFundIds, fiscalYearId, trTypes, requestContext);
    var toTransfer = transactionService.getTransactionsToFunds(ledgerFundIds, fiscalYearId, trTypes, requestContext);

    return GenericCompositeFuture.join(List.of(fromTransfer, toTransfer))
      .map(f -> holder.withToTransfers(toTransfer.result()).withFromTransfers(fromTransfer.result()));
  }

  public Future<LedgersCollection> populateLedgersTotals(LedgersCollection ledgersCollection, String fiscalYearId, RequestContext requestContext) {
    return getFiscalYear(fiscalYearId, requestContext)
      .compose(fiscalYear -> collectResultsOnSuccess(ledgersCollection.getLedgers().stream()
        .map(ledger -> populateLedgerTotals(ledger, fiscalYear, requestContext))
        .collect(Collectors.toList()))
      .map(ledgers -> new LedgersCollection().withLedgers(ledgers).withTotalRecords(ledgers.size())));
  }

  private Future<List<Budget>> getBudgetsByLedgerIdFiscalYearId(String ledgerId, String fiscalYearId , RequestContext requestContext) {
    String query = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledgerId, fiscalYearId);
    return budgetService.getBudgets(query, 0, Integer.MAX_VALUE, requestContext)
      .map(BudgetsCollection::getBudgets);
  }

  private LedgerFiscalYearTransactionsHolder buildHolderSkeleton(String fiscalYearId, Ledger ledger, List<Budget> budgets) {
    ledger.withUnavailable(HelperUtils.calculateTotals(budgets, Budget::getUnavailable))
      .withInitialAllocation(HelperUtils.calculateTotals(budgets, Budget::getInitialAllocation))
      .withAwaitingPayment(HelperUtils.calculateTotals(budgets, Budget::getAwaitingPayment))
      .withEncumbered(HelperUtils.calculateTotals(budgets, Budget::getEncumbered))
      .withExpenditures(HelperUtils.calculateTotals(budgets, Budget::getExpenditures))
      .withCredits(HelperUtils.calculateTotals(budgets, Budget::getCredits));
    return new LedgerFiscalYearTransactionsHolder(fiscalYearId, ledger, budgets);
  }

  /**
   * The method follows this formula: <br>
   * <p>
   * allocated = initialAllocation + allocationTo - allocationFrom <br>
   * totalFunding = allocated + netTransfers <br>
   * available = totalFunding - (encumbered + awaitingPayment - expended - credited) <br>
   * cashBalance = totalFunding - expended + credited <br>
   * overEncumbered = encumbered - totalFunding <br>
   * overExpended = expended - credited + awaitingPayment - totalFunding
   * </p>
   * @param holder LedgerFiscalYearTransactionsHolder
   */
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

    BigDecimal expended = BigDecimal.valueOf(ledger.getExpenditures());
    BigDecimal credited = BigDecimal.valueOf(ledger.getCredits());
    ledger.withCashBalance(totalFunding.subtract(expended).add(credited).doubleValue());

    BigDecimal encumbered = BigDecimal.valueOf(ledger.getEncumbered());
    BigDecimal overEncumbered = encumbered.subtract(totalFunding.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
    ledger.withOverEncumbrance(overEncumbered.doubleValue());

    BigDecimal awaitingPayment = BigDecimal.valueOf(ledger.getAwaitingPayment());
    BigDecimal overExpended = expended.subtract(credited).add(awaitingPayment)
      .subtract(totalFunding.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
    ledger.withOverExpended(overExpended.doubleValue());

    BigDecimal available = totalFunding.subtract(
      encumbered.add(awaitingPayment).add(expended).subtract(credited));
    ledger.withAvailable(available.doubleValue());
  }
}
