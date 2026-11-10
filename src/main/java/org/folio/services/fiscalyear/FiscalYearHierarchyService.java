package org.folio.services.fiscalyear;

import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEAR_HIERARCHY_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.FiscalYearHierarchyCollection;

import io.vertx.core.Future;

public class FiscalYearHierarchyService {
  private final RestClient restClient;

  public FiscalYearHierarchyService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<FiscalYearHierarchyCollection> retrieveFiscalYearHierarchy(String fiscalYearId, String ledgerStatus,
      String groupStatus, String fundStatus, String budgetStatus, String expenseClassStatus, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FISCAL_YEAR_HIERARCHY_STORAGE))
      .withQueryParameter("fiscalYearId", fiscalYearId);
    addIfNotBlank(requestEntry, "ledgerStatus", ledgerStatus);
    addIfNotBlank(requestEntry, "groupStatus", groupStatus);
    addIfNotBlank(requestEntry, "fundStatus", fundStatus);
    addIfNotBlank(requestEntry, "budgetStatus", budgetStatus);
    addIfNotBlank(requestEntry, "expenseClassStatus", expenseClassStatus);
    return restClient.get(requestEntry.buildEndpoint(), FiscalYearHierarchyCollection.class, requestContext);
  }

  private void addIfNotBlank(RequestEntry requestEntry, String key, String value) {
    if (StringUtils.isNotBlank(value)) {
      requestEntry.withQueryParameter(key, value);
    }
  }
}
