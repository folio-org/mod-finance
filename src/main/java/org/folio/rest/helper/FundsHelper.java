package org.folio.rest.helper;

import static java.util.stream.Collectors.*;
import static me.escoffier.vertx.completablefuture.VertxCompletableFuture.supplyBlockingAsync;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.handleGetRequest;
import static org.folio.rest.util.ResourcePathResolver.FUNDS;
import static org.folio.rest.util.ResourcePathResolver.FUND_TYPES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.CompositeFund;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.FundTypesCollection;
import org.folio.rest.jaxrs.model.FundsCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.util.HelperUtils;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class FundsHelper extends AbstractHelper {

  private static final String GET_FUND_TYPES_BY_QUERY = resourcesPath(FUND_TYPES) + SEARCH_PARAMS;
  private static final String GET_FUNDS_BY_QUERY = resourcesPath(FUNDS) + SEARCH_PARAMS;
  public static final String SEARCH_CURRENT_FISCAL_YEAR_QUERY = "(series==\"%s\") AND ((periodStart<=%s AND periodEnd>%s) OR (periodStart<=%s AND periodEnd>%s)) sortBy periodStart";

  public FundsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
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
      return getCurrentFiscalYearId(fund.getLedgerId())
        .thenCompose(fiscalYearId -> {
          if (isEmpty(fiscalYearId)) {
            throw new HttpException(422, FISCAL_YEARS_NOT_FOUND);
          }
          return handleCreateRequest(resourcesPath(FUNDS), fund).thenAccept(fund::setId)
            .thenCompose(ok -> assignFundToGroups(compositeFund, fiscalYearId));
        })
        .thenApply(aVoid -> compositeFund);
    }
    return handleCreateRequest(resourcesPath(FUNDS), fund)
      .thenApply(id -> {
        fund.setId(id);
        return compositeFund;
      });
  }

  private CompletableFuture<String> getCurrentFiscalYearId(String ledgerId) {
    FiscalYearsHelper fiscalYearsHelper = new FiscalYearsHelper(httpClient, okapiHeaders, ctx, lang);
    return new LedgersHelper(httpClient, okapiHeaders, ctx, lang).getLedger(ledgerId)
      .thenCompose(ledger -> fiscalYearsHelper.getFiscalYear(ledger.getFiscalYearOneId()))
      .thenApply(this::buildCurrentFYQuery)
      .thenCompose(endpoint -> fiscalYearsHelper.getFiscalYears(1, 0, endpoint))
      .thenApply(fiscalYearsCollection -> fiscalYearsCollection.getFiscalYears()
        .stream()
        .map(FiscalYear::getId)
        .findFirst()
        .orElse(null));
  }

  private String buildCurrentFYQuery(FiscalYear fiscalYearOne) {
    Instant now = Instant.now();
    Instant next = now.plus(HelperUtils.getFiscalYearDuration(fiscalYearOne));
    return String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, fiscalYearOne.getSeries(), now, now, next, next);
  }

  private CompletableFuture<Void> assignFundToGroups(CompositeFund compositeFund, String fiscalYearId) {
    List<GroupFundFiscalYear> groupFundFiscalYears = buildGroupFundFiscalYears(compositeFund, fiscalYearId);
    return VertxCompletableFuture.allOf(ctx, groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> getGroupFundFiscalYearHelper().createGroupFundFiscalYear(groupFundFiscalYear))
      .toArray(CompletableFuture[]::new));
  }

  private GroupFundFiscalYearHelper getGroupFundFiscalYearHelper() {
    return new GroupFundFiscalYearHelper(httpClient, okapiHeaders, ctx, lang);
  }

  private List<GroupFundFiscalYear> buildGroupFundFiscalYears(CompositeFund compositeFund, String fiscalYearId) {
    return compositeFund.getGroupIds()
      .stream()
      .distinct()
      .map(groupId -> buildGroupFundFiscalYear(compositeFund, fiscalYearId, groupId))
      .collect(Collectors.toList());
  }

  private GroupFundFiscalYear buildGroupFundFiscalYear(CompositeFund compositeFund, String fiscalYearId, String groupId) {
    return new GroupFundFiscalYear().withGroupId(groupId)
      .withFundId(compositeFund.getFund().getId())
      .withFiscalYearId(fiscalYearId);
  }

  private CompletableFuture<Void> assignFundToGroups(List<GroupFundFiscalYear> groupFundFiscalYears) {
    return VertxCompletableFuture.allOf(ctx, groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> new GroupFundFiscalYearHelper(httpClient, okapiHeaders, ctx, lang).createGroupFundFiscalYear(groupFundFiscalYear))
      .toArray(CompletableFuture[]::new));
  }

  private CompletableFuture<Void> unassignGroupsForFund(List<String> groupFundFiscalYearIds) {
    return VertxCompletableFuture.allOf(ctx, groupFundFiscalYearIds.stream()
      .map(id -> new GroupFundFiscalYearHelper(httpClient, okapiHeaders, ctx, lang).deleteGroupFundFiscalYear(id))
      .toArray(CompletableFuture[]::new));

  public CompletableFuture<FundsCollection> getFunds(int limit, int offset, String query) {
    String endpoint = String.format(GET_FUNDS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> supplyBlockingAsync(ctx, () -> json.mapTo(FundsCollection.class)));
  }

  public CompletableFuture<CompositeFund> getFund(String id) {
    return handleGetRequest(resourceByIdPath(FUNDS, id, lang), httpClient, ctx, okapiHeaders, logger)
      .thenApply(json -> new CompositeFund().withFund(json.mapTo(Fund.class)))
      .thenCompose(compositeFund -> getCurrentFiscalYearId(compositeFund.getFund()
        .getLedgerId())
          .thenCompose(currentFYId -> isEmpty(currentFYId) ? CompletableFuture.completedFuture(null)
              : getGroupIdsThatFundBelongs(id, currentFYId))
          .thenApply(compositeFund::withGroupIds));
  }

  private CompletionStage<List<String>> getGroupIdsThatFundBelongs(String fundId, String currentFYId) {
    String query = String.format("fundId==%s AND fiscalYearId==%s", fundId, currentFYId);
    return getGroupFundFiscalYearHelper().getGroupFundFiscalYears(Integer.MAX_VALUE, 0, query)
      .thenApply(groupFundFiscalYearCollection -> groupFundFiscalYearCollection.getGroupFundFiscalYears()
        .stream()
        .map(GroupFundFiscalYear::getGroupId)
        .collect(Collectors.toList()));

  }

  public CompletableFuture<Void> updateFund(CompositeFund compositeFund) {

    Fund fund = compositeFund.getFund();
    List<String> groupIds = compositeFund.getGroupIds();

    if(CollectionUtils.isEmpty(groupIds)) {
      return handleUpdateRequest(resourceByIdPath(FUNDS, fund.getId(), lang), fund);
    } else {
      return getCurrentFiscalYearId(fund.getLedgerId())
        .thenCompose(currentFiscalYearId -> {
          if(StringUtils.isNotEmpty(currentFiscalYearId)) {
            GroupFundFiscalYearHelper helper = new GroupFundFiscalYearHelper(okapiHeaders, ctx, lang);
            String query = String.format("fundId==%s AND fiscalYearId==%s", fund.getId(), currentFiscalYearId);
            return helper.getGroupFundFiscalYears(Integer.MAX_VALUE, 0, query)
              .thenCompose(groupFundFiscalYearCollection -> {

                Map<String, GroupFundFiscalYear> groupFundFiscalYearFromStorageMap
                  = groupFundFiscalYearCollection.getGroupFundFiscalYears().stream()
                  .collect(toMap(GroupFundFiscalYear::getGroupId, groupFundFiscalYear -> groupFundFiscalYear));

                List<GroupFundFiscalYear> groupFundFiscalYearsForCreation
                  = groupIds.stream()
                  .filter(groupId -> !groupFundFiscalYearFromStorageMap.containsKey(groupId))
                  .map(groupId -> new GroupFundFiscalYear().withGroupId(groupId).withFiscalYearId(currentFiscalYearId).withFundId(fund.getId()))
                  .collect(toList());

                List<String> groupFundFiscalYearsForDeletion
                  = groupFundFiscalYearFromStorageMap.keySet().stream()
                  .filter(groupIdFromStorage -> !groupIds.contains(groupIdFromStorage))
                  .collect(toList());

                return assignFundToGroups(groupFundFiscalYearsForCreation)
                  .thenCompose(v -> unassignGroupsForFund(groupFundFiscalYearsForDeletion));
              });
          }
          return CompletableFuture.completedFuture(null);
        }).thenCompose(vVoid -> handleUpdateRequest(resourceByIdPath(FUNDS, fund.getId(), lang), fund));
    }
  }

  public CompletableFuture<Void> deleteFund(String id) {
    GroupFundFiscalYearHelper helper = new GroupFundFiscalYearHelper(okapiHeaders, ctx, lang);
    String query = String.format("fundId==%s", id);
    return helper.getGroupFundFiscalYears(Integer.MAX_VALUE, 0, query)
      .thenApply(collection -> collection.getGroupFundFiscalYears().stream().map(GroupFundFiscalYear::getId).collect(toSet()))
      .thenApply(ids -> ids.stream().map(helper::deleteGroupFundFiscalYear).toArray(CompletableFuture[]::new))
      .thenCompose(vVoid -> handleDeleteRequest(resourceByIdPath(FUNDS, id, lang)));
  }

}
