package org.folio.rest.helper;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.GROUP_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.FUNDS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.FundTypesCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.services.budget.BudgetService;
import org.folio.services.fund.FundService;
import org.folio.services.group.GroupFundFiscalYearService;
import org.folio.services.group.GroupService;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import one.util.streamex.StreamEx;

public class FundsHelper extends AbstractHelper {

  @Autowired
  private RestClient restClient;
  @Autowired
  private GroupFundFiscalYearService groupFundFiscalYearService;
  @Autowired
  private LedgerDetailsService ledgerDetailsService;
  @Autowired
  FundService fundService;
  @Autowired
  BudgetService budgetService;
  @Autowired
  GroupService groupService;

  public FundsHelper(Map<String, String> okapiHeaders, Context ctx) {
    super(okapiHeaders, ctx);
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  public Future<FundType> createFundType(FundType fundType, RequestContext requestContext) {
    return fundService.createFundType(fundType, requestContext);
  }

  public Future<FundTypesCollection> getFundTypes(int limit, int offset, String query, RequestContext requestContext) {
    return fundService.getFundTypes(limit, offset, query, requestContext);
  }

  public Future<FundType> getFundType(String id, RequestContext requestContext) {
    return fundService.getFundTypeById(id, requestContext);
  }

  public Future<Void> updateFundType(FundType fundType, RequestContext requestContext) {
    return fundService.updateFundType(fundType, requestContext);
  }

  public Future<Void> deleteFundType(String id, RequestContext requestContext) {
    return fundService.deleteFundType(id, requestContext);
  }

  public Future<CompositeFund> createFund(CompositeFund compositeFund, RequestContext requestContext) {
    if (CollectionUtils.isNotEmpty(compositeFund.getGroupIds())) {
      return ledgerDetailsService.getCurrentFiscalYear(compositeFund.getFund().getLedgerId(), new RequestContext(ctx, okapiHeaders))
        .compose(fiscalYear -> {
          if (Objects.isNull(fiscalYear)) {
            throw new HttpException(422, FISCAL_YEARS_NOT_FOUND);
          }
          return fundService.createFund(compositeFund.getFund(), requestContext)
            .map(newFund -> {
              compositeFund.getFund().withId(newFund.getId());
              return null;
            })
            .compose(compFund -> assignFundToGroups(compositeFund, fiscalYear.getId()));
        })
        .map(aVoid -> compositeFund);
    }
    return fundService.createFund(compositeFund.getFund(), requestContext)
      .map(compositeFund::withFund);
  }

  private Future<Void> assignFundToGroups(CompositeFund compositeFund, String fiscalYearId) {
    List<GroupFundFiscalYear> groupFundFiscalYears = buildGroupFundFiscalYears(compositeFund, fiscalYearId);
    var futures = groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> groupFundFiscalYearService.createGroupFundFiscalYear(groupFundFiscalYear, new RequestContext(ctx, okapiHeaders)))
      .toList();
    return GenericCompositeFuture.join(futures)
      .mapEmpty();
  }

  private List<GroupFundFiscalYear> buildGroupFundFiscalYears(CompositeFund compositeFund, String budgetId, String fiscalYearId, List<String> groupIds) {
    return StreamEx.of(groupIds)
      .map(groupId -> buildGroupFundFiscalYear(compositeFund, budgetId, fiscalYearId, groupId))
      .toList();
  }

  private List<GroupFundFiscalYear> buildGroupFundFiscalYears(CompositeFund compositeFund, String fiscalYearId) {
    return buildGroupFundFiscalYears(compositeFund, null, fiscalYearId, compositeFund.getGroupIds());
  }

  private GroupFundFiscalYear buildGroupFundFiscalYear(CompositeFund compositeFund, String budgetId, String fiscalYearId, String groupId) {
    return new GroupFundFiscalYear().withGroupId(groupId)
      .withBudgetId(budgetId)
      .withFundId(compositeFund.getFund().getId())
      .withFiscalYearId(fiscalYearId);
  }

  public Future<CompositeFund> getCompositeFund(String id, RequestContext requestContext) {
    return fundService.getFundById(id, requestContext)
      .map(fund -> new CompositeFund().withFund(fund))
      .compose(compositeFund -> ledgerDetailsService.getCurrentFiscalYear(compositeFund.getFund().getLedgerId(), new RequestContext(ctx, okapiHeaders))
          .compose(currentFY -> Objects.isNull(currentFY) ? succeededFuture(null)
              : getGroupIdsThatFundBelongs(id, currentFY.getId()))
          .map(compositeFund::withGroupIds));
  }

  private Future<Void> assignFundToGroups(List<GroupFundFiscalYear> groupFundFiscalYears) {
    var futures = groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> groupFundFiscalYearService.createGroupFundFiscalYear(groupFundFiscalYear, new RequestContext(ctx, okapiHeaders)))
      .toList();

    return GenericCompositeFuture.join(futures)
      .mapEmpty();
  }

  private Future<Void> unassignGroupsForFund(Collection<String> groupFundFiscalYearIds) {
    var futures = groupFundFiscalYearIds.stream()
      .map(id -> groupFundFiscalYearService.deleteGroupFundFiscalYear(id, new RequestContext(ctx, okapiHeaders)))
      .toList();
    return GenericCompositeFuture.join(futures)
      .mapEmpty();
  }

  private Future<List<String>> getGroupIdsThatFundBelongs(String fundId, String currentFYId) {
    return groupFundFiscalYearService.getGroupFundFiscalYearCollection(fundId, currentFYId, new RequestContext(ctx, okapiHeaders))
      .map(groupFundFiscalYearCollection -> groupFundFiscalYearCollection.getGroupFundFiscalYears()
        .stream()
        .map(GroupFundFiscalYear::getGroupId)
        .collect(toList()));
  }

  private Future<List<GroupFundFiscalYear>> getGroupFundFiscalYearsThatFundBelongs(String fundId, String currentFYId) {
    return groupFundFiscalYearService.getGroupFundFiscalYearCollection(fundId, currentFYId, new RequestContext(ctx, okapiHeaders))
      .map(groupFundFiscalYearCollection -> new ArrayList<>(groupFundFiscalYearCollection.getGroupFundFiscalYears()));
  }

  private List<String> groupFundFiscalYearIdsForDeletion(List<GroupFundFiscalYear> groupFundFiscalYearCollection, List<String> groupIdsForDeletion) {
    return groupFundFiscalYearCollection
      .stream()
      .filter(item -> groupIdsForDeletion.contains(item.getGroupId()))
      .map(GroupFundFiscalYear::getId)
      .collect(toList());
  }

  private Future<Void> createGroupFundFiscalYears(CompositeFund compositeFund, String currentFiscalYearId, List<String> groupIdsForCreation, RequestContext requestContext) {
    if(CollectionUtils.isNotEmpty(groupIdsForCreation)) {
      return groupService.getGroups(0, 0, convertIdsToCqlQuery(groupIdsForCreation), requestContext)
        .compose(groupsCollection -> {
          if(groupsCollection.getTotalRecords() == groupIdsForCreation.size()) {
            String query = getBudgetsCollectionQuery(currentFiscalYearId, compositeFund.getFund().getId());
            return budgetService.getBudgets(query, 0, 1, new RequestContext(ctx, okapiHeaders))
                .compose(budgetsCollection -> {
                  List<Budget> budgets = budgetsCollection.getBudgets();
                  String budgetId = null;
                  if(!budgets.isEmpty()) {
                    budgetId = budgets.get(0).getId();
                  }
                  return assignFundToGroups(buildGroupFundFiscalYears(compositeFund, budgetId, currentFiscalYearId, groupIdsForCreation));
                });
          } else {
            throw new HttpException(422, GROUP_NOT_FOUND);
          }
        });
    } else {
      return succeededFuture(null);
    }
  }

  private Future<Void> deleteGroupFundFiscalYears(List<String> groupFundFiscalYearForDeletionIds) {
    if(CollectionUtils.isNotEmpty(groupFundFiscalYearForDeletionIds)) {
      return unassignGroupsForFund(groupFundFiscalYearForDeletionIds);
    } else {
      return succeededFuture(null);
    }
  }

  public Future<Void> updateFund(CompositeFund compositeFund, RequestContext requestContext) {
    return fundService.getFundById(compositeFund.getFund().getId(), requestContext)
      .compose(fundFromStorage -> fundService.updateFund(compositeFund.getFund(), requestContext)
        .compose(v1 -> updateFundGroups(compositeFund, requestContext))
        .recover(t -> rollbackFundPutIfNeeded(fundFromStorage, t, requestContext)))
      .mapEmpty();

  }

  private Future<Void> updateFundGroups(CompositeFund compositeFund, RequestContext requestContext) {
    Fund fund = compositeFund.getFund();
    Set<String> groupIds = new HashSet<>(compositeFund.getGroupIds());
    return ledgerDetailsService.getCurrentFiscalYear(fund.getLedgerId(), new RequestContext(ctx, okapiHeaders))
      .compose(currentFiscalYear-> {
        if(Objects.nonNull(currentFiscalYear)) {
          String currentFiscalYearId = currentFiscalYear.getId();
          return getGroupFundFiscalYearsThatFundBelongs(fund.getId(), currentFiscalYearId)
            .compose(groupFundFiscalYearCollection -> {
              List<String> groupIdsFromStorage = StreamEx.of(groupFundFiscalYearCollection).map(GroupFundFiscalYear::getGroupId).toList();

              return createGroupFundFiscalYears(compositeFund, currentFiscalYearId, getSetDifference(groupIdsFromStorage, groupIds), requestContext)
                .compose(vVoid -> deleteGroupFundFiscalYears(groupFundFiscalYearIdsForDeletion(
                  groupFundFiscalYearCollection, getSetDifference(groupIds, groupIdsFromStorage))));
            });
        } else if(groupIds.isEmpty()) {
          return succeededFuture(null);
        } else {
          throw new HttpException(422, FISCAL_YEARS_NOT_FOUND);
        }
      });
  }

  private Future<Void> rollbackFundPutIfNeeded(Fund fundFromStorage, Throwable t, RequestContext requestContext) {
    if (t == null) {
      return succeededFuture(null);
    }
    return fundService.getFundById(fundFromStorage.getId(), requestContext)
      .map(latestFund -> fundFromStorage.withVersion(latestFund.getVersion()))
      .compose(v -> restClient.put(resourceByIdPath(FUNDS_STORAGE, fundFromStorage.getId()), fundFromStorage, requestContext))
      .recover(v -> Future.failedFuture(t))
      .compose(v -> Future.failedFuture(t));
  }

  private String getBudgetsCollectionQuery(String currentFiscalYearId, String fundId) {
    return String.format("fundId=%s AND fiscalYearId=%s", fundId, currentFiscalYearId);
  }

  public Future<Void> deleteFund(String id, RequestContext requestContext) {
    String query = String.format("fundId==%s", id);
    return groupFundFiscalYearService.getGroupFundFiscalYears(query, 0, Integer.MAX_VALUE, new RequestContext(ctx, okapiHeaders))
      .map(collection -> collection.getGroupFundFiscalYears().stream().map(GroupFundFiscalYear::getId).collect(toSet()))
      .compose(this::unassignGroupsForFund)
      .compose(vVoid -> restClient.delete(resourceByIdPath(FUNDS_STORAGE, id), requestContext));
  }

  /**
   * This method returns the set difference of B and A - the set of elements in B but not in A
   * @param a set A
   * @param b set B
   * @return the relative complement of A in B
   */
  public static List<String> getSetDifference(Collection<String> a, Collection<String> b) {
    return b.stream()
      .filter(item -> !a.contains(item))
      .toList();
  }

}
