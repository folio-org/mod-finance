package org.folio.services.budget;

import static java.lang.Integer.MAX_VALUE;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.models.BudgetExpenseClassHolder;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;

import org.folio.completablefuture.FolioVertxCompletableFuture;
import org.folio.services.transactions.CommonTransactionService;

public class BudgetExpenseClassService {

  private final RestClient budgetExpenseClassRestClient;
  private final CommonTransactionService transactionService;

  public BudgetExpenseClassService(RestClient budgetExpenseClassRestClient, CommonTransactionService transactionService) {
    this.budgetExpenseClassRestClient = budgetExpenseClassRestClient;
    this.transactionService = transactionService;
  }

  public CompletableFuture<List<BudgetExpenseClass>> getBudgetExpenseClasses(String budgetId, RequestContext requestContext) {
    String query = String.format("budgetId==%s", budgetId);
    return budgetExpenseClassRestClient.get(query, 0, MAX_VALUE, requestContext, BudgetExpenseClassCollection.class)
      .thenApply(BudgetExpenseClassCollection::getBudgetExpenseClasses);
  }

  public CompletableFuture<Void> createBudgetExpenseClasses(SharedBudget sharedBudget, RequestContext requestContext) {
    if (!CollectionUtils.isEmpty(sharedBudget.getStatusExpenseClasses())) {
      List<BudgetExpenseClass> createList = sharedBudget.getStatusExpenseClasses().stream()
        .map(statusExpenseClass -> buildBudgetExpenseClass(statusExpenseClass, sharedBudget.getId()))
        .collect(Collectors.toList());
      return createBudgetExpenseClasses(createList, requestContext);
    }
    return CompletableFuture.completedFuture(null);
  }

  public CompletableFuture<Void> updateBudgetExpenseClassesLinks(SharedBudget sharedBudget, RequestContext requestContext) {

    return getBudgetExpenseClasses(sharedBudget.getId(), requestContext)
      .thenApply(budgetExpenseClasses -> mapBudgetExpenseClassesByOperation(budgetExpenseClasses, sharedBudget))
      .thenCompose(budgetExpenseClassHolder -> deleteBudgetExpenseClasses(budgetExpenseClassHolder.getDeleteList(), sharedBudget, requestContext)
        .thenCompose(aVoid -> updateBudgetExpenseClasses(budgetExpenseClassHolder.getUpdateList(), requestContext))
        .thenCompose(aVoid -> createBudgetExpenseClasses(budgetExpenseClassHolder.getCreateList(), requestContext)));
  }

  private BudgetExpenseClassHolder mapBudgetExpenseClassesByOperation(List<BudgetExpenseClass> budgetExpenseClasses, SharedBudget sharedBudget) {
    Map<String, BudgetExpenseClass> map = budgetExpenseClasses.stream()
      .collect(toMap(BudgetExpenseClass::getExpenseClassId, identity()));
    BudgetExpenseClassHolder holder = new BudgetExpenseClassHolder();

    sharedBudget.getStatusExpenseClasses().forEach(statusExpenseClass -> {
      if (map.containsKey(statusExpenseClass.getExpenseClassId())) {
        BudgetExpenseClass budgetExpenseClass = map.get(statusExpenseClass.getExpenseClassId());
        if (isDifferentStatuses(statusExpenseClass, budgetExpenseClass)) {
          budgetExpenseClass.setStatus(BudgetExpenseClass.Status.fromValue(statusExpenseClass.getStatus().value()));
          holder.addToUpdateList(budgetExpenseClass);
        }
        map.remove(statusExpenseClass.getExpenseClassId());
      } else {
        holder.addToCreateList(buildBudgetExpenseClass(statusExpenseClass, sharedBudget.getId()));
      }
    });

    holder.addAllToDeleteList(map.values());

    return holder;
  }

  private CompletableFuture<Void> deleteBudgetExpenseClasses(List<BudgetExpenseClass> deleteList, SharedBudget budget, RequestContext requestContext) {
    if (deleteList.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    return checkNoTransactionsAssigned(deleteList, budget, requestContext)
      .thenCompose(vVoid -> FolioVertxCompletableFuture.allOf(requestContext.getContext(), deleteList.stream()
        .map(budgetExpenseClass -> budgetExpenseClassRestClient.delete(budgetExpenseClass.getId(), requestContext))
        .toArray(CompletableFuture[]::new)));
  }

  private CompletableFuture<Void> checkNoTransactionsAssigned(List<BudgetExpenseClass> deleteList, SharedBudget budget, RequestContext requestContext) {
    return transactionService.retrieveTransactions(deleteList, budget, requestContext)
      .thenAccept(transactions -> {
        if (isNotEmpty(transactions)) {
          throw new HttpException(400, TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR);
        }
      });
  }

  private CompletableFuture<Void> updateBudgetExpenseClasses(List<BudgetExpenseClass> updateList, RequestContext requestContext) {
    if (updateList.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    return FolioVertxCompletableFuture.allOf(requestContext.getContext(), updateList.stream()
      .map(budgetExpenseClass -> budgetExpenseClassRestClient.put(budgetExpenseClass.getId(), budgetExpenseClass, requestContext))
      .toArray(CompletableFuture[]::new));
  }

  private CompletableFuture<Void> createBudgetExpenseClasses(List<BudgetExpenseClass> createList, RequestContext requestContext) {
    if (createList.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    return FolioVertxCompletableFuture.allOf(requestContext.getContext(), createList.stream()
      .map(budgetExpenseClass -> budgetExpenseClassRestClient.post(budgetExpenseClass, requestContext, BudgetExpenseClass.class))
      .toArray(CompletableFuture[]::new));
  }

  private boolean isDifferentStatuses(StatusExpenseClass statusExpenseClass, BudgetExpenseClass budgetExpenseClass) {
    return !budgetExpenseClass.getStatus().value().equals(statusExpenseClass.getStatus().value());
  }

  public BudgetExpenseClass buildBudgetExpenseClass(StatusExpenseClass statusExpenseClass, String budgetId) {
    return new BudgetExpenseClass()
      .withBudgetId(budgetId)
      .withExpenseClassId(statusExpenseClass.getExpenseClassId())
      .withStatus(BudgetExpenseClass.Status.fromValue(statusExpenseClass.getStatus().value()));
  }
}
