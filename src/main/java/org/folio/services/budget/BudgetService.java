package org.folio.services.budget;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.util.BudgetUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;

public class BudgetService {

  private final RestClient budgetRestClient;
  private final BudgetExpenseClassService budgetExpenseClassService;

  public BudgetService(RestClient budgetRestClient, BudgetExpenseClassService budgetExpenseClassService) {
    this.budgetRestClient = budgetRestClient;
    this.budgetExpenseClassService = budgetExpenseClassService;
  }

  public CompletableFuture<BudgetsCollection> getBudgets(String query, int offset, int limit, RequestContext requestContext) {
    return budgetRestClient.get(query, offset, limit, requestContext, BudgetsCollection.class);
  }

  public CompletableFuture<SharedBudget> getBudgetById(String budgetId, RequestContext requestContext) {
    return budgetRestClient.getById(budgetId, requestContext, Budget.class)
      .thenCompose(budget -> budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContext)
      .thenApply(budgetExpenseClasses -> BudgetUtils.buildSharedBudget(budget, budgetExpenseClasses)));
  }

  public CompletableFuture<Void> updateBudget(SharedBudget sharedBudget, RequestContext requestContext) {
    return budgetRestClient.getById(sharedBudget.getId(), requestContext, Budget.class)
      .thenApply(budgetFromStorage -> {
        SharedBudget mergedBudget = mergeBudgets(sharedBudget, budgetFromStorage);
        validateBudget(mergedBudget);
        return mergedBudget;
      })
      .thenCompose(updatedSharedBudget -> budgetExpenseClassService.updateBudgetExpenseClassesLinks(updatedSharedBudget, requestContext)
        .thenCompose(aVoid -> budgetRestClient.put(updatedSharedBudget.getId(), BudgetUtils.convertToBudget(updatedSharedBudget), requestContext)));
  }

  private SharedBudget mergeBudgets(SharedBudget sharedBudget, Budget budgetFromStorage) {
    return sharedBudget
      .withAllocated(budgetFromStorage.getAllocated())
      .withAvailable(budgetFromStorage.getAvailable())
      .withUnavailable(budgetFromStorage.getUnavailable())
      .withInitialAllocation(budgetFromStorage.getInitialAllocation())
      .withAllocationTo(budgetFromStorage.getAllocationTo())
      .withAllocationFrom(budgetFromStorage.getAllocationFrom())
      .withAwaitingPayment(budgetFromStorage.getAwaitingPayment())
      .withExpenditures(budgetFromStorage.getExpenditures())
      .withEncumbered(budgetFromStorage.getEncumbered())
      .withOverEncumbrance(budgetFromStorage.getOverEncumbrance())
      .withOverExpended(budgetFromStorage.getOverExpended())
      .withNetTransfers(budgetFromStorage.getNetTransfers());
  }

  public CompletableFuture<Void> deleteBudget(String id, RequestContext requestContext) {
    return budgetRestClient.delete(id, requestContext);
  }

  private void validateBudget(SharedBudget budget) {
    List<Error> errors = new ArrayList<>();

    errors.addAll(checkRemainingEncumbrance(budget));
    errors.addAll(checkRemainingExpenditure(budget));

    if (!errors.isEmpty()) {
      throw new HttpException(422, new Errors()
        .withErrors(errors)
        .withTotalRecords(errors.size()));
    }
  }

  private List<Error> checkRemainingEncumbrance(SharedBudget budget) {
    BigDecimal allocated = BigDecimal.valueOf(budget.getAllocated());
    BigDecimal encumbered = BigDecimal.valueOf(budget.getEncumbered());
    BigDecimal expenditures = BigDecimal.valueOf(budget.getExpenditures());
    BigDecimal awaitingPayment = BigDecimal.valueOf(budget.getAwaitingPayment());

    //[remaining amount we can encumber] = (allocated * allowableEncumbered) - (encumbered + awaitingPayment + expended)
    if (budget.getAllowableEncumbrance() != null) {
      BigDecimal newAllowableEncumbrance = BigDecimal.valueOf(budget.getAllowableEncumbrance()).movePointLeft(2);
      if (allocated.multiply(newAllowableEncumbrance).compareTo(encumbered.add(awaitingPayment).add(expenditures)) < 0) {
        return Collections.singletonList(ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED.toError());
      }
    }
    return Collections.emptyList();
  }

  private List<Error> checkRemainingExpenditure(SharedBudget budget) {
    BigDecimal allocated = BigDecimal.valueOf(budget.getAllocated());
    BigDecimal expenditures = BigDecimal.valueOf(budget.getExpenditures());
    BigDecimal awaitingPayment = BigDecimal.valueOf(budget.getAwaitingPayment());
    BigDecimal available = BigDecimal.valueOf(budget.getAvailable());
    BigDecimal unavailable = BigDecimal.valueOf(budget.getUnavailable());

    //[amount we can expend] = (allocated * allowableExpenditure) - (allocated - (unavailable + available)) - (awaitingPayment + expended)
    if (budget.getAllowableExpenditure() != null) {
      BigDecimal newAllowableExpenditure = BigDecimal.valueOf(budget.getAllowableExpenditure())
        .movePointLeft(2);
      if (allocated.multiply(newAllowableExpenditure)
        .subtract(allocated.subtract(available.add(unavailable)))
        .subtract(expenditures.add(awaitingPayment))
        .compareTo(BigDecimal.ZERO) < 0) {
        return Collections.singletonList(ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED.toError());
      }
    }
    return Collections.emptyList();
  }
}
