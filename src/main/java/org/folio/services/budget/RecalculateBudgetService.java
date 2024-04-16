package org.folio.services.budget;

import io.vertx.core.Future;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.util.BudgetUtils;
import org.folio.services.transactions.TransactionService;

import java.util.List;

public class RecalculateBudgetService {
  private final BudgetService budgetService;
  private final TransactionService transactionService;

  public RecalculateBudgetService(BudgetService budgetService, TransactionService transactionService) {
    this.budgetService = budgetService;
    this.transactionService = transactionService;
  }

  public Future<Void> recalculateBudget(String budgetId, RequestContext requestContext) {
    return budgetService.getBudgetById(budgetId, requestContext)
      .compose(budget -> transactionService.getBudgetTransactions(BudgetUtils.convertToBudget(budget), requestContext)
        .map(transactions -> recalculateBudgetBasedOnTransactions(budget, transactions)))
      .compose(budget -> budgetService.updateBudgetWithAmountFields(budget, requestContext))
      .mapEmpty();
  }

  private SharedBudget recalculateBudgetBasedOnTransactions(SharedBudget budget, List<Transaction> transactions) {
    double initialAllocation = 0d;
    double allocationTo = 0d;
    double allocationFrom = 0d;
    double netTransfers = 0d;
    double encumbered = 0d;
    double awaitingPayment = 0d;
    double expended = 0d;

    if (CollectionUtils.isNotEmpty(transactions)) {
      String fundId = budget.getFundId();
      RecalculatedBudgetBuilder budgetBuilder = new RecalculatedBudgetBuilder(transactions);
      SharedBudget recalculatedBudget = budgetBuilder.withInitialAllocation(fundId).withAllocationTo(fundId)
        .withAllocationFrom(fundId).withNetTransfers(fundId).withEncumbered().withAwaitingPayment().withExpended().build();

      initialAllocation = recalculatedBudget.getInitialAllocation();
      allocationTo = recalculatedBudget.getAllocationTo();
      allocationFrom = recalculatedBudget.getAllocationFrom();
      netTransfers = recalculatedBudget.getNetTransfers();
      encumbered = recalculatedBudget.getEncumbered();
      awaitingPayment = recalculatedBudget.getAwaitingPayment();
      expended = recalculatedBudget.getExpenditures();
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

}
