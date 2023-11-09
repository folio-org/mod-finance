package org.folio.services.budget;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.Integer.MAX_VALUE;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.BUDGET_EXPENSE_CLASSES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.models.BudgetExpenseClassHolder;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.services.transactions.CommonTransactionService;

import io.vertx.core.Future;

public class BudgetExpenseClassService{

  private final RestClient restClient;
  private final CommonTransactionService transactionService;

  public BudgetExpenseClassService(RestClient restClient, CommonTransactionService transactionService) {
    this.restClient = restClient;
    this.transactionService = transactionService;
  }

  public Future<List<BudgetExpenseClass>> getBudgetExpenseClasses(String budgetId, RequestContext requestContext) {
    String query = String.format("budgetId==%s", budgetId);
    var requestEntry = new RequestEntry(resourcesPath(BUDGET_EXPENSE_CLASSES))
      .withQuery(query)
      .withLimit(MAX_VALUE)
      .withOffset(0);
    return restClient.get(requestEntry.buildEndpoint(), BudgetExpenseClassCollection.class, requestContext)
      .map(BudgetExpenseClassCollection::getBudgetExpenseClasses);
  }

  public Future<Void> createBudgetExpenseClasses(SharedBudget sharedBudget, RequestContext requestContext) {
    if (!CollectionUtils.isEmpty(sharedBudget.getStatusExpenseClasses())) {
      List<BudgetExpenseClass> createList = sharedBudget.getStatusExpenseClasses().stream()
        .map(statusExpenseClass -> buildBudgetExpenseClass(statusExpenseClass, sharedBudget.getId()))
        .collect(Collectors.toList());
      return createBudgetExpenseClasses(createList, requestContext);
    }
    return succeededFuture(null);
  }

  public Future<Void> updateBudgetExpenseClassesLinks(SharedBudget sharedBudget, RequestContext requestContext) {

    return getBudgetExpenseClasses(sharedBudget.getId(), requestContext)
      .map(budgetExpenseClasses -> mapBudgetExpenseClassesByOperation(budgetExpenseClasses, sharedBudget))
      .compose(budgetExpenseClassHolder -> createBudgetExpenseClasses(budgetExpenseClassHolder.getCreateList(), requestContext)
        .compose(aVoid -> updateBudgetExpenseClasses(budgetExpenseClassHolder.getUpdateList(), requestContext))
        .compose(aVoid -> deleteBudgetExpenseClasses(budgetExpenseClassHolder.getDeleteList(), sharedBudget, requestContext)));
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

  private Future<Void> deleteBudgetExpenseClasses(List<BudgetExpenseClass> deleteList, SharedBudget budget, RequestContext requestContext) {
    if (deleteList.isEmpty()) {
      return succeededFuture(null);
    }
    return checkNoTransactionsAssigned(deleteList, budget, requestContext)
      .compose(v -> GenericCompositeFuture.all(deleteList.stream()
        .map(budgetExpenseClass -> restClient.delete(resourceByIdPath(BUDGET_EXPENSE_CLASSES, budgetExpenseClass.getId()), requestContext))
        .collect(Collectors.toList())))
      .mapEmpty();
  }

  private Future<Void> checkNoTransactionsAssigned(List<BudgetExpenseClass> deleteList, SharedBudget budget, RequestContext requestContext) {
    return transactionService.retrieveTransactions(deleteList, budget, requestContext)
      .map(transactions -> {
        if (isNotEmpty(transactions)) {
          throw new HttpException(400, TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR);
        }
        return null;
      });
  }

  private Future<Void> updateBudgetExpenseClasses(List<BudgetExpenseClass> updateList, RequestContext requestContext) {
    if (updateList.isEmpty()) {
      return succeededFuture(null);
    }
    return GenericCompositeFuture.all(updateList.stream()
      .map(budgetExpenseClass -> restClient.put(resourceByIdPath(BUDGET_EXPENSE_CLASSES, budgetExpenseClass.getId()), budgetExpenseClass, requestContext))
      .collect(Collectors.toList()))
      .mapEmpty();
  }

  private Future<Void> createBudgetExpenseClasses(List<BudgetExpenseClass> createList, RequestContext requestContext) {
    if (createList.isEmpty()) {
      return succeededFuture(null);
    }
    var futures = createList.stream()
      .map(budgetExpenseClass -> restClient.post(resourcesPath(BUDGET_EXPENSE_CLASSES), budgetExpenseClass, BudgetExpenseClass.class, requestContext))
      .collect(Collectors.toList());
    return GenericCompositeFuture.join(futures)
      .mapEmpty();
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

  public Future<List<BudgetExpenseClass>> getBudgetExpenseClasses(List<String> budgetsIds, RequestContext requestContext) {
    return collectResultsOnSuccess(ofSubLists(new ArrayList<>(budgetsIds), MAX_IDS_FOR_GET_RQ).map(ids -> getBudgetExpenseClassesByIds(ids, requestContext)).collect(Collectors.toList()))
      .map(lists -> lists.stream()
        .flatMap(Collection::stream)
        .collect(toList()));
  }

  public  Future<List<BudgetExpenseClass>> getBudgetExpenseClassesByIds(List<String> ids, RequestContext requestContext) {
    String budgetId = "budgetId";
    String query = convertIdsToCqlQuery(ids, budgetId);
    var requestEntry = new RequestEntry(resourcesPath(BUDGET_EXPENSE_CLASSES))
      .withQuery(query)
      .withOffset(0)
      .withLimit(MAX_IDS_FOR_GET_RQ);
    return restClient.get(requestEntry.buildEndpoint(), BudgetExpenseClassCollection.class, requestContext)
      .map(BudgetExpenseClassCollection::getBudgetExpenseClasses);
  }
}
