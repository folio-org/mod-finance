package org.folio.services.group;

import static org.folio.rest.util.HelperUtils.combineCqlExpressions;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.finance.GroupCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.services.protection.AcqUnitsService;

public class GroupService {
  private final RestClient groupStorageRestClient;
  private final AcqUnitsService acqUnitsService;

  public GroupService(RestClient groupStorageRestClient, AcqUnitsService acqUnitsService) {
    this.groupStorageRestClient = groupStorageRestClient;
    this.acqUnitsService = acqUnitsService;
  }

  public CompletableFuture<GroupCollection> getGroupsWithAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .thenApply(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .thenCompose(effectiveQuery -> groupStorageRestClient.get(effectiveQuery, offset, limit, requestContext, GroupCollection.class));
  }
}
