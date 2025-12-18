package org.folio.services.protection;

import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.ACQUISITIONS_UNITS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.services.protection.AcqUnitConstants.ACQUISITIONS_UNIT_IDS;
import static org.folio.services.protection.AcqUnitConstants.ACTIVE_UNITS_CQL;
import static org.folio.services.protection.AcqUnitConstants.FD_FUND_ACQUISITIONS_UNIT_IDS;
import static org.folio.services.protection.AcqUnitConstants.IS_DELETED_PROP;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.folio.services.protection.AcqUnitConstants.NO_FD_FUND_UNIT_ASSIGNED_CQL;

import io.vertx.core.Future;
import java.util.List;
import java.util.stream.Collectors;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.finance.AcquisitionsUnit;
import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembership;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;

public class AcqUnitsService {

  public final Logger log = LogManager.getLogger();

  private final RestClient restClient;
  private final AcqUnitMembershipsService acqUnitMembershipsService;

  public AcqUnitsService(RestClient restClient, AcqUnitMembershipsService acqUnitMembershipsService) {
    this.restClient = restClient;
    this.acqUnitMembershipsService = acqUnitMembershipsService;
  }

  public Future<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, RequestContext requestContext) {
    log.debug("getAcquisitionsUnits:: Getting acquisitionsUnits by query={}, offset={}, and limit={}", query, offset, limit);
    if (StringUtils.isEmpty(query)) {
      log.info("getAcquisitionsUnits:: query is empty, so using '{}' query", ACTIVE_UNITS_CQL);
      query = ACTIVE_UNITS_CQL;
    } else if (!query.contains(IS_DELETED_PROP)) {
      query = combineCqlExpressions("and", ACTIVE_UNITS_CQL, query);
    }
    var requestEntry = new RequestEntry(resourcesPath(ACQUISITIONS_UNITS))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), AcquisitionsUnitCollection.class, requestContext);
  }

  public Future<String> buildAcqUnitsCqlClause(RequestContext requestContext) {
    return buildGenericAcqUnitsCqlClause(requestContext, ACQUISITIONS_UNIT_IDS, NO_ACQ_UNIT_ASSIGNED_CQL);
  }

  public Future<String> buildAcqUnitsCqlClauseForFinanceData(RequestContext requestContext) {
    return buildGenericAcqUnitsCqlClause(requestContext, FD_FUND_ACQUISITIONS_UNIT_IDS, NO_FD_FUND_UNIT_ASSIGNED_CQL);
  }

  private Future<String> buildGenericAcqUnitsCqlClause(RequestContext requestContext, String fieldName, String noUnitAssignedCql) {
    return getAcqUnitIdsForSearch(requestContext)
      .map(ids -> {
        if (ids.isEmpty()) {
          return noUnitAssignedCql;
        }
        String acqUnitIdsCql = convertIdsToCqlQuery(ids, fieldName, false);
        return String.format("%s or (%s)", acqUnitIdsCql, noUnitAssignedCql);
      });
  }

  private Future<List<String>> getAcqUnitIdsForSearch(RequestContext requestContext) {
    var unitsForUser = getAcqUnitIdsForUser(getUserId(requestContext), requestContext);
    var unitsAllowRead = getOpenForReadAcqUnitIds(requestContext);

    return Future.join(unitsForUser, unitsAllowRead)
      .map(cf -> StreamEx.of(unitsForUser.result(), unitsAllowRead.result())
        .flatCollection(strings -> strings)
        .distinct()
        .collect(Collectors.toList()));
  }

  private Future<List<String>> getAcqUnitIdsForUser(String userId, RequestContext requestContext) {
    return acqUnitMembershipsService.getAcquisitionsUnitsMemberships("userId==" + userId, 0, Integer.MAX_VALUE, requestContext)
      .map(memberships -> {
        List<String> ids = memberships.getAcquisitionsUnitMemberships()
          .stream()
          .map(AcquisitionsUnitMembership::getAcquisitionsUnitId)
          .collect(Collectors.toList());

        if (log.isDebugEnabled()) {
          log.debug("User belongs to {} acq units: {}", ids.size(), StreamEx.of(ids).joining(", "));
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
        if (log.isDebugEnabled()) {
          log.debug("{} acq units with 'protectRead==false' are found: {}", ids.size(), StreamEx.of(ids).joining(", "));
        }
        return ids;
      });
  }

  private String getUserId(RequestContext requestContext) {
    return requestContext.headers().get(OKAPI_USERID_HEADER);
  }
}
