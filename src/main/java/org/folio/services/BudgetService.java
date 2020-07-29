package org.folio.services;

import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.util.ErrorCodes;

import io.vertx.core.json.JsonObject;

public class BudgetService {

  private RestClient budgetRestClient;
  private TransactionService transactionService;
  private BudgetExpenseClassService budgetExpenseClassService;
  private GroupFundFiscalYearService groupFundFiscalYearService;

  public BudgetService(RestClient budgetRestClient,
                       TransactionService transactionService,
                       BudgetExpenseClassService budgetExpenseClassService,
                       GroupFundFiscalYearService groupFundFiscalYearService) {
    this.budgetRestClient = budgetRestClient;
    this.transactionService = transactionService;
    this.budgetExpenseClassService = budgetExpenseClassService;
    this.groupFundFiscalYearService = groupFundFiscalYearService;
  }

  public CompletableFuture<BudgetsCollection> getBudgets(String query, int offset, int limit, RequestContext requestContext) {
    return budgetRestClient.get(query, offset, limit, requestContext, BudgetsCollection.class);
  }

  public CompletableFuture<SharedBudget> getBudgetById(String budgetId, RequestContext requestContext) {
    return budgetRestClient.getById(budgetId, requestContext, Budget.class)
      .thenCompose(budget -> budgetExpenseClassService.getBudgetExpenseClasses(budgetId, requestContext)
      .thenApply(budgetExpenseClasses -> buildSharedBudget(budget, budgetExpenseClasses)));
  }

  public CompletableFuture<SharedBudget> createBudget(SharedBudget sharedBudget, RequestContext requestContext) {
    double allocatedValue = sharedBudget.getAllocated();
    sharedBudget.setAllocated(0d);
    return budgetRestClient.post(convertToBudget(sharedBudget), requestContext, Budget.class)
      .thenCompose(createdBudget -> allocateToBudget(createdBudget.withAllocated(allocatedValue), requestContext))
      .thenCompose(createdBudget -> groupFundFiscalYearService.updateBudgetIdForGroupFundFiscalYears(createdBudget, requestContext)
        .thenCompose(aVoid -> budgetExpenseClassService.createBudgetExpenseClasses(sharedBudget, requestContext))
        .thenApply(aVoid -> convertToSharedBudget(createdBudget).withStatusExpenseClasses(sharedBudget.getStatusExpenseClasses())));
  }


  public CompletableFuture<Void> updateBudget(SharedBudget sharedBudget, RequestContext requestContext) {
    return validateBudget(sharedBudget)
      .thenCompose(aVoid -> budgetExpenseClassService.updateBudgetExpenseClassesLinks(sharedBudget, requestContext))
      .thenCompose(aVoid -> budgetRestClient.put(sharedBudget.getId(), convertToBudget(sharedBudget), requestContext));
  }

  public CompletableFuture<Void> deleteBudget(String id, RequestContext requestContext) {
    return budgetRestClient.delete(id, requestContext);
  }

  private CompletableFuture<Budget> allocateToBudget(Budget createdBudget, RequestContext requestContext) {
    if (createdBudget.getAllocated() > 0d) {
      return transactionService.createAllocationTransaction(createdBudget, requestContext)
        .thenApply(transaction -> createdBudget)
        .exceptionally(e -> {
          throw new HttpException(500, ErrorCodes.ALLOCATION_TRANSFER_FAILED);
        });
    }
    return CompletableFuture.completedFuture(createdBudget);
  }

  private CompletableFuture<Void> validateBudget(Budget budget) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    List<Error> errors = new ArrayList<>();

    errors.addAll(checkRemainingEncumbrance(budget));
    errors.addAll(checkRemainingExpenditure(budget));

    if (!errors.isEmpty()) {
      future.completeExceptionally(new HttpException(400, new Errors()
        .withErrors(errors)
        .withTotalRecords(errors.size())));
    } else {
      future.complete(null);
    }
    return future;
  }

  private List<Error> checkRemainingEncumbrance(Budget budget) {
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

  private List<Error> checkRemainingExpenditure(Budget budget) {
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

  private SharedBudget buildSharedBudget(Budget budget, List<BudgetExpenseClass> budgetExpenseClasses) {
      List<StatusExpenseClass> statusExpenseClasses = budgetExpenseClasses.stream()
      .map(this::buildStatusExpenseClass)
      .collect(Collectors.toList());
    return convertToSharedBudget(budget).withStatusExpenseClasses(statusExpenseClasses);
  }

  private StatusExpenseClass buildStatusExpenseClass(BudgetExpenseClass budgetExpenseClass) {
    return new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClass.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.fromValue(budgetExpenseClass.getStatus().value()));
  }

  private SharedBudget convertToSharedBudget(Budget budget) {
    return JsonObject.mapFrom(budget).mapTo(SharedBudget.class);
  }

  private Budget convertToBudget(SharedBudget budget) {
    JsonObject jsonSharedBudget =  JsonObject.mapFrom(budget);
    jsonSharedBudget.remove("statusExpenseClasses");
    return jsonSharedBudget.mapTo(Budget.class);
  }

}
