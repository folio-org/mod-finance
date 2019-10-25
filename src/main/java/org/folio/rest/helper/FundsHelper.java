package org.folio.rest.helper;

import static me.escoffier.vertx.completablefuture.VertxCompletableFuture.supplyBlockingAsync;
import static org.folio.rest.helper.FiscalYearsHelper.GET_FISCAL_YEARS_BY_QUERY;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.handleGetRequest;
import static org.folio.rest.util.ResourcePathResolver.FUNDS;
import static org.folio.rest.util.ResourcePathResolver.FUND_TYPES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
import org.joda.time.DateTime;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class FundsHelper extends AbstractHelper {

  private static final String GET_FUND_TYPES_BY_QUERY = resourcesPath(FUND_TYPES) + SEARCH_PARAMS;
  private static final String GET_FUNDS_BY_QUERY = resourcesPath(FUNDS) + SEARCH_PARAMS;
  public static final String SEARCH_CURRENT_FISCAL_YEAR_QUERY = "(ledgerFY.ledgerId==\"%s\") AND ((periodStart<=%s AND periodEnd>%s) OR (periodStart<=%s AND periodEnd>%s)) sortBy periodStart";

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
        .thenCompose(fiscalYearId -> handleCreateRequest(resourcesPath(FUNDS), fund).thenAccept(fund::setId)
          .thenCompose(ok -> assignFundToGroups(compositeFund, fiscalYearId)))
        .thenApply(aVoid -> compositeFund);
    }
    return handleCreateRequest(resourcesPath(FUNDS), fund)
      .thenApply(ok -> compositeFund);
  }

  private CompletableFuture<String> getCurrentFiscalYearId(String ledgerId) {
    DateTime now = DateTime.now();
    DateTime next = now.plusYears(1);
    String query = String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, ledgerId, now, now, next, next);
    String endpoint = String.format(GET_FISCAL_YEARS_BY_QUERY, 1, 0,  buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenApply(fiscalYears -> fiscalYears.mapTo(FiscalYearsCollection.class))
      .thenApply(fiscalYearsCollection -> fiscalYearsCollection.getFiscalYears().stream()
        .map(FiscalYear::getId).findFirst()
        .orElseThrow(() -> new HttpException(422, FISCAL_YEARS_NOT_FOUND)));
  }

  private CompletableFuture<Void> assignFundToGroups(CompositeFund compositeFund, String fiscalYearId) {
    List<GroupFundFiscalYear> groupFundFiscalYears = buildGroupFundFiscalYear(compositeFund, fiscalYearId);
    return VertxCompletableFuture.allOf(ctx, groupFundFiscalYears.stream()
      .map(groupFundFiscalYear -> new GroupFundFiscalYearHelper(httpClient, okapiHeaders, ctx, lang).createGroupFundFiscalYear(groupFundFiscalYear))
      .toArray(CompletableFuture[]::new));
  }

  private List<GroupFundFiscalYear> buildGroupFundFiscalYear(CompositeFund compositeFund, String fiscalYearId) {
    return compositeFund.getGroupIds().stream().distinct().map(groupId -> new GroupFundFiscalYear()
      .withGroupId(groupId).withFundId(compositeFund.getFund().getId()).withFiscalYearId(fiscalYearId)
    ).collect(Collectors.toList());
  }

  public CompletableFuture<FundsCollection> getFunds(int limit, int offset, String query) {
    String endpoint = String.format(GET_FUNDS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> supplyBlockingAsync(ctx, () -> json.mapTo(FundsCollection.class)));
  }

  public CompletableFuture<CompositeFund> getFund(String id) {
    return handleGetRequest(resourceByIdPath(FUNDS, id, lang), httpClient, ctx, okapiHeaders, logger)
      .thenApply(json -> new CompositeFund().withFund(json.mapTo(Fund.class)));
  }

  public CompletableFuture<Void> updateFund(CompositeFund compositeFund) {
    Fund fund = compositeFund.getFund();
    return handleUpdateRequest(resourceByIdPath(FUNDS, fund.getId(), lang), fund);
  }

  public CompletableFuture<Void> deleteFund(String id) {
    return handleDeleteRequest(resourceByIdPath(FUNDS, id, lang));
  }

}
