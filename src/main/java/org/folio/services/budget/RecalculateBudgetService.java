package org.folio.services.budget;

import io.vertx.core.Future;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.BudgetUtils;
import org.folio.rest.util.MoneyUtils;
import org.folio.services.transactions.CommonTransactionService;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class RecalculateBudgetService {
  private final BudgetService budgetService;
  private final CommonTransactionService transactionService;

  public RecalculateBudgetService(BudgetService budgetService, CommonTransactionService transactionService) {
    this.budgetService = budgetService;
    this.transactionService = transactionService;
  }

  public Future<Void> recalculateBudget(String budgetId, RequestContext requestContext) {
    return budgetService.getBudgetById(budgetId, requestContext)
      .compose(budget -> transactionService.retrieveTransactions(BudgetUtils.convertToBudget(budget), requestContext)
        .map(transactions -> recalculateBudgetBasedOnTransactions(budget, transactions)))
      .compose(budget -> budgetService.updateBudget(budget, requestContext))
      .mapEmpty();
  }

  private SharedBudget recalculateBudgetBasedOnTransactions(SharedBudget budget, List<Transaction> transactions) {
    String fundId = budget.getFundId();
    double initialAllocation = 0d;
    double allocationTo = 0d;
    double allocationFrom = 0d;
    double netTransfers = 0d;
    double encumbered = 0d;
    double awaitingPayment = 0d;
    double expended = 0d;

    if (!transactions.isEmpty()) {
      CurrencyUnit currency = Monetary.getCurrency(transactions.get(0).getCurrency());
      Map<Transaction.TransactionType, List<Transaction>> transactionGroupedByType = transactions.stream().collect(groupingBy(Transaction::getTransactionType));
      List<Transaction> allocationToList = getSortedAllocationToList(transactionGroupedByType.getOrDefault(Transaction.TransactionType.ALLOCATION, Collections.emptyList()), fundId);

      initialAllocation = getInitialAllocation(allocationToList, currency);
      allocationTo = MoneyUtils.calculateTotalAmount(allocationToList, currency).subtract(Money.of(initialAllocation, currency))
        .with(Monetary.getDefaultRounding()).getNumber().doubleValue();
      allocationFrom = MoneyUtils.calculateTotalAmountWithRounding(getAllocationFromList(
        transactionGroupedByType.getOrDefault(Transaction.TransactionType.ALLOCATION, Collections.emptyList()), fundId), currency);
      netTransfers = calculateNetTransfers(transactionGroupedByType.getOrDefault(Transaction.TransactionType.TRANSFER,
        Collections.emptyList()), fundId, currency);
      encumbered = MoneyUtils.calculateTotalAmountWithRounding(
        transactionGroupedByType.getOrDefault(Transaction.TransactionType.ENCUMBRANCE, Collections.emptyList()), currency);
      awaitingPayment = MoneyUtils.calculateTotalAmountWithRounding(
        transactionGroupedByType.getOrDefault(Transaction.TransactionType.PENDING_PAYMENT, Collections.emptyList()), currency);

      MonetaryAmount tmpExpended = MoneyUtils.calculateTotalAmount(
        transactionGroupedByType.getOrDefault(Transaction.TransactionType.PAYMENT, Collections.emptyList()), currency);
      tmpExpended = tmpExpended.subtract(MoneyUtils.calculateTotalAmount(
        transactionGroupedByType.getOrDefault(Transaction.TransactionType.CREDIT, Collections.emptyList()), currency));

      expended = tmpExpended.with(Monetary.getDefaultRounding()).getNumber().doubleValue();
    }

    return budget
      .withInitialAllocation(initialAllocation)
      .withAllocationTo(allocationTo)
      .withAllocationFrom(allocationFrom)
      .withNetTransfers(netTransfers)
      .withEncumbered(encumbered)
      .withAwaitingPayment(awaitingPayment)
      .withExpenditures(expended);
  }

  private List<Transaction> getSortedAllocationToList(List<Transaction> transactions, String fundId) {
    return getAllocationStreamByDirection(transactions, fundId, Transaction::getToFundId)
      .sorted(Comparator.comparing(transaction -> transaction.getMetadata().getCreatedDate()))
      .toList();
  }

  private List<Transaction> getAllocationFromList(List<Transaction> transactions, String fundId) {
    return getAllocationStreamByDirection(transactions, fundId, Transaction::getFromFundId)
      .toList();
  }

  private double getInitialAllocation(List<Transaction> allocationToList, CurrencyUnit currency) {
    return allocationToList.stream()
      .filter(transaction -> Objects.isNull(transaction.getFromFundId()))
      .findFirst()
      .map(transaction -> Money.of(transaction.getAmount(), currency))
      .orElse(Money.zero(currency))
      .with(Monetary.getDefaultRounding())
      .getNumber().doubleValue();
  }

  private double calculateNetTransfers(List<Transaction> transferTransactions, String fundId, CurrencyUnit currency) {
    return transferTransactions.stream()
      .map(transaction -> {
        MonetaryAmount amount = Money.of(transaction.getAmount(), currency);
        return transaction.getFromFundId().equals(fundId) ? amount.negate() : amount;
      })
      .reduce(MonetaryFunctions::sum)
      .orElse(Money.zero(currency))
      .with(Monetary.getDefaultRounding())
      .getNumber().doubleValue();
  }

  private Stream<Transaction> getAllocationStreamByDirection(List<Transaction> transactions, String fundId, Function<Transaction, String> fundIdExtractor) {
    return transactions.stream()
      .filter(transaction -> Objects.equals(fundIdExtractor.apply(transaction), fundId));
  }

}
