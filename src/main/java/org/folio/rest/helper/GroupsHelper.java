package org.folio.rest.helper;

import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.HelperUtils.handleGetRequest;
import static org.folio.rest.util.ResourcePathResolver.GROUPS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupsCollection;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class GroupsHelper extends AbstractHelper {


  private static final String GET_GROUPS_BY_QUERY = resourcesPath(GROUPS) + SEARCH_PARAMS;

  public GroupsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Group> createGroup(Group group) {
    return handleCreateRequest(resourcesPath(GROUPS), group).thenApply(group::withId);
  }

  public CompletableFuture<GroupsCollection> getGroups(int limit, int offset, String query) {
    String endpoint = String.format(GET_GROUPS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint, httpClient, ctx, okapiHeaders, logger)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(GroupsCollection.class)));
  }

  public CompletableFuture<Group> getGroup(String id) {
    return handleGetRequest(resourceByIdPath(GROUPS, id, lang), httpClient, ctx, okapiHeaders, logger)
      .thenApply(json -> json.mapTo(Group.class));
  }

  public CompletableFuture<Void> updateGroup(Group group) {
    return handleUpdateRequest(resourceByIdPath(GROUPS, group.getId(), lang), group);
  }

  public CompletableFuture<Void> deleteGroup(String id) {
    return handleDeleteRequest(resourceByIdPath(GROUPS, id, lang));
  }
}

