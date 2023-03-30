package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.ErrorCodes.MISSING_FISCAL_YEAR_ID;
import static org.folio.rest.util.HelperUtils.OKAPI_URL;
import static org.folio.rest.util.HelperUtils.getEndpoint;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.helper.GroupsHelper;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.resource.FinanceGroups;
import org.folio.services.group.GroupExpenseClassTotalsService;
import org.folio.services.group.GroupService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class GroupsApi extends BaseApi implements FinanceGroups {

  private static final String GROUPS_LOCATION_PREFIX = getEndpoint(FinanceGroups.class) + "/%s";

  @Autowired
  private GroupExpenseClassTotalsService groupExpenseClassTotalsService;
  @Autowired
  private GroupService groupService;

  public GroupsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postFinanceGroups(Group entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    GroupsHelper helper = new GroupsHelper(okapiHeaders, vertxContext);
    helper.createGroup(entity)
      .thenAccept(type -> asyncResultHandler
        .handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL), String.format(GROUPS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void getFinanceGroups(String totalRecords, int offset, int limit, String query, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    GroupsHelper helper = new GroupsHelper(okapiHeaders, vertxContext);

    groupService.getGroupsWithAcqUnitsRestriction(query, offset, limit, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(groups -> asyncResultHandler.handle(succeededFuture(helper.buildOkResponse(groups))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void putFinanceGroupsById(String id, Group entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    GroupsHelper helper = new GroupsHelper(okapiHeaders, vertxContext);

    // Set id if this is available only in path
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      asyncResultHandler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateGroup(entity)
      .thenAccept(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void getFinanceGroupsById(String id, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    GroupsHelper helper = new GroupsHelper(okapiHeaders, vertxContext);
    helper.getGroup(id)
      .thenAccept(type -> asyncResultHandler.handle(succeededFuture(buildOkResponse(type))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void deleteFinanceGroupsById(String id, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    GroupsHelper helper = new GroupsHelper(okapiHeaders, vertxContext);
    helper.deleteGroup(id)
      .thenAccept(types -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }

  @Validate
  @Override
  public void getFinanceGroupsExpenseClassesTotalsById(String groupId, String fiscalYearId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    if (StringUtils.isEmpty(fiscalYearId)) {
      handleErrorResponse(asyncResultHandler, new HttpException(400, MISSING_FISCAL_YEAR_ID));
      return;
    }
    groupExpenseClassTotalsService.getExpenseClassTotals(groupId, fiscalYearId, new RequestContext(vertxContext, okapiHeaders))
      .thenAccept(obj -> asyncResultHandler.handle(succeededFuture(buildOkResponse(obj))))
      .exceptionally(fail -> handleErrorResponse(asyncResultHandler, fail));
  }


}
