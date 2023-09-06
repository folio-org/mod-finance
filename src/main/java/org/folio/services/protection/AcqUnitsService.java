package org.folio.services.protection;

import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.ACQUISITIONS_UNITS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.services.protection.AcqUnitConstants.ACQUISITIONS_UNIT_IDS;
import static org.folio.services.protection.AcqUnitConstants.ACTIVE_UNITS_CQL;
import static org.folio.services.protection.AcqUnitConstants.IS_DELETED_PROP;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.finance.AcquisitionsUnit;
import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembership;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import one.util.streamex.StreamEx;

public class AcqUnitsService {
  public final Logger logger = LogManager.getLogger(AcqUnitsService.class);

  private final RestClient restClient;
  private final AcqUnitMembershipsService acqUnitMembershipsService;

  public AcqUnitsService(RestClient restClient, AcqUnitMembershipsService acqUnitMembershipsService) {
    this.restClient = restClient;
    this.acqUnitMembershipsService = acqUnitMembershipsService;
  }

  public Future<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, RequestContext requestContext) {
    if (StringUtils.isEmpty(query)) {
      query = ACTIVE_UNITS_CQL;
    } else if (!query.contains(IS_DELETED_PROP)) {
      query = combineCqlExpressions("and", ACTIVE_UNITS_CQL, query);
    }
    var requestEntry = new RequestEntry(resourcesPath(ACQUISITIONS_UNITS))
      .withLimit(limit)
      .withOffset(offset)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), AcquisitionsUnitCollection.class, requestContext);
  }

  public Future<String> buildAcqUnitsCqlClause(RequestContext requestContext) {
    return getAcqUnitIdsForSearch(requestContext)
      .map(ids -> {
        if (ids.isEmpty()) {
          return NO_ACQ_UNIT_ASSIGNED_CQL;
        }
        return String.format("%s or (%s)", convertIdsToCqlQuery(ids, ACQUISITIONS_UNIT_IDS, false), NO_ACQ_UNIT_ASSIGNED_CQL);
      });
  }

  private Future<List<String>> getAcqUnitIdsForSearch(RequestContext requestContext) {
    var unitsForUser = getAcqUnitIdsForUser(requestContext.getHeaders().get(OKAPI_USERID_HEADER), requestContext);
    var unitsAllowRead = getOpenForReadAcqUnitIds(requestContext);

    return CompositeFuture.join(unitsForUser, unitsAllowRead)
      .map(cf -> StreamEx.of(unitsForUser.result(), unitsAllowRead.result())
        .flatCollection(strings -> strings)
        .distinct()
        .toList());
  }


  private Future<List<String>> getAcqUnitIdsForUser(String userId, RequestContext requestContext) {
    return acqUnitMembershipsService.getAcquisitionsUnitsMemberships("userId==" + userId, 0, Integer.MAX_VALUE, requestContext)
      .map(memberships -> {
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

  private Future<List<String>> getOpenForReadAcqUnitIds(RequestContext requestContext) {
    return getAcquisitionsUnits("protectRead==false", 0, Integer.MAX_VALUE, requestContext)
      .map(units -> {
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
