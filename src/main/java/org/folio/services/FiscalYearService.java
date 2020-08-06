package org.folio.services;

import static org.folio.rest.helper.FundsHelper.SEARCH_CURRENT_FISCAL_YEAR_QUERY;

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

public class FiscalYearService {
  private final LedgerService ledgerService;
  private final RestClient fiscalYearRestClient;

  public FiscalYearService(LedgerService ledgerService, RestClient fiscalYearRestClient) {
    this.ledgerService = ledgerService;
    this.fiscalYearRestClient = fiscalYearRestClient;
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
    return fiscalYearRestClient.get(query, offset, limit, requestContext, FiscalYearsCollection.class);
  }

  public CompletableFuture<FiscalYear> getFiscalYear(String fiscalYearId, RequestContext requestContext) {
    return fiscalYearRestClient.getById(fiscalYearId, requestContext, FiscalYear.class);
  }

  private boolean isOverlapped(FiscalYear firstYear, FiscalYear secondYear) {
    Date now = new Date();
    return firstYear.getPeriodStart().before(now) && firstYear.getPeriodEnd().after(now)
      && secondYear.getPeriodStart().before(now) && secondYear.getPeriodEnd().after(now)
      && firstYear.getPeriodEnd().after(secondYear.getPeriodStart());
  }

  private CompletableFuture<List<FiscalYear>> getFirstTwoFiscalYears(String ledgerId, RequestContext requestContext) {
    return ledgerService.retrieveLedgerById(ledgerId, requestContext)
      .thenCompose(ledger -> getFiscalYear(ledger.getFiscalYearOneId(), requestContext))
      .thenApply(this::buildCurrentFYQuery)
      .thenCompose(query -> getFiscalYears(2, 0, query, requestContext))
      .thenApply(FiscalYearsCollection::getFiscalYears);
  }


  private String buildCurrentFYQuery(FiscalYear fiscalYearOne) {
    Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
    return String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, fiscalYearOne.getSeries(), now);
  }
}
