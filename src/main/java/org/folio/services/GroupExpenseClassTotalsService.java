package org.folio.services;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.money.MonetaryAmount;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotal;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.Transaction;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;

public class GroupExpenseClassTotalsService {

  private final GroupFundFiscalYearService groupFundFiscalYearService;
  private final TransactionService transactionService;
  private final ExpenseClassService expenseClassService;

  public GroupExpenseClassTotalsService(GroupFundFiscalYearService groupFundFiscalYearService, TransactionService transactionService, ExpenseClassService expenseClassService) {
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
    return transactionService.getTransactionsByFundIds(fundIds, fiscalYearId, requestContext);
  }

  private CompletableFuture<List<ExpenseClass>> getExpenseClasses(List<GroupFundFiscalYear> groupFundFiscalYears, RequestContext requestContext) {
    List<String> budgetIds = groupFundFiscalYears.stream().map(GroupFundFiscalYear::getBudgetId).collect(Collectors.toList());
    return expenseClassService.getExpenseClassesByBudgetIds(budgetIds, requestContext);
  }

  private GroupExpenseClassTotalsCollection buildGroupExpenseClassesTotals(List<ExpenseClass> expenseClasses, List<Transaction> transactions) {

    Map<String, List<Transaction>> expenseClassIdTransactionsMap = transactions.stream()
      .filter(transaction -> StringUtils.isNotEmpty(transaction.getExpenseClassId()))
      .collect(groupingBy(Transaction::getExpenseClassId));

    double expendedGrandTotal = calculateTransactionsAmount(transactions);

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
    double expended = calculateTransactionsAmount(transactions);
    double percentageExpended = 0d;
    if (expendedGrandTotal != 0d) {
      percentageExpended = BigDecimal.valueOf(expended)
        .divide(BigDecimal.valueOf(expendedGrandTotal))
        .multiply(BigDecimal.valueOf(100))
        .doubleValue();
    }
    return new GroupExpenseClassTotal()
      .withId(expenseClass.getId())
      .withExpenseClassName(expenseClass.getName())
      .withExpended(expended)
      .withPercentageExpended(percentageExpended);
  }

}
