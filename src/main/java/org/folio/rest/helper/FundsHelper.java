package org.folio.rest.helper;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static me.escoffier.vertx.completablefuture.VertxCompletableFuture.supplyBlockingAsync;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.GROUP_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.HelperUtils.getSetDifference;
import static org.folio.rest.util.HelperUtils.handleGetRequest;
import static org.folio.rest.util.ResourcePathResolver.FUNDS;
import static org.folio.rest.util.ResourcePathResolver.FUND_TYPES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import one.util.streamex.StreamEx;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.FundTypesCollection;
import org.folio.rest.jaxrs.model.FundsCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class FundsHelper extends AbstractHelper {

  private static final String GET_FUND_TYPES_BY_QUERY = resourcesPath(FUND_TYPES) + SEARCH_PARAMS;
  private static final String GET_FUNDS_BY_QUERY = resourcesPath(FUNDS) + SEARCH_PARAMS;
  public static final String SEARCH_CURRENT_FISCAL_YEAR_QUERY = "series==\"%s\" AND periodEnd>%s sortBy periodStart";

  private GroupsHelper groupsHelper;
  private GroupFundFiscalYearHelper groupFundFiscalYearHelper;

  public FundsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
    groupsHelper = new GroupsHelper(httpClient, okapiHeaders, ctx, lang);
    groupFundFiscalYearHelper = new GroupFundFiscalYearHelper(httpClient, okapiHeaders, ctx, lang);
  }

  public CompletableFuture<FundType> createFundType(FundType fundType) {
    return handleCreateRequest(resourcesPath(FUND_TYPES), fundType).thenApply(fundType::withId);
  }

  public CompletableFuture<FundTypesCollection> getFundTypes(int limit, int offset, String query) {
    String endpoint = String.format(GET_FUND_TYPES_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> supplyBlockingAsync(ctx, () -> json.mapTo(FundTypesCollection.class)));
  }

  public CompletableFuture<FundType> getFundType(String id) {
    return handleGetRequest(resourceByIdPath(FUND_TYPES, id, lang), httpClient, ctx, okapiHeaders, logger)
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
      return getCurrentFiscalYear(fund.getLedgerId())
        .thenCompose(fiscalYear -> {
          if (Objects.isNull(fiscalYear)) {
            throw new HttpException(422, FISCAL_YEARS_NOT_FOUND);
          }
          return handleCreateRequest(resourcesPath(FUNDS), fund).thenAccept(fund::setId)
            .thenCompose(ok -> assignFundToGroups(compositeFund, fiscalYear.getId()));
        })
        .thenApply(aVoid -> compositeFund);
    }
    return handleCreateRequest(resourcesPath(FUNDS), fund)
      .thenApply(id -> {
        fund.setId(id);
        return compositeFund;
      });
  }

  public CompletableFuture<FiscalYear> getCurrentFiscalYear(String ledgerId) {
    FiscalYearsHelper fiscalYearsHelper = new FiscalYearsHelper(httpClient, okapiHeaders, ctx, lang);
    return getTwoFirstFiscalYears(ledgerId, fiscalYearsHelper)
      .thenApply(twoFirstFiscalYears -> {
        if(CollectionUtils.isNotEmpty(twoFirstFiscalYears)) {
          if(twoFirstFiscalYears.size() > 1 && isOverlapped(twoFirstFiscalYears.get(0), twoFirstFiscalYears.get(1))) {
            return twoFirstFiscalYears.get(1);
          } else {
            return twoFirstFiscalYears.get(0);
          }
        } else {
          return null;
        }
      });
  }

  private boolean isOverlapped(FiscalYear first, FiscalYear second) {
    return first.getPeriodEnd().after(second.getPeriodStart());
  }

  private CompletableFuture<List<FiscalYear>> getTwoFirstFiscalYears(String ledgerId, FiscalYearsHelper fiscalYearsHelper) {
    return new LedgersHelper(httpClient, okapiHeaders, ctx, lang).getLedger(ledgerId)
      .thenCompose(ledger -> fiscalYearsHelper.getFiscalYear(ledger.getFiscalYearOneId()))
      .thenApply(this::buildCurrentFYQuery)
      .thenCompose(endpoint -> fiscalYearsHelper.getFiscalYears(2, 0, endpoint))
      .thenApply(FiscalYearsCollection::getFiscalYears);
  }

  private String buildCurrentFYQuery(FiscalYear fiscalYearOne) {
    Instant now = Instant.now();
    return String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, fiscalYearOne.getSeries(), now);
  }

  private CompletableFuture<Void> assignFundToGroups(CompositeFund compositeFund, String fiscalYearId) {
    List<GroupFundFiscalYear> groupFundFiscalYears = buildGroupFundFiscalYears(compositeFund, fiscalYearId);
    return VertxCompletableFuture.allOf(ctx, groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> groupFundFiscalYearHelper.createGroupFundFiscalYear(groupFundFiscalYear))
      .toArray(CompletableFuture[]::new));
  }

  private List<GroupFundFiscalYear> buildGroupFundFiscalYears(CompositeFund compositeFund, String fiscalYearId, List<String> groupIds) {
    return StreamEx.of(groupIds)
      .map(groupId -> buildGroupFundFiscalYear(compositeFund, fiscalYearId, groupId))
      .toList();
  }

  private List<GroupFundFiscalYear> buildGroupFundFiscalYears(CompositeFund compositeFund, String fiscalYearId) {
    return buildGroupFundFiscalYears(compositeFund, fiscalYearId, compositeFund.getGroupIds());
  }

  private GroupFundFiscalYear buildGroupFundFiscalYear(CompositeFund compositeFund, String fiscalYearId, String groupId) {
    return new GroupFundFiscalYear().withGroupId(groupId)
      .withFundId(compositeFund.getFund().getId())
      .withFiscalYearId(fiscalYearId);
  }

  public CompletableFuture<FundsCollection> getFunds(int limit, int offset, String query) {
    String endpoint = String.format(GET_FUNDS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> supplyBlockingAsync(ctx, () -> json.mapTo(FundsCollection.class)));
  }

  public CompletableFuture<CompositeFund> getFund(String id) {
    return handleGetRequest(resourceByIdPath(FUNDS, id, lang), httpClient, ctx, okapiHeaders, logger)
      .thenApply(json -> new CompositeFund().withFund(json.mapTo(Fund.class)))
      .thenCompose(compositeFund -> getCurrentFiscalYear(compositeFund.getFund()
        .getLedgerId())
          .thenCompose(currentFY -> Objects.isNull(currentFY) ? CompletableFuture.completedFuture(null)
              : getGroupIdsThatFundBelongs(id, currentFY.getId()))
          .thenApply(compositeFund::withGroupIds));
  }

  private CompletableFuture<Void> assignFundToGroups(List<GroupFundFiscalYear> groupFundFiscalYears) {
    return VertxCompletableFuture.allOf(ctx, groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> new GroupFundFiscalYearHelper(httpClient, okapiHeaders, ctx, lang).createGroupFundFiscalYear(groupFundFiscalYear))
      .toArray(CompletableFuture[]::new));
  }

  private CompletableFuture<Void> unassignGroupsForFund(Collection<String> groupFundFiscalYearIds) {
    return VertxCompletableFuture.allOf(ctx, groupFundFiscalYearIds.stream()
      .map(id -> new GroupFundFiscalYearHelper(httpClient, okapiHeaders, ctx, lang).deleteGroupFundFiscalYear(id))
      .toArray(CompletableFuture[]::new));
  }

  private CompletableFuture<GroupFundFiscalYearCollection> getGroupFundFiscalYearCollection(String fundId, String currentFYId) {
    String query = String.format("fundId==%s AND fiscalYearId==%s", fundId, currentFYId);
    return groupFundFiscalYearHelper.getGroupFundFiscalYears(Integer.MAX_VALUE, 0, query);
  }

  private CompletionStage<List<String>> getGroupIdsThatFundBelongs(String fundId, String currentFYId) {
    return getGroupFundFiscalYearCollection(fundId, currentFYId)
      .thenApply(groupFundFiscalYearCollection -> groupFundFiscalYearCollection.getGroupFundFiscalYears()
        .stream()
        .map(GroupFundFiscalYear::getGroupId)
        .collect(toList()));
  }

  private CompletionStage<List<GroupFundFiscalYear>> getGroupFundFiscalYearsThatFundBelongs(String fundId, String currentFYId) {
    return getGroupFundFiscalYearCollection(fundId, currentFYId)
      .thenApply(groupFundFiscalYearCollection -> new ArrayList<>(groupFundFiscalYearCollection.getGroupFundFiscalYears()));
  }

  private List<String> groupFundFiscalYearIdsForDeletion(List<GroupFundFiscalYear> groupFundFiscalYearCollection, List<String> groupIdsForDeletion) {
    return groupFundFiscalYearCollection.stream().filter(item -> groupIdsForDeletion.contains(item.getGroupId())).map(GroupFundFiscalYear::getId).collect(toList());
  }

  private CompletionStage<Void> createGroupFundFiscalYears(CompositeFund compositeFund, String currentFiscalYearId, List<String> groupIdsForCreation) {
    if(CollectionUtils.isNotEmpty(groupIdsForCreation)) {
      return groupsHelper.getGroups(0, 0, convertIdsToCqlQuery(groupIdsForCreation))
        .thenCompose(collection -> {
          if(collection.getTotalRecords() == groupIdsForCreation.size()) {
            return assignFundToGroups(buildGroupFundFiscalYears(compositeFund, currentFiscalYearId, groupIdsForCreation));
          } else {
            throw new HttpException(422, GROUP_NOT_FOUND);
          }
        });
    } else {
      return VertxCompletableFuture.completedFuture(null);
    }
  }

  private CompletionStage<Void> deleteGroupFundFiscalYears(List<String> groupFundFiscalYearForDeletionIds) {
    if(CollectionUtils.isNotEmpty(groupFundFiscalYearForDeletionIds)) {
      return unassignGroupsForFund(groupFundFiscalYearForDeletionIds);
    } else {
      return VertxCompletableFuture.completedFuture(null);
    }
  }

  public CompletableFuture<Void> updateFund(CompositeFund compositeFund) {

    Fund fund = compositeFund.getFund();
    Set<String> groupIds = new HashSet<>(compositeFund.getGroupIds());

    return getCurrentFiscalYear(fund.getLedgerId())
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
          return VertxCompletableFuture.completedFuture(null);
        } else {
          throw new HttpException(422, FISCAL_YEARS_NOT_FOUND);
        }
      }).thenCompose(vVoid -> handleUpdateRequest(resourceByIdPath(FUNDS, fund.getId(), lang), fund));
  }

  public CompletableFuture<Void> deleteFund(String id) {
    String query = String.format("fundId==%s", id);
    return groupFundFiscalYearHelper.getGroupFundFiscalYears(Integer.MAX_VALUE, 0, query)
      .thenApply(collection -> collection.getGroupFundFiscalYears().stream().map(GroupFundFiscalYear::getId).collect(toSet()))
      .thenApply(this::unassignGroupsForFund)
      .thenCompose(vVoid -> handleDeleteRequest(resourceByIdPath(FUNDS, id, lang)));
  }

}
