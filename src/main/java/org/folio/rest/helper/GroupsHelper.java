package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.GROUPS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import io.vertx.core.Future;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupsCollection;

import io.vertx.core.Context;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

public class GroupsHelper extends AbstractHelper {

  private final RestClient restClient;
  private static final String GET_GROUPS_BY_QUERY = resourcesPath(GROUPS) + SEARCH_PARAMS;

  public GroupsHelper(Map<String, String> okapiHeaders, Context ctx) {
    super(okapiHeaders, ctx);
    this.restClient = new RestClient();
  }

  public Future<Group> createGroup(Group group, RequestContext requestContext) {
    return restClient.post(resourcesPath(GROUPS), group, Group.class, requestContext);
  }

  public Future<GroupsCollection> getGroups(int limit, int offset, String query) {
    String endpoint = String.format(GET_GROUPS_BY_QUERY, limit, offset, buildQueryParam(query, logger));
    return reshandleGetRequest(endpoint)
      .map(json -> FolioVertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(GroupsCollection.class)));
  }

  public Future<Group> getGroup(String id) {
    return handleGetRequest(resourceByIdPath(GROUPS, id))
      .map(json -> json.mapTo(Group.class));
  }

  public Future<Void> updateGroup(Group group) {
    return handleUpdateRequest(resourceByIdPath(GROUPS, group.getId()), group);
  }

  public Future<Void> deleteGroup(String id) {
    return handleDeleteRequest(resourceByIdPath(GROUPS, id));
  }
}

