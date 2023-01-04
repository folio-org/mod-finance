package org.folio.services.ledger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.services.configuration.ConfigurationEntriesService;
import org.folio.services.fiscalyear.FiscalYearService;

public class LedgerDetailsService {

  public static final String SEARCH_CURRENT_FISCAL_YEAR_QUERY = "series==\"%s\" AND periodEnd>=%s sortBy periodStart";

  private final FiscalYearService fiscalYearService;
  private final LedgerService ledgerService;
  private ConfigurationEntriesService configurationEntriesService;

  public LedgerDetailsService(FiscalYearService fiscalYearService, LedgerService ledgerService, ConfigurationEntriesService configurationEntriesService) {
    this.fiscalYearService = fiscalYearService;
    this.ledgerService = ledgerService;
    this.configurationEntriesService = configurationEntriesService;
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
      .thenCompose(ledger -> fiscalYearService.getFiscalYearById(ledger.getFiscalYearOneId(), requestContext))
      .thenCombine(configurationEntriesService.getSystemTimeZone(requestContext), this::buildCurrentFYQuery)
      .thenCompose(query -> fiscalYearService.getFiscalYearsWithoutAcqUnitsRestriction(query, 0, 3, requestContext))
      .thenApply(FiscalYearsCollection::getFiscalYears);
  }

  private boolean isOverlapped(FiscalYear firstYear, FiscalYear secondYear) {
    Date now = new Date();
    return firstYear.getPeriodStart().before(now) && firstYear.getPeriodEnd().after(now)
      && secondYear.getPeriodStart().before(now) && secondYear.getPeriodEnd().after(now)
      && firstYear.getPeriodEnd().after(secondYear.getPeriodStart());
  }

  private String buildCurrentFYQuery(FiscalYear fiscalYearOne, String timeZone) {
    LocalDate now = Instant.now().atZone(ZoneId.of(timeZone)).toLocalDate();
    return String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, fiscalYearOne.getSeries(), now);
  }
}
