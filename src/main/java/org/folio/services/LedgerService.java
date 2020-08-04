package org.folio.services;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.acq.model.finance.LedgerFY;
import org.folio.rest.acq.model.finance.LedgerFYCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.util.HelperUtils;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.helper.LedgersHelper.LEDGER_ID_AND_FISCAL_YEAR_ID;
import static org.folio.rest.util.ErrorCodes.LEDGER_FY_NOT_FOUND;
import static org.folio.rest.util.ResourcePathResolver.LEDGER_FYS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

public class LedgerService {
  //private static final String GET_LEDGERSFY_BY_QUERY = resourcesPath(LEDGER_FYS_STORAGE) + SE;

  private final RestClient ledgerStorageRestClient;
  private final RestClient ledgerFYStorageRestClient;

  public LedgerService(RestClient ledgerStorageRestClient, RestClient ledgerFYStorageRestClient) {
    this.ledgerStorageRestClient = ledgerStorageRestClient;
    this.ledgerFYStorageRestClient = ledgerFYStorageRestClient;
  }

  public CompletableFuture<Ledger> retrieveLedgerById(String ledgerId, RequestContext requestContext) {
    return ledgerStorageRestClient.getById(ledgerId, requestContext, Ledger.class);
  }

  public CompletableFuture<Ledger> getLedgerWithSummary(String ledgerId, String fiscalYearId, RequestContext requestContext) {
    CompletableFuture<Ledger> future = retrieveLedgerById(ledgerId, requestContext);
    if (isEmpty(fiscalYearId)) {
      return future;
    } else {
      return future.thenCompose(ledger -> getLedgerFY(ledgerId, fiscalYearId, requestContext)
        .thenApply(ledgerFY -> getLedgerWithTotals(ledgerFY, ledger)));
    }
  }

  private CompletableFuture<LedgerFY> getLedgerFY(String ledgerId, String fiscalYearId , RequestContext requestContext) {
    String query = String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledgerId, fiscalYearId);
  //  String endpoint = String.format(GET_LEDGERSFY_BY_QUERY, 1, 0, HelperUtils.buildQueryParam(query, logger), lang);
    return ledgerFYStorageRestClient.get(query, 0, 1, requestContext, LedgerFYCollection.class)
      .thenApply(ledgerFYs -> {
//        LedgerFYCollection ledgerFYs = entries.mapTo(LedgerFYCollection.class);
        if (CollectionUtils.isNotEmpty(ledgerFYs.getLedgerFY())) {
          return ledgerFYs.getLedgerFY().get(0);
        }
        throw new HttpException(BAD_REQUEST.getStatusCode(), LEDGER_FY_NOT_FOUND);
      });
  }

  private Ledger getLedgerWithTotals(LedgerFY ledgerFY, Ledger ledger) {
    return ledger.withAllocated(ledgerFY.getAllocated())
      .withAvailable(ledgerFY.getAvailable())
      .withUnavailable(ledgerFY.getUnavailable());
  }

}
