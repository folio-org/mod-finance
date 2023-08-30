package org.folio.services.protection;

import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.rest.util.ErrorCodes.FUND_UNITS_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.USER_HAS_NO_PERMISSIONS;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.services.protection.AcqUnitConstants.ACQUISITIONS_UNIT_ID;
import static org.folio.services.protection.AcqUnitConstants.ACQUISITIONS_UNIT_IDS;
import static org.folio.services.protection.AcqUnitConstants.ALL_UNITS_CQL;

import java.util.List;
import java.util.Set;
import io.vertx.core.Future;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.folio.HttpStatus;
import org.folio.rest.acq.model.finance.AcquisitionsUnit;
import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.services.protection.models.ProtectedOperationType;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ProtectionService {
  public final Logger logger = LogManager.getLogger(ProtectionService.class);

  private final AcqUnitsService acqUnitsService;
  private final AcqUnitMembershipsService acqUnitMembershipsService;

  public ProtectionService(AcqUnitsService acqUnitsService, AcqUnitMembershipsService acqUnitMembershipsService) {
    this.acqUnitsService = acqUnitsService;
    this.acqUnitMembershipsService = acqUnitMembershipsService;
  }

  public Future<Void> checkOperationsRestrictions(List<String> unitIds, Set<ProtectedOperationType> operations, RequestContext requestContext) {
    if (CollectionUtils.isNotEmpty(unitIds)) {
      return getUnitsByIds(unitIds, requestContext)
        .thenCompose(units -> {
          if (unitIds.size() == units.size()) {
            List<AcquisitionsUnit> activeUnits = units.stream()
              .filter(unit -> !unit.getIsDeleted())
              .collect(Collectors.toList());
            if (!activeUnits.isEmpty() && applyMergingStrategy(activeUnits, operations)) {
              return verifyUserIsMemberOfFundUnits(extractUnitIds(activeUnits), requestContext.getHeaders().get(OKAPI_USERID_HEADER), requestContext);
            }
            return succeededFuture(null);
          } else {
            throw new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(unitIds, extractUnitIds(units)));
          }
        });
    } else {
      return succeededFuture(null);
    }
  }

  private Future<List<AcquisitionsUnit>> getUnitsByIds(List<String> unitIds, RequestContext requestContext) {
    String query = combineCqlExpressions("and", ALL_UNITS_CQL, convertIdsToCqlQuery(unitIds));
    return acqUnitsService.getAcquisitionsUnits(query, 0, Integer.MAX_VALUE, requestContext)
      .map(AcquisitionsUnitCollection::getAcquisitionsUnits);
  }

  private boolean applyMergingStrategy(List<AcquisitionsUnit> units, Set<ProtectedOperationType> operations) {
    return units.stream().allMatch(unit -> operations.stream().anyMatch(operation -> operation.isProtected(unit)));
  }

  private Future<Void> verifyUserIsMemberOfFundUnits(List<String> unitIdsAssignedToFund, String currentUserId,
                                                                RequestContext requestContext) {
    String query = String.format("userId==%s AND %s", currentUserId, convertIdsToCqlQuery(unitIdsAssignedToFund, ACQUISITIONS_UNIT_ID, true));
    return acqUnitMembershipsService.getAcquisitionsUnitsMemberships(query, 0, Integer.MAX_VALUE, requestContext)
      .thenAccept(unit -> {
        if (unit.getTotalRecords() == 0) {
          throw new HttpException(HttpStatus.HTTP_FORBIDDEN.toInt(), USER_HAS_NO_PERMISSIONS);
        }
      })
      .exceptionally(t -> {
        throw new CompletionException(t);
      });
  }

  private List<String> extractUnitIds(List<AcquisitionsUnit> activeUnits) {
    return activeUnits.stream().map(AcquisitionsUnit::getId).collect(Collectors.toList());
  }

  private Error buildUnitsNotFoundError(List<String> expectedUnitIds, List<String> availableUnitIds) {
    List<String> missingUnitIds = ListUtils.subtract(expectedUnitIds, availableUnitIds);
    return FUND_UNITS_NOT_FOUND.toError().withAdditionalProperty(ACQUISITIONS_UNIT_IDS, missingUnitIds);
  }
}
