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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

  private static final Logger log = LogManager.getLogger();

  private final RestClient restClient;
  private final BudgetExpenseClassService budgetExpenseClassService;

  public BudgetService(RestClient restClient, BudgetExpenseClassService budgetExpenseClassService) {
    this.restClient = restClient;
    this.budgetExpenseClassService = budgetExpenseClassService;
  }

  public Future<BudgetsCollection> getBudgets(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(BUDGETS_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), BudgetsCollection.class, requestContext);
  }

  public Future<SharedBudget> getBudgetById(String budgetId, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(BUDGETS_STORAGE, budgetId), Budget.class, requestContext)
      .compose(budget -> budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContext)
        .map(budgetExpenseClasses -> BudgetUtils.buildSharedBudget(budget, budgetExpenseClasses)));
  }

  public Future<Void> updateBudgetWithAmountFields(SharedBudget sharedBudget, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(BUDGETS_STORAGE, sharedBudget.getId()), BudgetUtils.convertToBudget(sharedBudget), requestContext);
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
      .withCredits(budgetFromStorage.getCredits())
      .withEncumbered(budgetFromStorage.getEncumbered())
      .withOverEncumbrance(budgetFromStorage.getOverEncumbrance())
      .withOverExpended(budgetFromStorage.getOverExpended())
      .withNetTransfers(budgetFromStorage.getNetTransfers())
      .withTotalFunding(budgetFromStorage.getAllocated() + budgetFromStorage.getNetTransfers());
  }

  private Future<Void> rollbackBudgetPutIfNeeded(Budget budgetFromStorage, Throwable t, RequestContext requestContext) {
    log.debug("rollbackBudgetPutIfNeeded:: Rolling back budget '{}' if needed", budgetFromStorage.getId());
    if (t == null) {
      log.info("rollbackBudgetPutIfNeeded:: There is no any throwable error for budget '{}'", budgetFromStorage.getId());
      return succeededFuture(null);
    }
    return restClient.get(resourceByIdPath(BUDGETS_STORAGE, budgetFromStorage.getId()), Budget.class, requestContext)
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
    log.debug("validateBudget:: Validation budget: {}", budget.getId());
    List<Error> errors = new ArrayList<>();

    errors.addAll(checkRemainingEncumbrance(budget));
    errors.addAll(checkRemainingExpenditure(budget));

    if (!errors.isEmpty()) {
      log.error("validateBudget:: '{}' Error(s) found during validation for budget: {}", errors.size(), budget.getId());
      throw new HttpException(422, new Errors()
        .withErrors(errors)
        .withTotalRecords(errors.size()));
    }
    log.info("validateBudget:: Budget '{}' is passed from validation", budget.getId());
  }

  private List<Error> checkRemainingEncumbrance(SharedBudget budget) {
    log.debug("checkRemainingEncumbrance:: Check remaining encumbrance for budget: {}", budget.getId());
    BigDecimal encumbered = BigDecimal.valueOf(budget.getEncumbered());
    BigDecimal expenditures = BigDecimal.valueOf(budget.getExpenditures());
    BigDecimal credits = BigDecimal.valueOf(budget.getCredits());
    BigDecimal awaitingPayment = BigDecimal.valueOf(budget.getAwaitingPayment());
    BigDecimal allocated = BigDecimal.valueOf(budget.getAllocated());
    BigDecimal netTransfers = BigDecimal.valueOf(budget.getNetTransfers());

    // [remaining amount we can encumber] = (allocated + netTransfers) * allowableEncumbered - (encumbered + awaitingPayment + expended - credits)
    // calculations should be consistent with BatchTransactionChecks in mod-finance-storage
    if (budget.getAllowableEncumbrance() != null) {
      log.info("checkRemainingEncumbrance:: Budget '{}' allowable encumbrance is not null", budget.getId());
      BigDecimal allowableEncumbrance = BigDecimal.valueOf(budget.getAllowableEncumbrance()).movePointLeft(2);
      BigDecimal totalFunding = allocated.add(netTransfers);
      // unavailable amount shouldn't be negative
      BigDecimal unavailable = ensureNonNegative(encumbered.add(awaitingPayment).add(expenditures).subtract(credits));

      double remaining = totalFunding.multiply(allowableEncumbrance).subtract(unavailable).doubleValue();
      if (remaining < 0) {
        log.error("checkRemainingEncumbrance:: Allowable encumbrance limit exceeded for budget: {}", budget.getId());
        return Collections.singletonList(ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED.toError());
      }
    }
    return Collections.emptyList();
  }

  private List<Error> checkRemainingExpenditure(SharedBudget budget) {
    log.debug("checkRemainingExpenditure:: Check remaining expenditure for budget: {}", budget.getId());
    BigDecimal allocated = BigDecimal.valueOf(budget.getAllocated());
    BigDecimal netTransfers = BigDecimal.valueOf(budget.getNetTransfers());
    BigDecimal expenditures = BigDecimal.valueOf(budget.getExpenditures());
    BigDecimal credits = BigDecimal.valueOf(budget.getCredits());
    BigDecimal awaitingPayment = BigDecimal.valueOf(budget.getAwaitingPayment());

    // [remaining amount we can expend] = (allocated + netTransfers) * allowableExpenditure - (awaitingPayment + expended - credited)
    // calculations should be consistent with BatchTransactionChecks in mod-finance-storage
    if (budget.getAllowableExpenditure() != null) {
      log.info("checkRemainingExpenditure:: Budget '{}' allowable expenditure is not null", budget.getId());
      BigDecimal allowableExpenditure = BigDecimal.valueOf(budget.getAllowableExpenditure()).movePointLeft(2);
      BigDecimal totalFunding  = allocated.add(netTransfers);
      // unavailable amount shouldn't be negative
      BigDecimal unavailable = ensureNonNegative(awaitingPayment.add(expenditures).subtract(credits));

      double remaining = totalFunding.multiply(allowableExpenditure).subtract(unavailable).doubleValue();
      if (remaining < 0) {
        log.error("checkRemainingExpenditure:: Allowable expenditure limit exceed for budget: {}", budget.getId());
        return Collections.singletonList(ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED.toError());
      }
    }
    return Collections.emptyList();
  }

  private BigDecimal ensureNonNegative(BigDecimal amount) {
    return amount.max(BigDecimal.ZERO);
  }
}
