package org.folio.services.protection;

import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.finance.AcquisitionsUnit;
import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembership;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembershipCollection;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.services.BaseService;

import one.util.streamex.StreamEx;

public class AcquisitionsUnitsServiceImpl extends BaseService implements AcquisitionsUnitsService {

  @Override
  public CompletableFuture<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit,
                                                                            String lang, RequestContext requestContext) {
    HttpClientInterface client = getHttpClient(requestContext.getHeaders());
    if (StringUtils.isEmpty(query)) {
      query = ACTIVE_UNITS_CQL;
    } else if (!query.contains(IS_DELETED_PROP)) {
      query = combineCqlExpressions("and", ACTIVE_UNITS_CQL, query);
    }
    String endpoint = String.format(GET_UNITS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
    return handleGetRequest(endpoint, client, requestContext.getContext(), requestContext.getHeaders(), logger)
      .thenApply(jsonUnits -> jsonUnits.mapTo(AcquisitionsUnitCollection.class))
      .handle((acqUnitsColl, t) -> {
        client.closeClient();
        if (Objects.nonNull(t)) {
          throw new CompletionException(t.getCause());
        }
        return acqUnitsColl;
      });
  }

  @Override
  public CompletableFuture<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit,
                                                                                                 String lang, RequestContext requestContext) {
    HttpClientInterface client = getHttpClient(requestContext.getHeaders());
    String endpoint = String.format(GET_UNITS_MEMBERSHIPS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
    return handleGetRequest(endpoint, client, requestContext.getContext(), requestContext.getHeaders(), logger)
      .thenApply(jsonUnitsMembership -> jsonUnitsMembership.mapTo(AcquisitionsUnitMembershipCollection.class))
      .handle((acqUnitsMembershipColl, t) -> {
        client.closeClient();
        if (Objects.nonNull(t)) {
          throw new CompletionException(t.getCause());
        }
        return acqUnitsMembershipColl;
      });
  }

  @Override
  public CompletableFuture<String> buildAcqUnitsCqlClause(String query, int offset, int limit,
                                                          String lang, RequestContext requestContext) {
    return getAcqUnitIdsForSearch(lang, requestContext)
      .thenApply(ids -> {
        if (ids.isEmpty()) {
          return NO_ACQ_UNIT_ASSIGNED_CQL;
        }
        return String.format("%s or (%s)", convertIdsToCqlQuery(ids, ACQUISITIONS_UNIT_IDS, false), NO_ACQ_UNIT_ASSIGNED_CQL);
      });
  }

  private CompletableFuture<List<String>> getAcqUnitIdsForSearch(String lang, RequestContext requestContext) {
    return getAcqUnitIdsForUser(requestContext.getHeaders().get(OKAPI_USERID_HEADER), lang, requestContext)
      .thenCombine(getOpenForReadAcqUnitIds(lang, requestContext), (unitsForUser, unitsAllowRead) -> StreamEx.of(unitsForUser, unitsAllowRead)
        .flatCollection(strings -> strings)
        .distinct()
        .toList());
  }

  private CompletableFuture<List<String>> getAcqUnitIdsForUser(String userId, String lang, RequestContext requestContext) {
    return getAcquisitionsUnitsMemberships("userId==" + userId, 0, Integer.MAX_VALUE, lang, requestContext)
      .thenApply(memberships -> {
        List<String> ids = memberships.getAcquisitionsUnitMemberships()
          .stream()
          .map(AcquisitionsUnitMembership::getAcquisitionsUnitId)
          .collect(Collectors.toList());

        if (logger.isDebugEnabled()) {
          logger.debug("User belongs to {} acq units: {}", ids.size(), StreamEx.of(ids).joining(", "));
        }

        return ids;
      });
  }

  private CompletableFuture<List<String>> getOpenForReadAcqUnitIds(String lang, RequestContext requestContext) {
    return getAcquisitionsUnits("protectRead==false", 0, Integer.MAX_VALUE, lang, requestContext)
      .thenApply(units -> {
        List<String> ids = units.getAcquisitionsUnits()
          .stream()
          .map(AcquisitionsUnit::getId)
          .collect(Collectors.toList());
        if (logger.isDebugEnabled()) {
          logger.debug("{} acq units with 'protectRead==false' are found: {}", ids.size(), StreamEx.of(ids).joining(", "));
        }
        return ids;
    });
  }
}
