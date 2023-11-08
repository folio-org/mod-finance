package org.folio.rest.helper;

import java.util.Map;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupCollection;
import org.folio.services.group.GroupService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class GroupsHelper extends AbstractHelper {

  @Autowired
  private GroupService groupService;

  public GroupsHelper(Map<String, String> okapiHeaders, Context ctx) {
    super(okapiHeaders, ctx);
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  public Future<Group> createGroup(Group group, RequestContext requestContext) {
    return groupService.createGroup(group, requestContext);
  }

  public Future<GroupCollection> getGroups(String query, int offset, int limit, RequestContext requestContext) {
    return groupService.getGroupsWithAcqUnitsRestriction(query, offset, limit, requestContext);
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

