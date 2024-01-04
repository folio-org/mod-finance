package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.services.ledger.LedgerDetailsService.SEARCH_CURRENT_FISCAL_YEAR_QUERY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.services.configuration.ConfigurationEntriesService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class LedgerDetailsServiceTest {
  private RequestContext requestContext;

  @InjectMocks
  private LedgerDetailsService ledgerDetailsService;

  @Mock
  private FiscalYearService fiscalYearService;
  @Mock
  private LedgerService ledgerService;
  @Mock
  private ConfigurationEntriesService configurationEntriesService;


  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @Test
  void testShouldReturnCurrentFiscalYearIfNoNextYearAndOneCurrent(VertxTestContext vertxTestContext) {
    //Given
    String curFiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(curFiscalId);
    FiscalYear fiscalYear = new FiscalYear().withId(curFiscalId);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Collections.singletonList(fiscalYear));

    doReturn(succeededFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(succeededFuture(fiscalYear)).when(fiscalYearService).getFiscalYearById(curFiscalId, requestContext);
    doReturn(succeededFuture(fyCol)).when(fiscalYearService).getFiscalYearsWithoutAcqUnitsRestriction(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(succeededFuture("UTC")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    var future = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actFY = result.result();
        assertThat(actFY.getId(), equalTo(fiscalYear.getId()));
        vertxTestContext.completeNow();
      });

  }

  @Test
  @DisabledIf("isWithinDateRange")
  void testShouldReturnCurrentFiscalYearIfNoNextYearAndTwoCurrentWithOverlapping(VertxTestContext vertxTestContext) throws ParseException {
    //Given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);

    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("31/12/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("03/01/" + year);
    Date sEndDate = sdf.parse("31/12/" + year);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstFiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear secFiscalYear = new FiscalYear().withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstFiscalYear, secFiscalYear));

    doReturn(succeededFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(succeededFuture(firstFiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(succeededFuture(fyCol)).when(fiscalYearService).getFiscalYearsWithoutAcqUnitsRestriction(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(succeededFuture("UTC")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    var future = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actFY = result.result();
        assertThat(actFY.getId(), equalTo(secFiscalYear.getId()));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldReturnCurrentFiscalYearIfNextYearAndCurrentYear(VertxTestContext vertxTestContext) throws ParseException {
    //Given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int nextYear = year + 1;
    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("03/31/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("01/01/" + nextYear);
    Date sEndDate = sdf.parse("31/12/" + nextYear);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear secfiscalYear = new FiscalYear().withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear, secfiscalYear));

    doReturn(succeededFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(succeededFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(succeededFuture(fyCol)).when(fiscalYearService).getFiscalYearsWithoutAcqUnitsRestriction(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(succeededFuture("UTC")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    var future = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actFY = result.result();
        assertThat(actFY.getId(), equalTo(firstfiscalYear.getId()));

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldReturnNextFiscalYearIfNextYearAndCurrentYear(VertxTestContext vertxTestContext) throws ParseException {
    //Given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int nextYear = year + 1;
    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("06/31/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("01/01/" + nextYear);
    Date sEndDate = sdf.parse("31/12/" + nextYear);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear nextfiscalYear = new FiscalYear().withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear, nextfiscalYear));

    doReturn(succeededFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(succeededFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(succeededFuture(fyCol)).when(fiscalYearService).getFiscalYearsWithoutAcqUnitsRestriction(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(succeededFuture("UTC")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    var future = ledgerDetailsService.getPlannedFiscalYear(ledgerId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actFY = result.result();
        assertThat(actFY.getId(), equalTo(nextfiscalYear.getId()));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldReturnNullNextFYIfOnlyCurrentYear(VertxTestContext vertxTestContext) throws ParseException {
    //Given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);

    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("06/31/" + year);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(List.of(firstfiscalYear));

    doReturn(succeededFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(succeededFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(succeededFuture(fyCol)).when(fiscalYearService).getFiscalYearsWithoutAcqUnitsRestriction(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(succeededFuture("UTC")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    var future = ledgerDetailsService.getPlannedFiscalYear(ledgerId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actFY = result.result();
        assertNull(actFY);
        vertxTestContext.completeNow();
      });
  }

  @Test
  @DisabledIf("isWithinDateRange")
  void testShouldReturnNullNextFiscalYearIfTwoCurrentWithOverlapping(VertxTestContext vertxTestContext) throws ParseException {
    //Given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);

    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("31/12/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("03/01/" + year);
    Date sEndDate = sdf.parse("31/12/" + year);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear secfiscalYear = new FiscalYear().withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear, secfiscalYear));

    doReturn(succeededFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(succeededFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(succeededFuture(fyCol)).when(fiscalYearService).getFiscalYearsWithoutAcqUnitsRestriction(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(succeededFuture("UTC")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    var future = ledgerDetailsService.getPlannedFiscalYear(ledgerId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actFY = result.result();
        assertNull(actFY);
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldReturnCurrentYearRespectTheTenantChosenTimezone(VertxTestContext vertxTestContext) throws ParseException {
    //Given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int nextYear = year + 1;

    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("31/12/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("01/01/" + nextYear);
    Date sEndDate = sdf.parse("31/12/" + nextYear);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withSeries("FY").withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear nextfiscalYear = new FiscalYear().withSeries("FY").withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear, nextfiscalYear));

    doReturn(succeededFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(succeededFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(succeededFuture(fyCol)).when(fiscalYearService).getFiscalYearsWithoutAcqUnitsRestriction(any(String.class), eq(0), eq(3), eq(requestContext));
    doReturn(succeededFuture("America/Los_Angeles")).when(configurationEntriesService).getSystemTimeZone(eq(requestContext));
    //When
    var future = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actFY = result.result();
        assertThat(actFY.getId(), equalTo(firstfiscalYear.getId()));
        LocalDate now = Instant.now().atZone(ZoneId.of("America/Los_Angeles")).toLocalDate();
        String expQuery = String.format(SEARCH_CURRENT_FISCAL_YEAR_QUERY, "FY", now);
        verify(fiscalYearService).getFiscalYearsWithoutAcqUnitsRestriction(eq(expQuery), eq(0), eq(3), eq(requestContext));

        vertxTestContext.completeNow();
      });
  }

  /**
   * This method checks if the current date falls within the date range from the 1st to the 3rd of January.
   * Tests using this method are disabled during this period due to specific test data considerations.
   *
   * @return true if the current date is within the specified range, otherwise false.
   */
  private boolean isWithinDateRange() {
    LocalDate currentDate = LocalDate.now();
    LocalDate startDate = LocalDate.of(currentDate.getYear(), 1, 1);
    LocalDate endDate = LocalDate.of(currentDate.getYear(), 1, 3);

    return currentDate.isAfter(startDate) && currentDate.isBefore(endDate);
  }
}
