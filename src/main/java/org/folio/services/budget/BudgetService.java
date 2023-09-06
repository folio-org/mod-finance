package org.folio.services.budget;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.util.BudgetUtils;

import io.vertx.core.Future;

public class BudgetService {

  private final RestClient restClient;
  private final BudgetExpenseClassService budgetExpenseClassService;

  public BudgetService(RestClient restClient, BudgetExpenseClassService budgetExpenseClassService) {
    this.restClient = restClient;
    this.budgetExpenseClassService = budgetExpenseClassService;
  }

  public Future<BudgetsCollection> getBudgets(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(BUDGETS_STORAGE))
      .withLimit(limit)
      .withOffset(offset)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), BudgetsCollection.class, requestContext);
  }

  public Future<SharedBudget> getBudgetById(String budgetId, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(BUDGETS_STORAGE, budgetId), Budget.class, requestContext)
      .compose(budget -> budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContext)
        .map(budgetExpenseClasses -> BudgetUtils.buildSharedBudget(budget, budgetExpenseClasses)));
  }

  public Future<Void> updateBudget(SharedBudget sharedBudget, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(BUDGETS_STORAGE, sharedBudget.getId()), Budget.class, requestContext)
      .compose(budgetFromStorage -> {
          SharedBudget updatedSharedBudget = mergeBudgets(sharedBudget, budgetFromStorage);
          validateBudget(updatedSharedBudget);
          return restClient.put(resourceByIdPath(BUDGETS_STORAGE, updatedSharedBudget.getId()), BudgetUtils.convertToBudget(updatedSharedBudget), requestContext)
            .compose(aVoid -> budgetExpenseClassService.updateBudgetExpenseClassesLinks(updatedSharedBudget, requestContext)
              .recover(t -> rollbackBudgetPutIfNeeded(budgetFromStorage, t, requestContext))
              .mapEmpty());

      });
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
      .withNetTransfers(budgetFromStorage.getNetTransfers())
      .withTotalFunding(budgetFromStorage.getAllocated() + budgetFromStorage.getNetTransfers());
  }

  private Future<Void> rollbackBudgetPutIfNeeded(Budget budgetFromStorage, Throwable t, RequestContext requestContext) {
    if (t == null) {
      return succeededFuture(null);
    }
    return restClient.get(resourceByIdPath(BUDGETS_STORAGE, budgetFromStorage.getId()), Budget.class,requestContext)
      .map(latestBudget -> {
        budgetFromStorage.setVersion(latestBudget.getVersion());
        return null;
      })
      .compose(v -> restClient.put(resourceByIdPath(BUDGETS_STORAGE, budgetFromStorage.getId()), budgetFromStorage, requestContext))
      .recover(v -> Future.failedFuture(t))
      .compose(v -> Future.failedFuture(t));
  }

  public Future<Void> deleteBudget(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(BUDGETS_STORAGE, id), requestContext);
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
    BigDecimal encumbered = BigDecimal.valueOf(budget.getEncumbered());
    BigDecimal expenditures = BigDecimal.valueOf(budget.getExpenditures());
    BigDecimal awaitingPayment = BigDecimal.valueOf(budget.getAwaitingPayment());
    BigDecimal totalFunding = BigDecimal.valueOf(budget.getTotalFunding());

    //[remaining amount we can encumber] = (allocated * allowableEncumbered) - (encumbered + awaitingPayment + expended)
    if (budget.getAllowableEncumbrance() != null) {
      BigDecimal newAllowableEncumbrance = BigDecimal.valueOf(budget.getAllowableEncumbrance()).movePointLeft(2);
      if (totalFunding.multiply(newAllowableEncumbrance).compareTo(encumbered.add(awaitingPayment).add(expenditures)) < 0) {
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
