package org.folio.services.protection;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_PERMISSIONS;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.rest.util.ErrorCodes.FUND_UNITS_NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.USER_HAS_NO_ACQ_PERMISSIONS;
import static org.folio.rest.util.ErrorCodes.USER_HAS_NO_PERMISSIONS;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.services.protection.models.AcqDesiredPermissions.MANAGE;
import static org.folio.services.protection.AcqUnitConstants.ACQUISITIONS_UNIT_ID;
import static org.folio.services.protection.AcqUnitConstants.ACQUISITIONS_UNIT_IDS;
import static org.folio.services.protection.AcqUnitConstants.ALL_UNITS_CQL;
import static org.folio.services.protection.models.ProtectedOperationType.UPDATE;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.folio.HttpStatus;
import org.folio.config.Constants;
import org.folio.rest.acq.model.finance.AcquisitionsUnit;
import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.services.protection.models.AcqDesiredPermissions;
import org.folio.services.protection.models.ProtectedOperationType;

import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class ProtectionService {
  public final Logger logger = LoggerFactory.getLogger(ProtectionService.class);

  private final AcqUnitsService acqUnitsService;
  private final AcqUnitMembershipsService acqUnitMembershipsService;

  public ProtectionService(AcqUnitsService acqUnitsService, AcqUnitMembershipsService acqUnitMembershipsService) {
    this.acqUnitsService = acqUnitsService;
    this.acqUnitMembershipsService = acqUnitMembershipsService;
  }

  public CompletableFuture<Void> checkOperationsRestrictions(List<String> unitIds, Set<ProtectedOperationType> operations, RequestContext requestContext) {
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
            return CompletableFuture.completedFuture(null);
          } else {
            throw new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(unitIds, extractUnitIds(units)));
          }
        });
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  public CompletableFuture<Void> validateAcqUnitsOnUpdate(Fund updatedOrg, Fund currentOrg, RequestContext requestContext) {
    List<String> updatedAcqUnitIds = updatedOrg.getAcqUnitIds();
    List<String> currentAcqUnitIds = currentOrg.getAcqUnitIds();

    return VertxCompletableFuture.runAsync(requestContext.getContext(), () -> verifyUserHasManagePermission(updatedAcqUnitIds,
                                                    currentAcqUnitIds, getProvidedPermissions(requestContext.getHeaders())))
    .thenCompose(ok -> verifyIfUnitsAreActive(ListUtils.subtract(updatedAcqUnitIds, currentAcqUnitIds), requestContext))
    .thenCompose(ok -> checkOperationsRestrictions(currentAcqUnitIds, Collections.singleton(UPDATE), requestContext));
  }

  private CompletableFuture<List<AcquisitionsUnit>> getUnitsByIds(List<String> unitIds, RequestContext requestContext) {
    String query = combineCqlExpressions("and", ALL_UNITS_CQL, convertIdsToCqlQuery(unitIds));
    return acqUnitsService.getAcquisitionsUnits(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(AcquisitionsUnitCollection::getAcquisitionsUnits);
  }

  private boolean applyMergingStrategy(List<AcquisitionsUnit> units, Set<ProtectedOperationType> operations) {
    return units.stream().allMatch(unit -> operations.stream().anyMatch(operation -> operation.isProtected(unit)));
  }

  private CompletableFuture<Void> verifyUserIsMemberOfFundUnits(List<String> unitIdsAssignedToFund, String currentUserId,
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

  /**
   * The method checks if list of acquisition units to which the order is assigned is changed, if yes,
   * then check that if the user has desired permission to manage acquisition units assignments
   *
   * @throws HttpException if user does not have manage permission
   * @param newAcqUnitIds acquisitions units assigned to purchase order from request
   * @param currentAcqUnitIds acquisitions units assigned to purchase order from storage
   */
  private void verifyUserHasManagePermission(List<String> newAcqUnitIds, List<String> currentAcqUnitIds, List<String> permissions) {
    Set<String> newAcqUnits = new HashSet<>(CollectionUtils.emptyIfNull(newAcqUnitIds));
    Set<String> acqUnitsFromStorage = new HashSet<>(CollectionUtils.emptyIfNull(currentAcqUnitIds));

    if (isManagePermissionRequired(newAcqUnits, acqUnitsFromStorage) && isUserDoesNotHaveDesiredPermission(MANAGE, permissions)){
      throw new HttpException(HttpStatus.HTTP_FORBIDDEN.toInt(), USER_HAS_NO_ACQ_PERMISSIONS);
    }
  }

  private boolean isManagePermissionRequired(Set<String> newAcqUnits, Set<String> acqUnitsFromStorage) {
    return !CollectionUtils.isEqualCollection(newAcqUnits, acqUnitsFromStorage);
  }

  private boolean isUserDoesNotHaveDesiredPermission(AcqDesiredPermissions acqPerm, List<String> permissions) {
    return !permissions.contains(acqPerm.getPermission());
  }

  private List<String> getProvidedPermissions(Map<String, String> headers) {
    return new JsonArray(headers.getOrDefault(OKAPI_HEADER_PERMISSIONS, Constants.EMPTY_ARRAY)).stream().
      map(Object::toString)
      .collect(Collectors.toList());
  }

  /**
   * Verifies if all acquisition units exist and active based on passed ids
   *
   * @param acqUnitIds list of unit IDs.
   * @return completable future completed successfully if all units exist and active or exceptionally otherwise
   */
  public CompletableFuture<Void> verifyIfUnitsAreActive(List<String> acqUnitIds, RequestContext requestContext) {
    if (acqUnitIds.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    return getUnitsByIds(acqUnitIds, requestContext).thenAccept(units -> {
      List<String> activeUnitIds = units.stream()
        .filter(unit -> !unit.getIsDeleted())
        .map(AcquisitionsUnit::getId)
        .collect(Collectors.toList());

      if (acqUnitIds.size() != activeUnitIds.size()) {
        throw new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(acqUnitIds, activeUnitIds));
      }
    });
  }
}
