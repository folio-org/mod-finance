package org.folio.services.group;

import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearBatchRequest;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;

import io.vertx.core.Future;

public class GroupFundFiscalYearService {

  private final RestClient restClient;

  public GroupFundFiscalYearService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<GroupFundFiscalYear> createGroupFundFiscalYear(GroupFundFiscalYear groupFundFiscalYear, RequestContext requestContext) {
    return restClient.post(resourcesPath(GROUP_FUND_FISCAL_YEARS), groupFundFiscalYear, GroupFundFiscalYear.class, requestContext);
  }

  public Future<GroupFundFiscalYearCollection> getGroupFundFiscalYears(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(GROUP_FUND_FISCAL_YEARS))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), GroupFundFiscalYearCollection.class, requestContext);
  }

  public Future<Void> deleteGroupFundFiscalYear(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(GROUP_FUND_FISCAL_YEARS, id), requestContext);
  }

  public Future<GroupFundFiscalYearCollection> getGroupFundFiscalYearCollection(String fundId, String currentFYId, RequestContext requestContext) {
    String query = String.format("fundId==%s AND fiscalYearId==%s", fundId, currentFYId);
    return getGroupFundFiscalYears(query, 0, Integer.MAX_VALUE, requestContext);
  }

  public Future<List<GroupFundFiscalYear>> getGroupFundFiscalYearsWithBudgetId(String groupId, String fiscalYearId, RequestContext requestContext) {
    String query = String.format("groupId==%s AND fiscalYearId==%s AND budgetId=*", groupId, fiscalYearId);
    return getGroupFundFiscalYears(query, 0, Integer.MAX_VALUE, requestContext)
      .map(GroupFundFiscalYearCollection::getGroupFundFiscalYears);
  }

  public Future<GroupFundFiscalYearCollection> getGroupFundFiscalYearsByFundIds(GroupFundFiscalYearBatchRequest batchRequest, RequestContext requestContext) {
    List<String> fundIds = batchRequest.getFundIds();
    if (CollectionUtils.isEmpty(fundIds)) {
      return Future.succeededFuture(new GroupFundFiscalYearCollection().withGroupFundFiscalYears(List.of()).withTotalRecords(0));
    }

    return restClient.postBatch(resourcesPath(GROUP_FUND_FISCAL_YEARS) + "/batch",
      batchRequest,
      GroupFundFiscalYearCollection.class,
      requestContext);
  }
}
