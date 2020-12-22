package org.folio.rest.impl;

import org.folio.ApiTestSuite;
import org.folio.rest.acq.model.finance.LedgerFiscalYearRolloverError;
import org.folio.rest.acq.model.finance.LedgerFiscalYearRolloverErrorCollection;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.util.TestEntities;
import org.folio.services.LedgerRolloverErrorsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.RestTestUtils.*;
import static org.folio.rest.util.TestConfig.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LedgerRolloverErrorsApiTest {

  @Autowired
  private LedgerRolloverErrorsService mockLedgerRolloverErrorsService;

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
    }
    initSpringContext(LedgerRolloverErrorsApiTest.ContextConfiguration.class);
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterEach
  void resetMocks() {
    reset(mockLedgerRolloverErrorsService);
  }

  @Test
  void shouldReturnLedgerRolloverErrorsCollectionWhenCallGetAndRolloverErrorsServiceReturnLedgerRolloverErrors() {
    LedgerFiscalYearRolloverErrorCollection ledgerErrors = new LedgerFiscalYearRolloverErrorCollection()
      .withTotalRecords(1)
      .withLedgerFiscalYearRolloverErrors(Arrays.asList(new LedgerFiscalYearRolloverError()));

    when(mockLedgerRolloverErrorsService.retrieveLedgersRolloverErrors(any(), anyInt(), anyInt(), any()))
      .thenReturn(CompletableFuture.completedFuture(ledgerErrors));

    // When call getFinanceLedgerRollovers successfully
    LedgerFiscalYearRolloverErrorCollection rolloverErrorCollection = verifyGet(TestEntities.LEDGER_ROLLOVER_ERRORS.getEndpoint(), APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverErrorCollection.class);

    // Then return LedgerFiscalYearRolloverCollection
    assertThat(rolloverErrorCollection.getLedgerFiscalYearRolloverErrors(), hasSize(1));
  }

  @Test
  void shouldReturnErrorWhenCallGetAndRolloverErrorsServiceReturnError() {

    CompletableFuture<LedgerFiscalYearRolloverErrorCollection> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverErrorsService.retrieveLedgersRolloverErrors(any(), anyInt(), anyInt(), any()))
      .thenReturn(errorFuture);

    // When call getFinanceLedgerRollovers but ledgerRolloverService return error
    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER_ERRORS.getEndpoint(), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  static class ContextConfiguration {
    @Bean
    public LedgerRolloverErrorsService ledgerRolloverErrorsService() {
      return Mockito.mock(LedgerRolloverErrorsService.class);
    }
  }
}
