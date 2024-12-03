package org.folio.services.financedata;

import static org.folio.rest.util.HelperUtils.combineCqlExpressions;
import static org.folio.rest.util.ResourcePathResolver.FINANCE_DATA_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import io.vertx.core.Future;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.services.protection.AcqUnitsService;

public class FinanceDataService {
  private final RestClient restClient;
  private final AcqUnitsService acqUnitsService;

  public FinanceDataService(RestClient restClient, AcqUnitsService acqUnitsService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<FyFinanceDataCollection> getFinanceDataWithAcqUnitsRestriction(String query, int offset, int limit,
                                                                               RequestContext requestContext) {
    return acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(requestContext)
      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .compose(effectiveQuery -> getFinanceData(effectiveQuery, offset, limit, requestContext));
  }

  private Future<FyFinanceDataCollection> getFinanceData(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FINANCE_DATA_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FyFinanceDataCollection.class, requestContext);
  }

  public Future<Void> putFinanceData(FyFinanceDataCollection entity, RequestContext requestContext) {
    // 1. Validate
    validateFinanceData(entity);

    // 2. Apply calculation with allocating new value(allocation transaction should be created):
    calculateAllocation();

    // 3. Send request to update finance data
    updateFinanceData(entity, requestContext);

    // 4. Invoke Bulk Transactions API to create allocation transactions
    processTransaction(entity, requestContext);

    // 5. Invoke storage actions logs endpoint to save request payload + status metadata + recordsCount
    processLogs(entity, requestContext);
    return null;
  }

  private void validateFinanceData(FyFinanceDataCollection entity) {
    // validate entity
  }

  private void calculateAllocation() {
    // calculate allocation
  }

  private void updateFinanceData(FyFinanceDataCollection entity, RequestContext requestContext) {
    // send request to update finance data
  }

  private void processTransaction(FyFinanceDataCollection entity, RequestContext requestContext) {
    // invoke Bulk Transactions API to create allocation transactions
  }

  private void processLogs(FyFinanceDataCollection entity, RequestContext requestContext) {
    // invoke storage actions logs endpoint to save request payload + status metadata + recordsCount
  }
}

