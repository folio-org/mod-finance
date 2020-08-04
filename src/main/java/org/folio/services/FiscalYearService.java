package org.folio.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FiscalYearService {
  private static final Logger logger = LoggerFactory.getLogger(FiscalYearService.class);
  private static final String SEARCH_CURRENT_FISCAL_YEAR_QUERY = "series==\"%s\" AND periodEnd>=%s sortBy periodStart";

  private final RestClient fiscalYearStorageRestClient;
  private final LedgerService ledgerService;

  public FiscalYearService(RestClient fiscalYearStorageRestClient, LedgerService ledgerService) {
    this.fiscalYearStorageRestClient = fiscalYearStorageRestClient;
    this.ledgerService = ledgerService;
  }

  public CompletableFuture<FiscalYear> getCurrentFiscalYear(String ledgerId, RequestContext requestContext) {
    return getFirstTwoFiscalYears(ledgerId, requestContext)
      .thenApply(firstTwoFiscalYears -> {
        if(CollectionUtils.isNotEmpty(firstTwoFiscalYears)) {
          if(firstTwoFiscalYears.size() > 1 && isOverlapped(firstTwoFiscalYears.get(0), firstTwoFiscalYears.get(1))) {
            return firstTwoFiscalYears.get(1);
          } else {
            return firstTwoFiscalYears.get(0);
          }
        } else {
          return null;
        }
      });
  }

  public CompletableFuture<FiscalYearsCollection> getFiscalYears(int limit, int offset, String query, RequestContext requestContext) {
    return fiscalYearStorageRestClient.get(query, limit, offset, requestContext, FiscalYearsCollection.class);
  }

  public CompletableFuture<FiscalYear> getFiscalYear(String fiscalYearId, RequestContext requestContext) {
    return fiscalYearStorageRestClient.getById(fiscalYearId, requestContext, FiscalYear.class);
  }

  private boolean isOverlapped(FiscalYear firstYear, FiscalYear secondYear) {
    Date now = new Date();
    return firstYear.getPeriodStart().before(now) && firstYear.getPeriodEnd().after(now)
      && secondYear.getPeriodStart().before(now) && secondYear.getPeriodEnd().after(now)
      && firstYear.getPeriodEnd().after(secondYear.getPeriodStart());
  }

  private CompletableFuture<List<FiscalYear>> getFirstTwoFiscalYears(String ledgerId, RequestContext requestContext) {
    return ledgerService.getLedger(ledgerId, requestContext)
      .thenCompose(ledger -> getFiscalYear(ledger.getFiscalYearOneId(), requestContext))
      .thenApply(this::buildCurrentFYQuery)
      .thenCompose(endpoint -> getFiscalYears(2, 0, endpoint, requestContext))
      .thenApply(FiscalYearsCollection::getFiscalYears);
  }

  private String buildCurrentFYQuery(FiscalYear fiscalYearOne) {
    Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
    return String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, fiscalYearOne.getSeries(), now);
  }
}
