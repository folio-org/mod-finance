package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.helper.LedgersHelper.LEDGER_ID_AND_FISCAL_YEAR_ID;
import static org.folio.rest.util.ErrorCodes.LEDGER_FY_NOT_FOUND;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.MockServer.getCollectionRecords;
import static org.folio.rest.util.MockServer.getQueryParams;
import static org.folio.rest.util.MockServer.getRecordById;
import static org.folio.rest.util.ResourcePathResolver.LEDGER_FYS_STORAGE;
import static org.folio.rest.util.TestEntities.LEDGER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.folio.rest.acq.model.finance.LedgerFY;
import org.folio.rest.acq.model.finance.LedgerFYCollection;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Ledger;
import org.junit.jupiter.api.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class LedgerSummaryTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(LedgerSummaryTest.class);

  @Test
  public void testGetLedgerByIdWithSummary() {
    logger.info("=== Test Get Ledger by id with summary ===");
    String fiscalYearId = "ac2164c7-ba3d-1bc2-a12c-e35ceccbfaf2";
    Ledger ledger = verifyGetWithParam(LEDGER.getEndpointWithDefaultId(), APPLICATION_JSON, OK.getStatusCode(), "fiscalYear",
      fiscalYearId).as(Ledger.class);

    // Make sure that correct storage endpoint was used
    assertThat(getRecordById(LEDGER.name()), hasSize(1));
    assertThat(getCollectionRecords(LEDGER_FYS_STORAGE), hasSize(1));
    assertThat(getQueryParams(LEDGER_FYS_STORAGE), hasSize(1));
    assertThat(getQueryParams(LEDGER_FYS_STORAGE).get(0), equalTo(String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledger.getId(), fiscalYearId)));
    LedgerFY ledgerFY = getCollectionRecords(LEDGER_FYS_STORAGE).get(0).mapTo(LedgerFYCollection.class).getLedgerFY().get(0);

    assertThat(ledger.getAllocated(), equalTo(ledgerFY.getAllocated()));
    assertThat(ledger.getAvailable(), equalTo(ledgerFY.getAvailable()));
    assertThat(ledger.getUnavailable(), equalTo(ledgerFY.getUnavailable()));
  }

  @Test
  public void testGetLedgerByIdWithSummaryLedgerFyEmpty() {
    logger.info("=== Test Get Ledger by id with summary, ledger FY not found ===");
    Errors errors = verifyGetWithParam(LEDGER.getEndpointWithDefaultId(), APPLICATION_JSON, BAD_REQUEST.getStatusCode(), "fiscalYear",
      ID_DOES_NOT_EXIST).as(Errors.class);

    // Make sure that correct storage endpoint was used

    assertThat(getRecordById(LEDGER.name()), hasSize(1));
    String ledgerId = getRecordById(LEDGER.name()).get(0).getString(ID);
    assertThat(getQueryParams(LEDGER_FYS_STORAGE), hasSize(1));
    assertThat(getQueryParams(LEDGER_FYS_STORAGE).get(0), equalTo(String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledgerId, ID_DOES_NOT_EXIST)));

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), equalTo(LEDGER_FY_NOT_FOUND.getCode()));
  }

  @Test
  public void testGetLedgerByIdWithSummaryInternalServerError() {
    logger.info("=== Test Get Ledger by id with summary, internal server error ===");
    Errors errors = verifyGetWithParam(LEDGER.getEndpointWithDefaultId(), APPLICATION_JSON, INTERNAL_SERVER_ERROR.getStatusCode(), "fiscalYear",
      ID_FOR_INTERNAL_SERVER_ERROR).as(Errors.class);

    // Make sure that correct storage endpoint was used

    assertThat(getRecordById(LEDGER.name()), hasSize(1));
    String ledgerId = getRecordById(LEDGER.name()).get(0).getString(ID);
    assertThat(getQueryParams(LEDGER_FYS_STORAGE), hasSize(1));
    assertThat(getQueryParams(LEDGER_FYS_STORAGE).get(0), equalTo(String.format(LEDGER_ID_AND_FISCAL_YEAR_ID, ledgerId, ID_FOR_INTERNAL_SERVER_ERROR)));

    assertThat(errors.getErrors(), hasSize(1));
  }

}
