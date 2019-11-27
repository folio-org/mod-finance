package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.resource.FinanceLedgers;
import org.folio.rest.util.HelperUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.FISCAL_YEARS_NOT_FOUND;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestEntities.FISCAL_YEAR;
import static org.folio.rest.util.TestEntities.LEDGER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CurrentFiscalYearTest extends ApiTestBase {

  @Test
  public void testOneFiscalYear() {

    FiscalYear year = new FiscalYear().withId(UUID.randomUUID().toString());

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(year.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(year));

    FiscalYear response = verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(year.getId()));
  }

  @Test
  public void testTwoOverlappedFiscalYears() {

    FiscalYear firstYear = new FiscalYear().withId(UUID.randomUUID().toString());
    firstYear.setPeriodEnd(new Date());

    FiscalYear secondYear = new FiscalYear().withId(UUID.randomUUID().toString());
    secondYear.setPeriodStart(Date.from(firstYear.getPeriodEnd().toInstant().minusSeconds(1000)));

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstYear.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(firstYear));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(secondYear));

    FiscalYear response = verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(secondYear.getId()));
  }

  @Test
  public void testTwoNonOverlappedFiscalYears() {

    FiscalYear firstYear = new FiscalYear().withId(UUID.randomUUID().toString());
    firstYear.setPeriodEnd(new Date());

    FiscalYear secondYear = new FiscalYear().withId(UUID.randomUUID().toString());
    secondYear.setPeriodStart(Date.from(firstYear.getPeriodEnd().toInstant().plusSeconds(1000)));

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(firstYear.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(firstYear));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(secondYear));

    FiscalYear response = verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, OK.getStatusCode()).as(FiscalYear.class);
    assertThat(response.getId(), is(firstYear.getId()));
  }

  @Test
  public void testFiscalYearNotFound() {

    FiscalYear year = new FiscalYear().withId(UUID.randomUUID().toString());
    year.setSeries(SERIES_DOES_NOT_EXIST);

    String ledgerId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId).withFiscalYearOneId(year.getId());
    addMockEntry(LEDGER.name(), JsonObject.mapFrom(ledger));
    addMockEntry(FISCAL_YEAR.name(), JsonObject.mapFrom(year));

    verifyGet(getCurrentFiscalYearEndpoint(ledgerId), APPLICATION_JSON, NOT_FOUND.getStatusCode());

  }

  @Test
  public void testGetFiscalYearLedgerNotFound() {
    verifyGet(getCurrentFiscalYearEndpoint(UUID.randomUUID().toString()), APPLICATION_JSON, NOT_FOUND.getStatusCode());
  }

  private String getCurrentFiscalYearEndpoint(String ledgerId) {
    return HelperUtils.getEndpoint(FinanceLedgers.class) + "/" + ledgerId + "/current-fiscal-year";
  }

}
