package org.folio.services.group;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

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
import org.folio.rest.util.MoneyUtils;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.RecalculatedBudgetBuilder;
import org.folio.services.transactions.CommonTransactionService;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;

import io.vertx.core.Future;

public class GroupExpenseClassTotalsService {

  private static final Logger log = LogManager.getLogger();

  private final GroupFundFiscalYearService groupFundFiscalYearService;
  private final CommonTransactionService transactionService;
  private final ExpenseClassService expenseClassService;

  public GroupExpenseClassTotalsService(GroupFundFiscalYearService groupFundFiscalYearService, CommonTransactionService transactionService, ExpenseClassService expenseClassService) {
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
      log.info("getGroupExpenseClassTotals:: groupFfys is empty, so returning new collection");
      return succeededFuture(new GroupExpenseClassTotalsCollection().withTotalRecords(0));
    }
    var transactions = getTransactions(groupFfys, fiscalYearId, requestContext);
    var expenseClasses = getExpenseClasses(groupFfys, requestContext);
    return GenericCompositeFuture.join(List.of(transactions, expenseClasses))
      .map(cf -> buildGroupExpenseClassesTotals(expenseClasses.result(), transactions.result()));
  }

  private Future<List<Transaction>> getTransactions(List<GroupFundFiscalYear> groupFundFiscalYears, String fiscalYearId, RequestContext requestContext) {
    List<String> fundIds = groupFundFiscalYears.stream().map(GroupFundFiscalYear::getFundId).collect(Collectors.toList());
    return transactionService.retrieveTransactionsByFundIds(fundIds, fiscalYearId, requestContext);
  }

  private Future<List<ExpenseClass>> getExpenseClasses(List<GroupFundFiscalYear> groupFundFiscalYears, RequestContext requestContext) {
    List<String> budgetIds = groupFundFiscalYears.stream().map(GroupFundFiscalYear::getBudgetId).collect(Collectors.toList());
    return expenseClassService.getExpenseClassesByBudgetIds(budgetIds, requestContext);
  }

  private GroupExpenseClassTotalsCollection buildGroupExpenseClassesTotals(List<ExpenseClass> expenseClasses, List<Transaction> transactions) {
    log.debug("buildGroupExpenseClassesTotals:: Building Group Expense classes totals");
    Map<String, List<Transaction>> expenseClassIdTransactionsMap = transactions.stream()
      .filter(transaction -> StringUtils.isNotEmpty(transaction.getExpenseClassId()))
      .collect(groupingBy(Transaction::getExpenseClassId));

    List<Transaction> paymentsCredits = transactions.stream()
            .filter(transaction -> transaction.getTransactionType() == Transaction.TransactionType.PAYMENT
                    || transaction.getTransactionType() == Transaction.TransactionType.CREDIT)
            .collect(Collectors.toList());

    double expendedGrandTotal = calculateTransactionsAmount(paymentsCredits);

    List<GroupExpenseClassTotal> groupExpenseClassTotals =  expenseClasses.stream()
      .map(expenseClass -> buildGroupExpenseClassTotal(expenseClass,
        expenseClassIdTransactionsMap.getOrDefault(expenseClass.getId(),  Collections.emptyList()),
        expendedGrandTotal))
      .collect(toList());

    log.info("buildGroupExpenseClassesTotals:: Creating collection for groupExpenseClassTotals with '{}' element(s) and expendedGrandTotal '{}'", groupExpenseClassTotals.size(), expendedGrandTotal);
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
    log.debug("buildGroupExpenseClassTotal:: Building group expense class totals by using expendedGrandTotal={} and '{}' transaction(s)", expendedGrandTotal, transactions);
    double expended = 0d;
    double encumbered = 0d;
    double awaitingPayment = 0d;
    Double percentageExpended = 0d;

    if (CollectionUtils.isNotEmpty(transactions)) {
      RecalculatedBudgetBuilder budgetBuilder = new RecalculatedBudgetBuilder(transactions);
      SharedBudget recalculatedBudget = budgetBuilder.withEncumbered().withAwaitingPayment().withExpended().build();

      expended = recalculatedBudget.getExpenditures();
      encumbered = recalculatedBudget.getEncumbered();
      awaitingPayment = recalculatedBudget.getAwaitingPayment();

      CurrencyUnit currency = Monetary.getCurrency(transactions.get(0).getCurrency());
      percentageExpended = expendedGrandTotal == 0 ? null : MoneyUtils.calculateExpendedPercentage(Money.of(recalculatedBudget.getExpenditures(), currency), expendedGrandTotal);
    }
    log.info("buildGroupExpenseClassTotal:: Creating groupExpenseClass total for encumbered={}, awaitingPayment={}, expended={}, and percentageExpended={}", encumbered, awaitingPayment, expenseClass, percentageExpended);
    return new GroupExpenseClassTotal()
      .withId(expenseClass.getId())
      .withExpenseClassName(expenseClass.getName())
      .withEncumbered(encumbered)
      .withAwaitingPayment(awaitingPayment)
      .withExpended(expended)
      .withPercentageExpended(percentageExpended);
  }

}
