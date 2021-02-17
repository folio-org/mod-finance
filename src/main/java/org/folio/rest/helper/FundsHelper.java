package org.folio.rest.helper;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.GROUP_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.HelperUtils.getSetDifference;
import static org.folio.rest.util.ResourcePathResolver.FUNDS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.FUND_TYPES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.FundTypesCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.services.group.GroupFundFiscalYearService;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.folio.completablefuture.FolioVertxCompletableFuture;
import one.util.streamex.StreamEx;

public class FundsHelper extends AbstractHelper {

  private static final String GET_FUND_TYPES_BY_QUERY = resourcesPath(FUND_TYPES) + SEARCH_PARAMS;

  @Autowired
  private RestClient budgetRestClient;
  @Autowired
  private GroupFundFiscalYearService groupFundFiscalYearService;
  @Autowired
  private LedgerService ledgerService;
  @Autowired
  private LedgerDetailsService ledgerDetailsService;

  private GroupsHelper groupsHelper;

  public FundsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
    groupsHelper = new GroupsHelper(httpClient, okapiHeaders, ctx, lang);
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  public CompletableFuture<FundType> createFundType(FundType fundType) {
    return handleCreateRequest(resourcesPath(FUND_TYPES), fundType).thenApply(fundType::withId);
  }

  public CompletableFuture<FundTypesCollection> getFundTypes(int limit, int offset, String query) {
    String endpoint = String.format(GET_FUND_TYPES_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenCompose(json -> FolioVertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(FundTypesCollection.class)));
  }

  public CompletableFuture<FundType> getFundType(String id) {
    return handleGetRequest(resourceByIdPath(FUND_TYPES, id, lang))
      .thenApply(json -> json.mapTo(FundType.class));
  }

  public CompletableFuture<Void> updateFundType(FundType fundType) {
    return handleUpdateRequest(resourceByIdPath(FUND_TYPES, fundType.getId(), lang), fundType);
  }

  public CompletableFuture<Void> deleteFundType(String id) {
    return handleDeleteRequest(resourceByIdPath(FUND_TYPES, id, lang));
  }

  public CompletableFuture<CompositeFund> createFund(CompositeFund compositeFund) {
    final Fund fund = compositeFund.getFund();
    if (CollectionUtils.isNotEmpty(compositeFund.getGroupIds())) {
      return ledgerDetailsService.getCurrentFiscalYear(fund.getLedgerId(), new RequestContext(ctx, okapiHeaders))
        .thenCompose(fiscalYear -> {
          if (Objects.isNull(fiscalYear)) {
            throw new HttpException(422, FISCAL_YEARS_NOT_FOUND);
          }
          return handleCreateRequest(resourcesPath(FUNDS_STORAGE), fund).thenAccept(fund::setId)
            .thenCompose(ok -> assignFundToGroups(compositeFund, fiscalYear.getId()));
        })
        .thenApply(aVoid -> compositeFund);
    }
    return handleCreateRequest(resourcesPath(FUNDS_STORAGE), fund)
      .thenApply(id -> {
        fund.setId(id);
        return compositeFund;
      });
  }

  private CompletableFuture<Void> assignFundToGroups(CompositeFund compositeFund, String fiscalYearId) {
    List<GroupFundFiscalYear> groupFundFiscalYears = buildGroupFundFiscalYears(compositeFund, fiscalYearId);
    return FolioVertxCompletableFuture.allOf(ctx, groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> groupFundFiscalYearService.createGroupFundFiscalYear(groupFundFiscalYear, new RequestContext(ctx, okapiHeaders)))
      .toArray(CompletableFuture[]::new));
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

  public CompletableFuture<CompositeFund> getCompositeFund(String id) {
    return handleGetRequest(resourceByIdPath(FUNDS_STORAGE, id, lang))
      .thenApply(json -> new CompositeFund().withFund(json.mapTo(Fund.class)))
      .thenCompose(compositeFund -> ledgerDetailsService.getCurrentFiscalYear(compositeFund.getFund().getLedgerId(), new RequestContext(ctx, okapiHeaders))
          .thenCompose(currentFY -> Objects.isNull(currentFY) ? CompletableFuture.completedFuture(null)
              : getGroupIdsThatFundBelongs(id, currentFY.getId()))
          .thenApply(compositeFund::withGroupIds));
  }

  public CompletableFuture<Fund> getFund(String id) {
    return handleGetRequest(resourceByIdPath(FUNDS_STORAGE, id, lang))
      .thenApply(json -> json.mapTo(Fund.class));
  }

  private CompletableFuture<Void> assignFundToGroups(List<GroupFundFiscalYear> groupFundFiscalYears) {
    return FolioVertxCompletableFuture.allOf(ctx, groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> groupFundFiscalYearService.createGroupFundFiscalYear(groupFundFiscalYear, new RequestContext(ctx, okapiHeaders)))
      .toArray(CompletableFuture[]::new));
  }

  private CompletableFuture<Void> unassignGroupsForFund(Collection<String> groupFundFiscalYearIds) {
    return FolioVertxCompletableFuture.allOf(ctx, groupFundFiscalYearIds.stream()
      .map(id -> groupFundFiscalYearService.deleteGroupFundFiscalYear(id, new RequestContext(ctx, okapiHeaders)))
      .toArray(CompletableFuture[]::new));
  }

  private CompletionStage<List<String>> getGroupIdsThatFundBelongs(String fundId, String currentFYId) {
    return groupFundFiscalYearService.getGroupFundFiscalYearCollection(fundId, currentFYId, new RequestContext(ctx, okapiHeaders))
      .thenApply(groupFundFiscalYearCollection -> groupFundFiscalYearCollection.getGroupFundFiscalYears()
        .stream()
        .map(GroupFundFiscalYear::getGroupId)
        .collect(toList()));
  }

  private CompletionStage<List<GroupFundFiscalYear>> getGroupFundFiscalYearsThatFundBelongs(String fundId, String currentFYId) {
    return groupFundFiscalYearService.getGroupFundFiscalYearCollection(fundId, currentFYId, new RequestContext(ctx, okapiHeaders))
      .thenApply(groupFundFiscalYearCollection -> new ArrayList<>(groupFundFiscalYearCollection.getGroupFundFiscalYears()));
  }

  private List<String> groupFundFiscalYearIdsForDeletion(List<GroupFundFiscalYear> groupFundFiscalYearCollection, List<String> groupIdsForDeletion) {
    return groupFundFiscalYearCollection.stream().filter(item -> groupIdsForDeletion.contains(item.getGroupId())).map(GroupFundFiscalYear::getId).collect(toList());
  }

  private CompletionStage<Void> createGroupFundFiscalYears(CompositeFund compositeFund, String currentFiscalYearId, List<String> groupIdsForCreation) {
    if(CollectionUtils.isNotEmpty(groupIdsForCreation)) {
      return groupsHelper.getGroups(0, 0, convertIdsToCqlQuery(groupIdsForCreation))
        .thenCompose(groupsCollection -> {
          if(groupsCollection.getTotalRecords() == groupIdsForCreation.size()) {
            String query = getBudgetsCollectionQuery(currentFiscalYearId, compositeFund.getFund().getId());
            return budgetRestClient.get(query, 0, 1, new RequestContext(ctx, okapiHeaders), BudgetsCollection.class)
                .thenCompose(budgetsCollection -> {
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
      return CompletableFuture.completedFuture(null);
    }
  }

  private CompletionStage<Void> deleteGroupFundFiscalYears(List<String> groupFundFiscalYearForDeletionIds) {
    if(CollectionUtils.isNotEmpty(groupFundFiscalYearForDeletionIds)) {
      return unassignGroupsForFund(groupFundFiscalYearForDeletionIds);
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  public CompletableFuture<Void> updateFund(CompositeFund compositeFund) {

    Fund fund = compositeFund.getFund();
    Set<String> groupIds = new HashSet<>(compositeFund.getGroupIds());

    return ledgerDetailsService.getCurrentFiscalYear(fund.getLedgerId(), new RequestContext(ctx, okapiHeaders))
      .thenCompose(currentFiscalYear-> {
        if(Objects.nonNull(currentFiscalYear)) {
          String currentFiscalYearId = currentFiscalYear.getId();
          return getGroupFundFiscalYearsThatFundBelongs(fund.getId(), currentFiscalYearId)
            .thenCompose(groupFundFiscalYearCollection -> {
              List<String> groupIdsFromStorage = StreamEx.of(groupFundFiscalYearCollection).map(GroupFundFiscalYear::getGroupId).toList();
              return createGroupFundFiscalYears(compositeFund, currentFiscalYearId, getSetDifference(groupIdsFromStorage, groupIds))
                .thenCompose(vVoid -> deleteGroupFundFiscalYears(groupFundFiscalYearIdsForDeletion(groupFundFiscalYearCollection, getSetDifference(groupIds, groupIdsFromStorage))));
            });
        } else if(groupIds.isEmpty()) {
          return CompletableFuture.completedFuture(null);
        } else {
          throw new HttpException(422, FISCAL_YEARS_NOT_FOUND);
        }
      }).thenCompose(vVoid -> handleUpdateRequest(resourceByIdPath(FUNDS_STORAGE, fund.getId(), lang), fund));
  }

  private String getBudgetsCollectionQuery(String currentFiscalYearId, String fundId) {
    return String.format("fundId=%s AND fiscalYearId=%s", fundId, currentFiscalYearId);
  }

  public CompletableFuture<Void> deleteFund(String id) {
    String query = String.format("fundId==%s", id);
    return groupFundFiscalYearService.getGroupFundFiscalYears(query, 0, Integer.MAX_VALUE, new RequestContext(ctx, okapiHeaders))
      .thenApply(collection -> collection.getGroupFundFiscalYears().stream().map(GroupFundFiscalYear::getId).collect(toSet()))
      .thenApply(this::unassignGroupsForFund)
      .thenCompose(vVoid -> handleDeleteRequest(resourceByIdPath(FUNDS_STORAGE, id, lang)));
  }

}
