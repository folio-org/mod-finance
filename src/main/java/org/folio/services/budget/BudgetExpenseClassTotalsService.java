package org.folio.services.budget;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotal;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.MoneyUtils;
import org.folio.services.ExpenseClassService;
import org.folio.services.transactions.CommonTransactionService;

import io.vertx.core.Future;
import org.javamoney.moneta.Money;

public class BudgetExpenseClassTotalsService {

  private final RestClient restClient;
  private final ExpenseClassService expenseClassService;
  private final CommonTransactionService transactionService;
  private final BudgetExpenseClassService budgetExpenseClassService;

  public BudgetExpenseClassTotalsService(RestClient restClient,
                                         ExpenseClassService expenseClassService,
                                         CommonTransactionService transactionService,
                                         BudgetExpenseClassService budgetExpenseClassService) {
    this.restClient = restClient;
    this.expenseClassService = expenseClassService;
    this.transactionService = transactionService;
    this.budgetExpenseClassService = budgetExpenseClassService;
  }

  public Future<BudgetExpenseClassTotalsCollection> getExpenseClassTotals(String budgetId, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(BUDGETS_STORAGE, budgetId), Budget.class, requestContext)
      .compose(budget -> expenseClassService.getExpenseClassesByBudgetId(budgetId, requestContext)
        .compose(expenseClasses -> transactionService.retrieveTransactions(budget, requestContext)
          .map(transactions -> buildBudgetExpenseClassesTotals(expenseClasses, transactions, budget))))
      .compose(budgetExpenseClassTotalsCollection -> budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContext)
        .map(budgetExpenseClasses -> updateExpenseClassStatus(budgetExpenseClassTotalsCollection, budgetExpenseClasses)));
  }

  private BudgetExpenseClassTotalsCollection buildBudgetExpenseClassesTotals(List<ExpenseClass> expenseClasses, List<Transaction> transactions, Budget budget) {

    double totalExpended = getBudgetTotalExpended(budget);

    Map<String, List<Transaction>> groupedByExpenseClassId = transactions.stream()
      .filter(transaction -> Objects.nonNull(transaction.getExpenseClassId()))
      .collect(groupingBy(Transaction::getExpenseClassId));

    Map<ExpenseClass, List<Transaction>> groupedByExpenseClass = expenseClasses.stream()
      .collect(toMap(Function.identity(), expenseClass -> groupedByExpenseClassId.getOrDefault(expenseClass.getId(), Collections.emptyList())));

    List<BudgetExpenseClassTotal> budgetExpenseClassTotals = buildBudgetExpenseClassesTotals(groupedByExpenseClass, totalExpended);

    return new BudgetExpenseClassTotalsCollection()
      .withBudgetExpenseClassTotals(budgetExpenseClassTotals)
      .withTotalRecords(budgetExpenseClassTotals.size());
  }

  private double getBudgetTotalExpended(Budget budget) {
    BigDecimal totalExpended = BigDecimal.valueOf(budget.getExpenditures()).add(BigDecimal.valueOf(budget.getOverExpended()));
    return totalExpended.doubleValue();
  }

  private List<BudgetExpenseClassTotal> buildBudgetExpenseClassesTotals(Map<ExpenseClass, List<Transaction>> groupedByExpenseClass, double totalExpended) {
    return groupedByExpenseClass.entrySet().stream()
      .map(entry -> buildBudgetExpenseClassTotals(entry.getKey(), entry.getValue(), totalExpended))
      .collect(Collectors.toList());
  }

  private BudgetExpenseClassTotal buildBudgetExpenseClassTotals(ExpenseClass expenseClass, List<Transaction> transactions, double totalExpended) {
    double expended = 0d;
    double encumbered = 0d;
    double awaitingPayment = 0d;
    Double expendedPercentage = 0d;

    if (CollectionUtils.isNotEmpty(transactions)) {
      RecalculatedBudgetBuilder budgetBuilder = new RecalculatedBudgetBuilder(transactions);
      SharedBudget recalculatedBudget = budgetBuilder.withEncumbered().withAwaitingPayment().withExpended().build();

      expended = recalculatedBudget.getExpenditures();
      encumbered = recalculatedBudget.getEncumbered();
      awaitingPayment = recalculatedBudget.getAwaitingPayment();

      CurrencyUnit currency = Monetary.getCurrency(transactions.get(0).getCurrency());
      expendedPercentage = totalExpended == 0 ? null : MoneyUtils.calculateExpendedPercentage(Money.of(recalculatedBudget.getExpenditures(), currency), totalExpended);
    }

    return new BudgetExpenseClassTotal()
      .withId(expenseClass.getId())
      .withExpenseClassName(expenseClass.getName())
      .withEncumbered(encumbered)
      .withAwaitingPayment(awaitingPayment)
      .withExpended(expended)
      .withPercentageExpended(expendedPercentage);
  }

  private BudgetExpenseClassTotalsCollection updateExpenseClassStatus(BudgetExpenseClassTotalsCollection budgetExpenseClassTotalsCollection,
                                                                      List<BudgetExpenseClass> budgetExpenseClasses) {
    List<BudgetExpenseClassTotal> budgetExpenseClassTotals = budgetExpenseClassTotalsCollection.getBudgetExpenseClassTotals();
    Map<String, BudgetExpenseClassTotal.ExpenseClassStatus> idStatusMap = budgetExpenseClasses.stream()
      .collect(toMap(BudgetExpenseClass::getExpenseClassId, budgetExpenseClass -> BudgetExpenseClassTotal.ExpenseClassStatus.fromValue(budgetExpenseClass.getStatus().value())));
    budgetExpenseClassTotals.forEach(budgetExpenseClassTotal -> budgetExpenseClassTotal.setExpenseClassStatus(idStatusMap.get(budgetExpenseClassTotal.getId())));
    return budgetExpenseClassTotalsCollection;
  }

}
