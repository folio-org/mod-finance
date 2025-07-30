package org.folio.services.budget;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.folio.models.ExpenseClassUnassigned.getExpenseClassName;
import static org.folio.rest.util.MoneyUtils.calculateCreditedPercentage;
import static org.folio.rest.util.MoneyUtils.calculateExpendedPercentage;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.models.ExpenseClassUnassigned;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotal;
import org.folio.rest.jaxrs.model.BudgetExpenseClassTotalsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.ExpenseClassService;
import org.folio.services.transactions.TransactionService;

import io.vertx.core.Future;
import org.javamoney.moneta.Money;

public class BudgetExpenseClassTotalsService {

  private final RestClient restClient;
  private final ExpenseClassService expenseClassService;
  private final TransactionService transactionService;
  private final BudgetExpenseClassService budgetExpenseClassService;

  public BudgetExpenseClassTotalsService(RestClient restClient,
                                         ExpenseClassService expenseClassService,
                                         TransactionService transactionService,
                                         BudgetExpenseClassService budgetExpenseClassService) {
    this.restClient = restClient;
    this.expenseClassService = expenseClassService;
    this.transactionService = transactionService;
    this.budgetExpenseClassService = budgetExpenseClassService;
  }

  public Future<BudgetExpenseClassTotalsCollection> getExpenseClassTotals(String budgetId, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(BUDGETS_STORAGE, budgetId), Budget.class, requestContext)
      .compose(budget -> getExpenseClasses(budgetId, requestContext)
        .compose(expenseClasses -> transactionService.getBudgetTransactions(budget, requestContext)
          .map(transactions -> buildBudgetExpenseClassesTotals(expenseClasses, transactions, budget))))
      .compose(budgetExpenseClassTotalsCollection -> budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContext)
        .map(budgetExpenseClasses -> updateExpenseClassStatus(budgetExpenseClassTotalsCollection, budgetExpenseClasses)));
  }

  private Future<List<ExpenseClass>> getExpenseClasses(String budgetId, RequestContext requestContext) {
    return expenseClassService.getExpenseClassesByBudgetId(budgetId, requestContext)
      .map(expenseClasses -> {
        expenseClasses.add(new ExpenseClass().withId(ExpenseClassUnassigned.ID.getValue()));
        return expenseClasses;
      });
  }

  private BudgetExpenseClassTotalsCollection buildBudgetExpenseClassesTotals(List<ExpenseClass> expenseClasses, List<Transaction> transactions, Budget budget) {
    double totalExpended = budget.getExpenditures();
    double totalCredited = budget.getCredits();

    Map<String, List<Transaction>> groupedByExpenseClassId = transactions.stream()
      .map(transaction -> {
        if (StringUtils.isEmpty(transaction.getExpenseClassId())) {
          return transaction.withExpenseClassId(ExpenseClassUnassigned.ID.getValue());
        }
        return transaction;
      })
      .collect(groupingBy(Transaction::getExpenseClassId));

    Map<ExpenseClass, List<Transaction>> groupedByExpenseClass = expenseClasses.stream()
      .collect(toMap(Function.identity(), expenseClass -> groupedByExpenseClassId.getOrDefault(expenseClass.getId(), Collections.emptyList())));

    List<BudgetExpenseClassTotal> budgetExpenseClassTotals = buildBudgetExpenseClassesTotals(groupedByExpenseClass, totalExpended, totalCredited);

    return new BudgetExpenseClassTotalsCollection()
      .withBudgetExpenseClassTotals(budgetExpenseClassTotals)
      .withTotalRecords(budgetExpenseClassTotals.size());
  }

  private List<BudgetExpenseClassTotal> buildBudgetExpenseClassesTotals(Map<ExpenseClass, List<Transaction>> groupedByExpenseClass,
                                                                        double totalExpended, double totalCredited) {
    return groupedByExpenseClass.entrySet().stream()
      .map(entry -> buildBudgetExpenseClassTotals(entry.getKey(), entry.getValue(), totalExpended, totalCredited))
      .collect(Collectors.toList());
  }

  private BudgetExpenseClassTotal buildBudgetExpenseClassTotals(ExpenseClass expenseClass, List<Transaction> transactions,
                                                                double totalExpended, double totalCredited) {
    double credited = 0d;
    double expended = 0d;
    double encumbered = 0d;
    double awaitingPayment = 0d;
    Double expendedPercentage = 0d;
    Double creditedPercentage = 0d;

    if (CollectionUtils.isNotEmpty(transactions)) {
      RecalculatedBudgetBuilder budgetBuilder = new RecalculatedBudgetBuilder(transactions);
      SharedBudget recalculatedBudget = budgetBuilder.withEncumbered().withAwaitingPayment().withExpended().withCredited().build();

      expended = recalculatedBudget.getExpenditures();
      credited = recalculatedBudget.getCredits();
      encumbered = recalculatedBudget.getEncumbered();
      awaitingPayment = recalculatedBudget.getAwaitingPayment();

      CurrencyUnit currency = Monetary.getCurrency(transactions.getFirst().getCurrency());
      expendedPercentage = totalExpended == 0 ? null : calculateExpendedPercentage(Money.of(recalculatedBudget.getExpenditures(), currency), totalExpended);
      creditedPercentage = totalCredited == 0 ? null : calculateCreditedPercentage(Money.of(recalculatedBudget.getCredits(), currency), totalCredited);
    }

    String expenseClassName = getExpenseClassName(expenseClass);
    return new BudgetExpenseClassTotal()
      .withId(expenseClass.getId())
      .withExpenseClassName(expenseClassName)
      .withEncumbered(encumbered)
      .withAwaitingPayment(awaitingPayment)
      .withExpended(expended)
      .withPercentageExpended(expendedPercentage)
      .withCredited(credited)
      .withPercentageCredited(creditedPercentage);
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
