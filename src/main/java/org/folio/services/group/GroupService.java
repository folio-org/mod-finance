package org.folio.services.group;

import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.ACQUISITIONS_UNITS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Future;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.finance.GroupCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.services.protection.AcqUnitsService;

public class GroupService {
  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;

  public GroupService(RestClient restClient, AcqUnitsService acqUnitsService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<GroupCollection> getGroupsWithAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .compose(effectiveQuery -> {
        // TODO: check all limits and offsets
        var requestEntry = new RequestEntry(resourcesPath(ACQUISITIONS_UNITS))
          .withLimit(limit)
          .withOffset(offset);
        return restClient.get(requestEntry, GroupCollection.class, requestContext);
      });
  }
}
