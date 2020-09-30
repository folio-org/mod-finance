package org.folio.services;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LedgerDetailsService {

  public static final String SEARCH_CURRENT_FISCAL_YEAR_QUERY = "series==\"%s\" AND periodEnd>=%s sortBy periodStart";

  private final FiscalYearService fiscalYearService;
  private final LedgerService ledgerService;


  public LedgerDetailsService(FiscalYearService fiscalYearService, LedgerService ledgerService) {
    this.fiscalYearService = fiscalYearService;
    this.ledgerService = ledgerService;
  }

  public CompletableFuture<FiscalYear> getCurrentFiscalYear(String ledgerId, RequestContext requestContext) {
    return getFirstThreeFiscalYears(ledgerId, requestContext)
      .thenApply(this::defineCurrentFiscalYear);
  }

  public CompletableFuture<FiscalYear> getPlannedFiscalYear(String ledgerId, RequestContext requestContext) {
    return getFirstThreeFiscalYears(ledgerId, requestContext)
      .thenApply(firstTwoFiscalYears -> {
         FiscalYear curFY = defineCurrentFiscalYear(firstTwoFiscalYears);
         int size = firstTwoFiscalYears.size();
         int curIndex = firstTwoFiscalYears.indexOf(curFY);
         int nextIndex = curIndex + 1;
         if (curFY != null && nextIndex != size && size > 1) {
           return firstTwoFiscalYears.get(curIndex + 1);
         } else {
           return null;
         }
      });
  }

  private FiscalYear defineCurrentFiscalYear(List<FiscalYear> firstTwoFiscalYears) {
    if (CollectionUtils.isNotEmpty(firstTwoFiscalYears)) {
      if (firstTwoFiscalYears.size() > 1 && isOverlapped(firstTwoFiscalYears.get(0), firstTwoFiscalYears.get(1))) {
        return firstTwoFiscalYears.get(1);
      } else {
        return firstTwoFiscalYears.get(0);
      }
    } else {
      return null;
    }
  }

  private CompletableFuture<List<FiscalYear>> getFirstThreeFiscalYears(String ledgerId, RequestContext requestContext) {
    return ledgerService.retrieveLedgerById(ledgerId, requestContext)
      .thenCompose(ledger -> fiscalYearService.getFiscalYear(ledger.getFiscalYearOneId(), requestContext))
      .thenApply(this::buildCurrentFYQuery)
      .thenCompose(query -> fiscalYearService.getFiscalYears(3, 0, query, requestContext))
      .thenApply(FiscalYearsCollection::getFiscalYears);
  }

  private boolean isOverlapped(FiscalYear firstYear, FiscalYear secondYear) {
    Date now = new Date();
    return firstYear.getPeriodStart().before(now) && firstYear.getPeriodEnd().after(now)
      && secondYear.getPeriodStart().before(now) && secondYear.getPeriodEnd().after(now)
      && firstYear.getPeriodEnd().after(secondYear.getPeriodStart());
  }

  private String buildCurrentFYQuery(FiscalYear fiscalYearOne) {
    Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
    return String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, fiscalYearOne.getSeries(), now);
  }
}