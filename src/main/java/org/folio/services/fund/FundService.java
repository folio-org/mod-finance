package org.folio.services.fund;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundsCollection;
import org.folio.rest.util.HelperUtils;
import org.folio.services.protection.AcqUnitsService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import io.vertx.core.Future;
import java.util.concurrent.CompletionException;

import static java.util.stream.Collectors.toList;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.RestConstants.NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.FUND_NOT_FOUND_ERROR;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FUNDS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

public class FundService {
  private static final Logger logger = LogManager.getLogger(FundService.class);

  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;
  public static final String ID = "id";
  private static final String ENDPOINT = "/finance/funds";

  public FundService(RestClient restClient, AcqUnitsService acqUnitsService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<Fund> retrieveFundById(String fundId, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(FUNDS_STORAGE, fundId), Fund.class, requestContext);
  }

  public Future<FundsCollection> getFundsWithAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .thenCompose(effectiveQuery -> restClient.get(effectiveQuery, offset, limit, requestContext, FundsCollection.class));
  }

  public Future<FundsCollection> getFundsWithoutAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return restClient.get(query, offset, limit, requestContext, FundsCollection.class);
  }

  public Future<List<Fund>> getFundsByIds(Collection<String> ids, RequestContext requestContext) {
    String query = HelperUtils.convertIdsToCqlQuery(ids);
    RequestEntry requestEntry = new RequestEntry(ENDPOINT).withQuery(query)
      .withLimit(MAX_IDS_FOR_GET_RQ)
      .withOffset(0);
    return restClient.get(requestEntry, requestContext, FundsCollection.class)
      .map(FundsCollection::getFunds);
  }

  public Future<List<Fund>> getFunds(List<String> fundIds, RequestContext requestContext) {
    return collectResultsOnSuccess(
      ofSubLists(new ArrayList<>(fundIds), MAX_IDS_FOR_GET_RQ).map(ids -> getFundsByIds(ids, requestContext))
        .toList()).map(
      lists -> lists.stream()
        .flatMap(Collection::stream)
        .collect(toList()));
  }

  private Future<List<Fund>> getFundsByIds(List<String> ids, RequestContext requestContext) {
    String query = HelperUtils.convertIdsToCqlQuery(ids);
    RequestEntry requestEntry = new RequestEntry(resourcesPath(FUNDS_STORAGE)).withQuery(query)
      .withOffset(0)
      .withLimit(MAX_IDS_FOR_GET_RQ);
    return restClient.get(requestEntry, requestContext, FundsCollection.class)
      .map(FundsCollection::getFunds);
  }
}
