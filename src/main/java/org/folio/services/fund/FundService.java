package org.folio.services.fund;

import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FUNDS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.FUND_TYPES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.FundTypesCollection;
import org.folio.rest.jaxrs.model.FundsCollection;
import org.folio.rest.util.HelperUtils;
import org.folio.services.protection.AcqUnitsService;

import io.vertx.core.Future;

public class FundService {
  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;
  public static final String ID = "id";

  public FundService(RestClient restClient, AcqUnitsService acqUnitsService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<Fund> getFundById(String fundId, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(FUNDS_STORAGE, fundId), Fund.class, requestContext);
  }

  public Future<Fund> createFund(Fund fund, RequestContext requestContext) {
    return restClient.post(resourcesPath(FUNDS_STORAGE), fund, Fund.class, requestContext);
  }

  public Future<FundsCollection> getFundsWithAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .map(effectiveQuery -> new RequestEntry(resourcesPath(FUNDS_STORAGE))
        .withOffset(offset)
        .withLimit(limit)
        .withQuery(effectiveQuery)
      )
      .compose(requestEntry -> restClient.get(requestEntry.buildEndpoint(), FundsCollection.class, requestContext));
  }

  public Future<FundsCollection> getFundsWithoutAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FUNDS_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FundsCollection.class, requestContext);
  }

  public Future<List<Fund>> getFundsByIds(List<String> fundIds, RequestContext requestContext) {
    return collectResultsOnSuccess(
      ofSubLists(new ArrayList<>(fundIds), MAX_IDS_FOR_GET_RQ)
        .map(ids -> getFundsByIdsChunk(ids, requestContext))
        .toList())
      .map(lists -> lists.stream()
        .flatMap(Collection::stream)
        .toList()
      );
  }

  private Future<List<Fund>> getFundsByIdsChunk(List<String> ids, RequestContext requestContext) {
    String query = HelperUtils.convertIdsToCqlQuery(ids);
    var requestEntry = new RequestEntry(resourcesPath(FUNDS_STORAGE))
      .withQuery(query)
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE);
    return restClient.get(requestEntry.buildEndpoint(), FundsCollection.class, requestContext)
      .map(FundsCollection::getFunds);
  }

  public Future<FundType> getFundTypeById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(FUND_TYPES, id), FundType.class, requestContext);
  }

  public Future<FundType> createFundType(FundType fundType, RequestContext requestContext) {
    return restClient.post(resourcesPath(FUND_TYPES), fundType, FundType.class, requestContext);

  }

  public Future<FundTypesCollection> getFundTypes(int offset, int limit, String query, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FUND_TYPES))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FundTypesCollection.class, requestContext);

  }

  public Future<Void> updateFundType(FundType fundType, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(FUND_TYPES, fundType.getId()), fundType, requestContext);
  }

  public Future<Void> updateFund(Fund fund, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(FUNDS_STORAGE, fund.getId()), fund, requestContext);
  }

  public Future<Void> deleteFundType(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(FUND_TYPES, id), requestContext);
  }
}
