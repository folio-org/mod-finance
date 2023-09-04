package org.folio.rest.helper;

import java.util.Map;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupsCollection;
import org.folio.services.group.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class GroupsHelper extends AbstractHelper {

  @Autowired
  GroupService groupService;

  public GroupsHelper(Map<String, String> okapiHeaders, Context ctx) {
    super(okapiHeaders, ctx);
  }

  public Future<Group> createGroup(Group group, RequestContext requestContext) {
    return groupService.createGroup(group, requestContext);
  }

  public Future<GroupsCollection> getGroups(int limit, int offset, String query, RequestContext requestContext) {
    return groupService.getGroups(limit, offset, query, requestContext);
  }
  public Future<Group> getGroup(String id, RequestContext requestContext) {
    return groupService.getGroupById(id, requestContext);
  }

  public Future<Void> updateGroup(Group group, RequestContext requestContext) {
    return groupService.updateGroup(group, requestContext);
  }

  public Future<Void> deleteGroup(String id, RequestContext requestContext) {
    return groupService.deleteGroup(id, requestContext);
  }
}

