package org.folio.services;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;

public class GroupFundFiscalYearService {

  private final RestClient groupFundFiscalYearRestClient;

  public GroupFundFiscalYearService(RestClient groupFundFiscalYearRestClient) {
    this.groupFundFiscalYearRestClient = groupFundFiscalYearRestClient;
  }

  public CompletableFuture<GroupFundFiscalYear> createGroupFundFiscalYear(GroupFundFiscalYear groupFundFiscalYear, RequestContext requestContext) {
    return groupFundFiscalYearRestClient.post(groupFundFiscalYear, requestContext, GroupFundFiscalYear.class);
  }

  public CompletableFuture<GroupFundFiscalYearCollection> getGroupFundFiscalYears(String query, int offset, int limit, RequestContext requestContext) {
    return groupFundFiscalYearRestClient.get(query, offset, limit, requestContext,GroupFundFiscalYearCollection.class);
  }

  public CompletableFuture<Void> deleteGroupFundFiscalYear(String id, RequestContext requestContext) {
    return groupFundFiscalYearRestClient.delete(id, requestContext);
  }

  public CompletableFuture<Void> updateBudgetIdForGroupFundFiscalYears(Budget budget, RequestContext requestContext) {
    return getGroupFundFiscalYearCollection(budget.getFundId(), budget.getFiscalYearId(), requestContext)
      .thenCompose(gfFys -> processGroupFundFyUpdate(budget, gfFys, requestContext));
  }

  private CompletableFuture<Void> processGroupFundFyUpdate(Budget budget, GroupFundFiscalYearCollection gffyCollection, RequestContext requestContext) {
    CompletableFuture[] futures = gffyCollection.getGroupFundFiscalYears()
      .stream()
      .map(gffy -> groupFundFiscalYearRestClient.put(gffy.getId(), gffy.withBudgetId(budget.getId()), requestContext))
      .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(futures);
  }

  public CompletableFuture<GroupFundFiscalYearCollection> getGroupFundFiscalYearCollection(String fundId, String currentFYId, RequestContext requestContext) {
    String query = String.format("fundId==%s AND fiscalYearId==%s", fundId, currentFYId);
    return getGroupFundFiscalYears(query, 0, Integer.MAX_VALUE, requestContext);
  }

}
