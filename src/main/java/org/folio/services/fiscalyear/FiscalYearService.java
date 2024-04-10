package org.folio.services.fiscalyear;

import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;

import io.vertx.core.Future;

public class FiscalYearService {

  private static final Logger log = LogManager.getLogger();

  private final RestClient restClient;

  public FiscalYearService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<FiscalYear> createFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    return restClient.post(resourcesPath(FISCAL_YEARS_STORAGE), fiscalYear, FiscalYear.class, requestContext);
  }

  public Future<FiscalYearsCollection> getFiscalYearsWithoutAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(FISCAL_YEARS_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FiscalYearsCollection.class, requestContext);
  }

  public Future<FiscalYear> getFiscalYearById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(FISCAL_YEARS_STORAGE, id), FiscalYear.class, requestContext);
  }

  public Future<FiscalYear> getFiscalYearByFiscalYearCode(String fiscalYearCode, RequestContext requestContext) {
    String query = getFiscalYearByFiscalYearCode(fiscalYearCode);
    var requestEntry = new RequestEntry(resourcesPath(FISCAL_YEARS_STORAGE))
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), FiscalYearsCollection.class, requestContext)
      .map(collection -> {
        if (CollectionUtils.isNotEmpty(collection.getFiscalYears())) {
          return collection.getFiscalYears().get(0);
        }
        log.warn("getFiscalYearByFiscalYearCode:: Fiscal year by fiscalYearCode '{}' was not found", fiscalYearCode);
        throw new HttpException(400, FISCAL_YEARS_NOT_FOUND);
      });
  }

  public String getFiscalYearByFiscalYearCode(String fiscalYearCode) {
    return String.format("code=%s", fiscalYearCode);
  }

  public Future<Void> updateFiscalYear(FiscalYear fiscalYear, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(FISCAL_YEARS_STORAGE, fiscalYear.getId()), fiscalYear, requestContext);
  }

  public Future<Void> deleteFiscalYear(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(FISCAL_YEARS_STORAGE, id), requestContext);
  }

}
