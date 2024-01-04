package org.folio.services.budget;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.MoneyUtils;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ALLOCATION;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.CREDIT;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ENCUMBRANCE;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.PAYMENT;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.PENDING_PAYMENT;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.TRANSFER;

public class RecalculatedBudgetBuilder {

  private static final Logger logger = LogManager.getLogger(RecalculatedBudgetBuilder.class);

  private double initialAllocation = 0d;
  private double allocationTo = 0d;
  private double allocationFrom = 0d;
  private double netTransfers = 0d;
  private double encumbered = 0d;
  private double awaitingPayment = 0d;
  private double expended = 0d;

  private boolean initialAllocationSet = false;

  private final CurrencyUnit currency;
  private final Map<Transaction.TransactionType, List<Transaction>> transactionGroupedByType;

  public RecalculatedBudgetBuilder(List<Transaction> transactions) {
    if (CollectionUtils.isEmpty(transactions)) {
      logger.error("The transactions list is empty. Cannot proceed with RecalculatedBudgetBuilder creation.");
      throw new IllegalArgumentException("The transactions list cannot be empty");
    }

    this.currency = Monetary.getCurrency(transactions.get(0).getCurrency());
    this.transactionGroupedByType = transactions.stream().collect(groupingBy(Transaction::getTransactionType));
  }

  /**
   * Sets the initial allocation amount based on the first increase allocation transaction sorted by date.
   *
   * @param fundId Fund ID
   * @return This RecalculatedBudgetBuilder instance for method chaining
   */
  public RecalculatedBudgetBuilder withInitialAllocation(String fundId) {
    initialAllocationSet = true;
    initialAllocation = getSortedAllocationToList(fundId).stream()
      .filter(transaction -> Objects.isNull(transaction.getFromFundId()))
      .findFirst()
      .map(transaction -> Money.of(transaction.getAmount(), currency))
      .orElse(Money.zero(currency))
      .with(Monetary.getDefaultRounding())
      .getNumber().doubleValue();
    return this;
  }

  /**
   * Sets the allocation to amount based on the sum of all allocation transactions where the toFundId
   * matches the fundId of the current budget.
   * Requires that withInitialAllocation() is called before this method.
   *
   * @param fundId Fund ID
   * @return This RecalculatedBudgetBuilder instance for method chaining
   * @throws IllegalStateException if withInitialAllocation is not called before this method
   */
  public RecalculatedBudgetBuilder withAllocationTo(String fundId) {
    if (!initialAllocationSet) {
      logger.error("Cannot set Allocation To without calling withInitialAllocation first.");
      throw new IllegalStateException("withInitialAllocation must be called before withAllocationTo");
    }

    allocationTo = MoneyUtils.calculateTotalAmount(getSortedAllocationToList(fundId), currency).subtract(Money.of(initialAllocation, currency))
      .with(Monetary.getDefaultRounding()).getNumber().doubleValue();
    return this;
  }

  /**
   * Sets the allocation from amount based on the sum of all allocation transactions where the fromFundId
   * matches the fundId of the current budget.
   *
   * @param fundId Fund ID
   * @return This RecalculatedBudgetBuilder instance for method chaining
   */
  public RecalculatedBudgetBuilder withAllocationFrom(String fundId) {
    allocationFrom = MoneyUtils.calculateTotalAmountWithRounding(getAllocationFromList(fundId), currency);
    return this;
  }

  /**
   * Sets the net transfers amount based on the sum of 'Transfer' transactions.
   * Transfers originating from the budget are subtracted, while others are summed.
   *
   * @param fundId Fund ID
   * @return This RecalculatedBudgetBuilder instance for method chaining
   */
  public RecalculatedBudgetBuilder withNetTransfers(String fundId) {
    netTransfers = getTransactionByType(TRANSFER).stream()
      .map(transaction -> {
        MonetaryAmount amount = Money.of(transaction.getAmount(), currency);
        return Objects.equals(transaction.getFromFundId(), fundId) ? amount.negate() : amount;
      })
      .reduce(MonetaryFunctions::sum)
      .orElse(Money.zero(currency))
      .with(Monetary.getDefaultRounding())
      .getNumber().doubleValue();
    return this;
  }

  /**
   * Sets the encumbered amount based on the sum of 'Encumbrance' transactions.
   *
   * @return This RecalculatedBudgetBuilder instance for method chaining
   */
  public RecalculatedBudgetBuilder withEncumbered() {
    encumbered = MoneyUtils.calculateTotalAmountWithRounding(getTransactionByType(ENCUMBRANCE), currency);
    return this;
  }

  /**
   * Sets the awaiting payment amount based on the sum of 'Pending Payment' transactions.
   *
   * @return This RecalculatedBudgetBuilder instance for method chaining
   */
  public RecalculatedBudgetBuilder withAwaitingPayment() {
    awaitingPayment = MoneyUtils.calculateTotalAmountWithRounding(getTransactionByType(PENDING_PAYMENT), currency);
    return this;
  }

  /**
   * Sets the expended amount based on the sum of 'Payment' transactions minus the sum of 'Credit' transactions.
   *
   * @return This RecalculatedBudgetBuilder instance for method chaining
   */
  public RecalculatedBudgetBuilder withExpended() {
    expended = MoneyUtils.calculateTotalAmount(getTransactionByType(PAYMENT), currency)
      .subtract(MoneyUtils.calculateTotalAmount(getTransactionByType(CREDIT), currency))
      .with(Monetary.getDefaultRounding()).getNumber().doubleValue();
    return this;
  }

  /**
   * Builds and returns a SharedBudget instance with the calculated budget components.
   *
   * @return SharedBudget instance representing the recalculated budget
   */
  public SharedBudget build() {
    return new SharedBudget()
      .withInitialAllocation(initialAllocation)
      .withAllocationTo(allocationTo)
      .withAllocationFrom(allocationFrom)
      .withNetTransfers(netTransfers)
      .withEncumbered(encumbered)
      .withAwaitingPayment(awaitingPayment)
      .withExpenditures(expended);
  }

  private List<Transaction> getSortedAllocationToList(String fundId) {
    return getAllocationStreamByDirection(getTransactionByType(ALLOCATION), fundId, Transaction::getToFundId)
      .sorted(Comparator.comparing(transaction -> transaction.getMetadata().getCreatedDate()))
      .toList();
  }

  private List<Transaction> getAllocationFromList(String fundId) {
    return getAllocationStreamByDirection(getTransactionByType(ALLOCATION), fundId, Transaction::getFromFundId)
      .toList();
  }

  private Stream<Transaction> getAllocationStreamByDirection(List<Transaction> transactions, String fundId, Function<Transaction, String> fundIdExtractor) {
    return transactions.stream()
      .filter(transaction -> Objects.equals(fundIdExtractor.apply(transaction), fundId));
  }

  private List<Transaction> getTransactionByType(Transaction.TransactionType transactionType) {
    return transactionGroupedByType.getOrDefault(transactionType, Collections.emptyList());
  }

}
