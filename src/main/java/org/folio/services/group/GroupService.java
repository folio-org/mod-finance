package org.folio.services.group;

import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.ACQUISITIONS_UNITS;
import static org.folio.rest.util.ResourcePathResolver.GROUPS;
import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.finance.GroupCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupsCollection;
import org.folio.services.protection.AcqUnitsService;

import io.vertx.core.Future;

public class GroupService {
  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;

  public GroupService(RestClient restClient, AcqUnitsService acqUnitsService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<GroupCollection> getGroupsWithAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .map(clause -> StringUtils.isBlank(query) ? clause : combineCqlExpressions("and", clause, query))
      .compose(effectiveQuery -> {
        // TODO: check all limits and offsets
        var requestEntry = new RequestEntry(resourcesPath(GROUPS))
          .withLimit(limit)
          .withOffset(offset);
        return restClient.get(requestEntry, GroupCollection.class, requestContext);
      });
  }

  public Future<Group> createGroup(Group group, RequestContext requestContext) {
    return restClient.post(resourcesPath(GROUPS), group, Group.class, requestContext);
  }

  public Future<GroupsCollection> getGroups(int limit, int offset, String query, RequestContext requestContext) {
    var requestEntry = new RequestEntry(GROUPS).withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry, GroupsCollection.class, requestContext);
  }

  public Future<Group> getGroupById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(GROUPS, id), Group.class, requestContext);
  }

  public Future<Void> deleteGroup(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(GROUPS, id), requestContext);
  }

  public Future<Void> updateGroup(Group group, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(GROUPS, group.getId()), group, requestContext);
  }
}
