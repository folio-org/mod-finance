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
  public static final String ID = "id";

  public FinanceDataService(RestClient restClient, AcqUnitsService acqUnitsService) {
    this.restClient = restClient;
    this.acqUnitsService = acqUnitsService;
  }

  public Future<FyFinanceDataCollection> getFinanceDataWithAcqUnitsRestriction(String query, int offset, int limit,
                                                                 RequestContext requestContext) {
    return restClient.get(new RequestEntry(resourcesPath(FINANCE_DATA_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query)
      .buildEndpoint(), FyFinanceDataCollection.class, requestContext);
//    return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
//      .map(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
//      .map(effectiveQuery -> new RequestEntry(resourcesPath(FINANCE_DATA_STORAGE))
//        .withOffset(offset)
//        .withLimit(limit)
//        .withQuery(effectiveQuery)
//      )
//      .compose(requestEntry -> restClient.get(requestEntry.buildEndpoint(), FyFinanceDataCollection.class, requestContext));
  }
}

