package org.folio.services.protection;

import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.services.protection.AcqUnitConstants.ACQUISITIONS_UNIT_IDS;
import static org.folio.services.protection.AcqUnitConstants.ACTIVE_UNITS_CQL;
import static org.folio.services.protection.AcqUnitConstants.IS_DELETED_PROP;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.finance.AcquisitionsUnit;
import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembership;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import one.util.streamex.StreamEx;

public class AcqUnitsService {
  public final Logger logger = LoggerFactory.getLogger(AcqUnitsService.class);

  private final RestClient acqUnitsStorageRestClient;
  private final AcqUnitMembershipsService acqUnitMembershipsService;

  public AcqUnitsService(RestClient acqUnitsStorageRestClient, AcqUnitMembershipsService acqUnitMembershipsService) {
    this.acqUnitsStorageRestClient = acqUnitsStorageRestClient;
    this.acqUnitMembershipsService = acqUnitMembershipsService;
  }

  public CompletableFuture<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, RequestContext requestContext) {
    if (StringUtils.isEmpty(query)) {
      query = ACTIVE_UNITS_CQL;
    } else if (!query.contains(IS_DELETED_PROP)) {
      query = combineCqlExpressions("and", ACTIVE_UNITS_CQL, query);
    }
    return acqUnitsStorageRestClient.get(query, offset, limit, requestContext, AcquisitionsUnitCollection.class);
  }

  public CompletableFuture<String> buildAcqUnitsCqlClause(RequestContext requestContext) {
    return getAcqUnitIdsForSearch(requestContext)
      .thenApply(ids -> {
        if (ids.isEmpty()) {
          return NO_ACQ_UNIT_ASSIGNED_CQL;
        }
        return String.format("%s or (%s)", convertIdsToCqlQuery(ids, ACQUISITIONS_UNIT_IDS, false), NO_ACQ_UNIT_ASSIGNED_CQL);
      });
  }

  private CompletableFuture<List<String>> getAcqUnitIdsForSearch(RequestContext requestContext) {
    return getAcqUnitIdsForUser(requestContext.getHeaders().get(OKAPI_USERID_HEADER), requestContext)
      .thenCombine(getOpenForReadAcqUnitIds(requestContext), (unitsForUser, unitsAllowRead) -> StreamEx.of(unitsForUser, unitsAllowRead)
        .flatCollection(strings -> strings)
        .distinct()
        .toList());
  }

  private CompletableFuture<List<String>> getAcqUnitIdsForUser(String userId, RequestContext requestContext) {
    return acqUnitMembershipsService.getAcquisitionsUnitsMemberships("userId==" + userId, 0, Integer.MAX_VALUE, requestContext)
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

  private CompletableFuture<List<String>> getOpenForReadAcqUnitIds(RequestContext requestContext) {
    return getAcquisitionsUnits("protectRead==false", 0, Integer.MAX_VALUE, requestContext)
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
