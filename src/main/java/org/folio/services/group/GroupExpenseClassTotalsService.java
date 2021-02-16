package org.folio.services.group;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotal;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.MoneyUtils;
import org.folio.services.ExpenseClassService;
import org.folio.services.transactions.CommonTransactionService;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;

public class GroupExpenseClassTotalsService {

  private final GroupFundFiscalYearService groupFundFiscalYearService;
  private final CommonTransactionService transactionService;
  private final ExpenseClassService expenseClassService;

  public GroupExpenseClassTotalsService(GroupFundFiscalYearService groupFundFiscalYearService, CommonTransactionService transactionService, ExpenseClassService expenseClassService) {
    this.groupFundFiscalYearService = groupFundFiscalYearService;
    this.transactionService = transactionService;
    this.expenseClassService = expenseClassService;
  }

  public CompletableFuture<GroupExpenseClassTotalsCollection> getExpenseClassTotals(String groupId, String fiscalYearId, RequestContext requestContext) {
    return groupFundFiscalYearService.getGroupFundFiscalYearsWithBudgetId(groupId, fiscalYearId, requestContext)
      .thenCompose(groupFundFiscalYearCollection -> getGroupExpenseClassTotals(groupFundFiscalYearCollection, fiscalYearId, requestContext));
  }

  private CompletableFuture<GroupExpenseClassTotalsCollection> getGroupExpenseClassTotals(List<GroupFundFiscalYear> groupFfys, String fiscalYearId, RequestContext requestContext) {
    if (groupFfys.isEmpty()) {
      return CompletableFuture.completedFuture(new GroupExpenseClassTotalsCollection().withTotalRecords(0));
    }

    return getTransactions(groupFfys, fiscalYearId, requestContext)
      .thenCombine(getExpenseClasses(groupFfys, requestContext),
        (transactions, expenseClasses) -> buildGroupExpenseClassesTotals(expenseClasses, transactions));
  }

  private CompletableFuture<List<Transaction>> getTransactions(List<GroupFundFiscalYear> groupFundFiscalYears, String fiscalYearId, RequestContext requestContext) {
    List<String> fundIds = groupFundFiscalYears.stream().map(GroupFundFiscalYear::getFundId).collect(Collectors.toList());
    return transactionService.retrieveTransactionsByFundIds(fundIds, fiscalYearId, requestContext);
  }

  private CompletableFuture<List<ExpenseClass>> getExpenseClasses(List<GroupFundFiscalYear> groupFundFiscalYears, RequestContext requestContext) {
    List<String> budgetIds = groupFundFiscalYears.stream().map(GroupFundFiscalYear::getBudgetId).collect(Collectors.toList());
    return expenseClassService.getExpenseClassesByBudgetIds(budgetIds, requestContext);
  }

  private GroupExpenseClassTotalsCollection buildGroupExpenseClassesTotals(List<ExpenseClass> expenseClasses, List<Transaction> transactions) {

    Map<String, List<Transaction>> expenseClassIdTransactionsMap = transactions.stream()
      .filter(transaction -> StringUtils.isNotEmpty(transaction.getExpenseClassId()))
      .collect(groupingBy(Transaction::getExpenseClassId));

    List<Transaction> paymentsCredits = transactions.stream()
            .filter(transaction -> transaction.getTransactionType() == Transaction.TransactionType.PAYMENT
                    || transaction.getTransactionType() == Transaction.TransactionType.CREDIT)
            .collect(toList());

    double expendedGrandTotal = calculateTransactionsAmount(paymentsCredits);

    List<GroupExpenseClassTotal> groupExpenseClassTotals =  expenseClasses.stream()
      .map(expenseClass -> buildGroupExpenseClassTotal(expenseClass,
        expenseClassIdTransactionsMap.getOrDefault(expenseClass.getId(),  Collections.emptyList()),
        expendedGrandTotal))
      .collect(toList());

    return new GroupExpenseClassTotalsCollection()
      .withGroupExpenseClassTotals(groupExpenseClassTotals)
      .withTotalRecords(groupExpenseClassTotals.size());
  }

  private Double calculateTransactionsAmount(List<Transaction> transactions) {
    return transactions.stream()
      .map(this::toMoney)
      .reduce(MonetaryFunctions.sum())
      .map(money -> money.getNumber().doubleValue())
      .orElse(0d);
  }

  private MonetaryAmount toMoney(Transaction transaction) {
    MonetaryAmount amount = Money.of(transaction.getAmount(), transaction.getCurrency());
    return transaction.getTransactionType() == Transaction.TransactionType.CREDIT ? amount.negate() : amount;
  }

  private GroupExpenseClassTotal buildGroupExpenseClassTotal(ExpenseClass expenseClass, List<Transaction> transactions, double expendedGrandTotal) {

    double expended = 0d;
    double encumbered = 0d;
    double awaitingPayment = 0d;
    Double percentageExpended = 0d;

    if (!transactions.isEmpty()) {
      CurrencyUnit currency = Monetary.getCurrency(transactions.get(0).getCurrency());
      Map<Transaction.TransactionType, List<Transaction>> transactionGroupedByType = transactions.stream().collect(groupingBy(Transaction::getTransactionType));

      encumbered = MoneyUtils.calculateTotalAmountWithRounding(
              transactionGroupedByType.getOrDefault(Transaction.TransactionType.ENCUMBRANCE, Collections.emptyList()), currency);
      awaitingPayment = MoneyUtils.calculateTotalAmountWithRounding(
              transactionGroupedByType.getOrDefault(Transaction.TransactionType.PENDING_PAYMENT, Collections.emptyList()), currency);

      MonetaryAmount tmpExpended = MoneyUtils.calculateTotalAmount(
              transactionGroupedByType.getOrDefault(Transaction.TransactionType.PAYMENT, Collections.emptyList()), currency);
      tmpExpended = tmpExpended.subtract(MoneyUtils.calculateTotalAmount(
              transactionGroupedByType.getOrDefault(Transaction.TransactionType.CREDIT, Collections.emptyList()), currency));

      expended = tmpExpended.with(Monetary.getDefaultRounding()).getNumber().doubleValue();

      percentageExpended = expendedGrandTotal == 0 ? null : MoneyUtils.calculateExpendedPercentage(tmpExpended, expendedGrandTotal);
    }

    return new GroupExpenseClassTotal()
      .withId(expenseClass.getId())
      .withExpenseClassName(expenseClass.getName())
      .withEncumbered(encumbered)
      .withAwaitingPayment(awaitingPayment)
      .withExpended(expended)
      .withPercentageExpended(percentageExpended);
  }

}
