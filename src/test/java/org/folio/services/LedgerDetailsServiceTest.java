package org.folio.services;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.services.fiscalyear.FiscalYearService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class LedgerDetailsServiceTest {
  private RequestContext requestContext;

  @InjectMocks
  private LedgerDetailsService ledgerDetailsService;

  @Mock
  private FiscalYearService fiscalYearService;
  @Mock
  private LedgerService ledgerService;

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
  void testShouldReturnCurrentFiscalYearIfNoNextYearAndOneCurrent() {
    //Given
    String curFiscalId = UUID.randomUUID().toString();
    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(curFiscalId);
    FiscalYear fiscalYear = new FiscalYear().withId(curFiscalId);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Collections.singletonList(fiscalYear));

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(fiscalYear)).when(fiscalYearService).getFiscalYearById(curFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext).join();
    //Then
    assertThat(actFY.getId(), equalTo(fiscalYear.getId()));
  }

  @Test
  void testShouldReturnCurrentFiscalYearIfNoNextYearAndTwoCurrentWithOverlapping() throws ParseException {
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

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(firstFiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext).join();
    //Then
    assertThat(actFY.getId(), equalTo(secFiscalYear.getId()));
  }

  @Test
  void testShouldReturnCurrentFiscalYearIfNextYearAndCurrentYear() throws ParseException {
    //Given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);

    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("03/31/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("01/01/" + year + 1);
    Date sEndDate = sdf.parse("31/12/" + year + 1);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear secfiscalYear = new FiscalYear().withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear, secfiscalYear));

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getCurrentFiscalYear(ledgerId, requestContext).join();
    //Then
    assertThat(actFY.getId(), equalTo(firstfiscalYear.getId()));
  }

  @Test
  void testShouldReturnNextFiscalYearIfNextYearAndCurrentYear() throws ParseException {
    //Given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);

    String firstCurFiscalId = UUID.randomUUID().toString();
    Date fStartDate = sdf.parse("01/01/" + year);
    Date fEndDate = sdf.parse("06/31/" + year);

    String secCurFiscalId = UUID.randomUUID().toString();
    Date sStartDate = sdf.parse("01/01/" + year + 1);
    Date sEndDate = sdf.parse("31/12/" + year + 1);

    String ledgerId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstCurFiscalId);
    FiscalYear firstfiscalYear = new FiscalYear().withId(firstCurFiscalId).withPeriodStart(fStartDate).withPeriodEnd(fEndDate);
    FiscalYear nextfiscalYear = new FiscalYear().withId(secCurFiscalId).withPeriodStart(sStartDate).withPeriodEnd(sEndDate);
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear, nextfiscalYear));

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getPlannedFiscalYear(ledgerId, requestContext).join();
    //Then
    assertThat(actFY.getId(), equalTo(nextfiscalYear.getId()));
  }

  @Test
  void testShouldReturnNullNextFYIfOnlyCurrentYear() throws ParseException {
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
    FiscalYearsCollection fyCol = new FiscalYearsCollection().withFiscalYears(Arrays.asList(firstfiscalYear));

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getPlannedFiscalYear(ledgerId, requestContext).join();
    //Then
    assertNull(actFY);
  }

  @Test
  void testShouldReturnNullNextFiscalYearIfTwoCurrentWithOverlapping() throws ParseException {
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

    doReturn(completedFuture(ledger)).when(ledgerService).retrieveLedgerById(ledgerId, requestContext);
    doReturn(completedFuture(firstfiscalYear)).when(fiscalYearService).getFiscalYearById(firstCurFiscalId, requestContext);
    doReturn(completedFuture(fyCol)).when(fiscalYearService).getFiscalYears(any(String.class), eq(0), eq(3), eq(requestContext));
    //When
    FiscalYear actFY = ledgerDetailsService.getPlannedFiscalYear(ledgerId, requestContext).join();
    //Then
    assertNull(actFY);
  }
}
