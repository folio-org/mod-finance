package org.folio.services.group;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.folio.rest.util.MoneyUtils.calculateCreditedPercentage;
import static org.folio.rest.util.MoneyUtils.calculateExpendedPercentage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotal;
import org.folio.rest.jaxrs.model.GroupExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.RecalculatedBudgetBuilder;
import org.folio.services.transactions.TransactionService;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;

import io.vertx.core.Future;

public class GroupExpenseClassTotalsService {

  private static final Logger log = LogManager.getLogger();

  private final GroupFundFiscalYearService groupFundFiscalYearService;
  private final TransactionService transactionService;
  private final ExpenseClassService expenseClassService;

  public GroupExpenseClassTotalsService(GroupFundFiscalYearService groupFundFiscalYearService, TransactionService transactionService, ExpenseClassService expenseClassService) {
    this.groupFundFiscalYearService = groupFundFiscalYearService;
    this.transactionService = transactionService;
    this.expenseClassService = expenseClassService;
  }

  public Future<GroupExpenseClassTotalsCollection> getExpenseClassTotals(String groupId, String fiscalYearId, RequestContext requestContext) {
    return groupFundFiscalYearService.getGroupFundFiscalYearsWithBudgetId(groupId, fiscalYearId, requestContext)
      .compose(groupFundFiscalYearCollection -> getGroupExpenseClassTotals(groupFundFiscalYearCollection, fiscalYearId, requestContext));
  }

  private Future<GroupExpenseClassTotalsCollection> getGroupExpenseClassTotals(List<GroupFundFiscalYear> groupFfys, String fiscalYearId, RequestContext requestContext) {
    log.debug("getGroupExpenseClassTotals:: Retrieving group expense class totals for groupFys with '{}' size by fiscalYearId '{}'", groupFfys.size(), fiscalYearId);
    if (groupFfys.isEmpty()) {
      log.info("getGroupExpenseClassTotals:: groupFfys is empty, so returning new collection for fiscalYearId: {}", fiscalYearId);
      return succeededFuture(new GroupExpenseClassTotalsCollection().withTotalRecords(0));
    }
    var transactions = getTransactions(groupFfys, fiscalYearId, requestContext);
    var expenseClasses = getExpenseClasses(groupFfys, requestContext);
    return GenericCompositeFuture.join(List.of(transactions, expenseClasses))
      .map(cf -> buildGroupExpenseClassesTotals(expenseClasses.result(), transactions.result()));
  }

  private Future<List<Transaction>> getTransactions(List<GroupFundFiscalYear> groupFundFiscalYears, String fiscalYearId, RequestContext requestContext) {
    List<String> fundIds = groupFundFiscalYears.stream().map(GroupFundFiscalYear::getFundId).collect(Collectors.toList());
    return transactionService.getTransactionsByFundIds(fundIds, fiscalYearId, requestContext);
  }

  private Future<List<ExpenseClass>> getExpenseClasses(List<GroupFundFiscalYear> groupFundFiscalYears, RequestContext requestContext) {
    List<String> budgetIds = groupFundFiscalYears.stream().map(GroupFundFiscalYear::getBudgetId).collect(Collectors.toList());
    return expenseClassService.getExpenseClassesByBudgetIds(budgetIds, requestContext);
  }

  private GroupExpenseClassTotalsCollection buildGroupExpenseClassesTotals(List<ExpenseClass> expenseClasses,
                                                                           List<Transaction> transactions) {
    log.debug("buildGroupExpenseClassesTotals:: Building Group Expense classes totals");
    Map<String, List<Transaction>> expenseClassIdTransactionsMap = transactions.stream()
      .filter(transaction -> StringUtils.isNotEmpty(transaction.getExpenseClassId()))
      .collect(groupingBy(Transaction::getExpenseClassId));

    List<Transaction> payments = transactions.stream()
      .filter(transaction -> transaction.getTransactionType() == Transaction.TransactionType.PAYMENT)
      .toList();
    List<Transaction> credits = transactions.stream()
      .filter(transaction -> transaction.getTransactionType() == Transaction.TransactionType.CREDIT)
      .toList();

    double expendedGrandTotal = calculateTransactionsAmount(payments);
    double creditedGrandTotal = calculateTransactionsAmount(credits);

    List<GroupExpenseClassTotal> groupExpenseClassTotals = expenseClasses.stream()
      .map(expenseClass -> buildGroupExpenseClassTotal(expenseClass,
        expenseClassIdTransactionsMap.getOrDefault(expenseClass.getId(), Collections.emptyList()),
        expendedGrandTotal, creditedGrandTotal))
      .collect(toList());

    log.info("buildGroupExpenseClassesTotals:: Creating collection for groupExpenseClassTotals with '{}' element(s) and expendedGrandTotal '{}'",
      groupExpenseClassTotals.size(), expendedGrandTotal);
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
    return Money.of(transaction.getAmount(), transaction.getCurrency());
  }

  private GroupExpenseClassTotal buildGroupExpenseClassTotal(ExpenseClass expenseClass, List<Transaction> transactions,
                                                             double expendedGrandTotal, double creditedGrandTotal) {
    log.debug("buildGroupExpenseClassTotal:: Building group expense class totals by using expendedGrandTotal={} and '{}' transaction(s)", expendedGrandTotal, transactions);
    double expended = 0d;
    double credited = 0d;
    double encumbered = 0d;
    double awaitingPayment = 0d;
    Double percentageExpended = 0d;
    Double percentageCredited = 0d;

    if (CollectionUtils.isNotEmpty(transactions)) {
      RecalculatedBudgetBuilder budgetBuilder = new RecalculatedBudgetBuilder(transactions);
      SharedBudget recalculatedBudget = budgetBuilder.withEncumbered().withAwaitingPayment()
        .withExpended().withCredited().build();

      expended = recalculatedBudget.getExpenditures();
      credited = recalculatedBudget.getCredits();
      encumbered = recalculatedBudget.getEncumbered();
      awaitingPayment = recalculatedBudget.getAwaitingPayment();

      CurrencyUnit currency = Monetary.getCurrency(transactions.get(0).getCurrency());
      percentageExpended = expendedGrandTotal == 0 ? null : calculateExpendedPercentage(Money.of(recalculatedBudget.getExpenditures(), currency), expendedGrandTotal);
      percentageCredited = creditedGrandTotal == 0 ? null : calculateCreditedPercentage(Money.of(recalculatedBudget.getCredits(), currency), creditedGrandTotal);
    }

    log.info("buildGroupExpenseClassTotal:: Creating groupExpenseClass total for encumbered={}, awaitingPayment={}, expended={}, percentageExpended={}, credited={}, and percentageCredited= {}",
      encumbered, awaitingPayment, expended, percentageExpended, credited, percentageCredited);
    return new GroupExpenseClassTotal()
      .withId(expenseClass.getId())
      .withExpenseClassName(expenseClass.getName())
      .withEncumbered(encumbered)
      .withAwaitingPayment(awaitingPayment)
      .withExpended(expended)
      .withPercentageExpended(percentageExpended)
      .withCredited(credited)
      .withPercentageCredited(percentageCredited);
  }

}
