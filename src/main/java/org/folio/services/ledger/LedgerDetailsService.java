package org.folio.services.ledger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.services.configuration.CommonSettingsService;
import org.folio.services.fiscalyear.FiscalYearService;

import io.vertx.core.Future;

public class LedgerDetailsService {

  private static final Logger log = LogManager.getLogger();
  public static final String SEARCH_CURRENT_FISCAL_YEAR_QUERY = "series==\"%s\" AND periodEnd>=%s sortBy periodStart";

  private final FiscalYearService fiscalYearService;
  private final LedgerService ledgerService;
  private final CommonSettingsService commonSettingsService;

  public LedgerDetailsService(FiscalYearService fiscalYearService, LedgerService ledgerService, CommonSettingsService commonSettingsService) {
    this.fiscalYearService = fiscalYearService;
    this.ledgerService = ledgerService;
    this.commonSettingsService = commonSettingsService;
  }

  public Future<FiscalYear> getCurrentFiscalYear(String ledgerId, RequestContext requestContext) {
    return getFirstThreeFiscalYears(ledgerId, requestContext)
      .map(this::defineCurrentFiscalYear);
  }

  public Future<FiscalYear> getPlannedFiscalYear(String ledgerId, RequestContext requestContext) {
    log.debug("getPlannedFiscalYear:: Getting planned fiscal year with ledgerId={}", ledgerId);
    return getFirstThreeFiscalYears(ledgerId, requestContext)
      .map(firstTwoFiscalYears -> {
        FiscalYear curFY = defineCurrentFiscalYear(firstTwoFiscalYears);
        int size = firstTwoFiscalYears.size();
        int curIndex = firstTwoFiscalYears.indexOf(curFY);
        int nextIndex = curIndex + 1;
        if (curFY != null && nextIndex != size && size > 1) {
          log.info("getPlannedFiscalYear:: curFY '{}', nextIndex '{}' is not null and size '{}' is greater than 1, returning (currentIndex '{}' + 1) - element", curFY, nextIndex, size, curIndex);
          return firstTwoFiscalYears.get(curIndex + 1);
        } else {
          log.warn("getPlannedFiscalYear:: curFY '{}' or/and nextIndex '{}' is null or/and size '{}' is less than 1, so returning null", curFY, nextIndex, size);
          return null;
        }
      });
  }

  private FiscalYear defineCurrentFiscalYear(List<FiscalYear> firstTwoFiscalYears) {
    log.debug("defineCurrentFiscalYear:: Defining current fiscal year");
    if (CollectionUtils.isNotEmpty(firstTwoFiscalYears)) {
      if (firstTwoFiscalYears.size() > 1 && isOverlapped(firstTwoFiscalYears.get(0), firstTwoFiscalYears.get(1))) {
        log.info("defineCurrentFiscalYear:: Returning second fiscal year because of overlapping '{}'", firstTwoFiscalYears.get(1).getId());
        return firstTwoFiscalYears.get(1);
      } else {
        log.info("defineCurrentFiscalYear:: Returning first fiscal year '{}'", firstTwoFiscalYears.get(0).getId());
        return firstTwoFiscalYears.get(0);
      }
    }
    log.warn("defineCurrentFiscalYear:: firstTwoFiscalYears is empty");
    return null;
  }

  private Future<List<FiscalYear>> getFirstThreeFiscalYears(String ledgerId, RequestContext requestContext) {
    return ledgerService.retrieveLedgerById(ledgerId, requestContext)
      .compose(ledger -> fiscalYearService.getFiscalYearById(ledger.getFiscalYearOneId(), requestContext))
      .compose(fyOne -> commonSettingsService.getSystemTimeZone(requestContext)
        .map(timeZone -> this.buildCurrentFYQuery(fyOne, timeZone)))
      .compose(query -> fiscalYearService.getFiscalYearsWithoutAcqUnitsRestriction(query, 0, 3, requestContext))
      .map(FiscalYearsCollection::getFiscalYears);
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
