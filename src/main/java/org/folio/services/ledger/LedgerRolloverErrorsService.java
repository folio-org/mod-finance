package org.folio.services.ledger;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_ERRORS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverError;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverErrorCollection;

import io.vertx.core.Future;

public class LedgerRolloverErrorsService {
  private final RestClient restClient;

  public LedgerRolloverErrorsService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<LedgerFiscalYearRolloverErrorCollection> getLedgerRolloverErrors(String query, int offset,
      int limit, String contentType, RequestContext requestContext) {
    if (contentType.toLowerCase().contains(APPLICATION_JSON.toLowerCase())) {
      var requestEntry = new RequestEntry(LEDGER_ROLLOVERS_ERRORS_STORAGE).withOffset(offset)
        .withLimit(limit)
        .withQuery(query);
      return restClient.get(requestEntry, LedgerFiscalYearRolloverErrorCollection.class, requestContext);
    } else {
      throw new HttpException(415, "Unsupported Media Type: " + contentType);
    }
  }

  public Future<LedgerFiscalYearRolloverError> createLedgerRolloverError(
      LedgerFiscalYearRolloverError rolloverError, RequestContext requestContext) {
    return restClient.post(resourcesPath(LEDGER_ROLLOVERS_ERRORS_STORAGE),rolloverError, LedgerFiscalYearRolloverError.class, requestContext);
  }

  public Future<Void> deleteLedgerRolloverError(String id, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(LEDGER_ROLLOVERS_ERRORS_STORAGE, id), requestContext);
  }

}
