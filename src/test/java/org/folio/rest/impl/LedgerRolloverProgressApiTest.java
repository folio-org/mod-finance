package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.RestTestUtils.verifyGet;
import static org.folio.rest.util.RestTestUtils.verifyPostResponse;
import static org.folio.rest.util.RestTestUtils.verifyPut;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.folio.ApiTestSuite;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgress;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgressCollection;
import org.folio.rest.util.TestEntities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class LedgerRolloverProgressApiTest {

  @Autowired
  private LedgerRolloverProgressService mockLedgerRolloverProgressService;

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
    }
    initSpringContext(LedgerRolloverProgressApiTest.ContextConfiguration.class);
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterEach
  void resetMocks() {
    reset(mockLedgerRolloverProgressService);
  }

  @Test
  void shouldReturnLedgerRolloverProgressCollectionWhenCallGetAndRolloverProgressServiceReturnLedgerRolloverProgress() {
    LedgerFiscalYearRolloverProgressCollection ledgerProgresses = new LedgerFiscalYearRolloverProgressCollection()
      .withTotalRecords(1)
      .withLedgerFiscalYearRolloverProgresses(Arrays.asList(new LedgerFiscalYearRolloverProgress()));

    when(mockLedgerRolloverProgressService.retrieveLedgerRolloverProgresses(any(), anyInt(), anyInt(), any()))
      .thenReturn(CompletableFuture.completedFuture(ledgerProgresses));

    // When call getFinanceLedgerRollovers successfully
    LedgerFiscalYearRolloverProgressCollection rolloverProgressCollection = verifyGet(TestEntities.LEDGER_ROLLOVER_PROGRESS.getEndpoint(), APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverProgressCollection.class);

    // Then return LedgerFiscalYearRolloverCollection
    assertThat(rolloverProgressCollection.getLedgerFiscalYearRolloverProgresses(), hasSize(1));

  }

  @Test
  void shouldReturnErrorWhenCallGetAndRolloverProgressServiceReturnError() {

    CompletableFuture<LedgerFiscalYearRolloverProgressCollection> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverProgressService.retrieveLedgerRolloverProgresses(any(), anyInt(), anyInt(), any()))
      .thenReturn(errorFuture);

    // When call getFinanceLedgerRollovers but ledgerRolloverService return error
    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER_PROGRESS.getEndpoint(), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));

  }

  @Test
  void shouldReturnLedgerRolloverProgressWhenCallGetByIdAndRolloverProgressServiceReturnLedgerRolloverProgress() {

    String ledgerRolloverId = UUID.randomUUID().toString();

    when(mockLedgerRolloverProgressService.retrieveLedgerRolloverProgressById(anyString(), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverProgress().withId(ledgerRolloverId)));

    // When call getFinanceLedgerRolloversById successfully
    LedgerFiscalYearRolloverProgress progress = verifyGet(TestEntities.LEDGER_ROLLOVER_PROGRESS.getEndpointWithId(ledgerRolloverId),
      APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverProgress.class);

    // Then return LedgerFiscalYearRollover with that id
    assertThat(progress.getId(), is(ledgerRolloverId));
  }

  @Test
  void shouldReturnErrorWhenCallGetByIdAndRolloverProgressServiceReturnError() {

    String ledgerRolloverId = UUID.randomUUID().toString();

    CompletableFuture<LedgerFiscalYearRolloverProgress> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverProgressService.retrieveLedgerRolloverProgressById(anyString(), any()))
      .thenReturn(errorFuture);

    // When call getFinanceLedgerRolloversById but service return Error

    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER_PROGRESS.getEndpointWithId(ledgerRolloverId), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  @Test
  void shouldReturnLedgerRolloverProgressWhenCallPostAndRolloverProgressServiceReturnLedgerRolloverProgress() {

    // Given LedgerRollover
    String progressId = UUID.randomUUID().toString();
    LedgerFiscalYearRolloverProgress progress = new LedgerFiscalYearRolloverProgress()
      .withId(progressId)
      .withLedgerRolloverId(UUID.randomUUID().toString());

    when(mockLedgerRolloverProgressService.createLedgerRolloverProgress(any(LedgerFiscalYearRolloverProgress.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverProgress().withId(progressId)));

    // When call postFinanceLedgerRollover successfully
    LedgerFiscalYearRolloverProgress rolloverProgress = verifyPostResponse(TestEntities.LEDGER_ROLLOVER_PROGRESS.getEndpoint(), progress,
      APPLICATION_JSON, 201)
      .as(LedgerFiscalYearRolloverProgress.class);

    // Then return LedgerFiscalYearRollover with that id
    assertThat(rolloverProgress.getId(), is(progressId));
  }

  @Test
  void shouldReturnErrorWhenCallPostAndRolloverProgressServiceReturnError() {

    // Given LedgerRollover
    String progressId = UUID.randomUUID().toString();
    LedgerFiscalYearRolloverProgress progress = new LedgerFiscalYearRolloverProgress()
      .withId(progressId);

    when(mockLedgerRolloverProgressService.createLedgerRolloverProgress(any(LedgerFiscalYearRolloverProgress.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverProgress().withId(progressId)));

    // When call postFinanceLedgerRollover without required parameters
    Errors errors = verifyPostResponse(TestEntities.LEDGER_ROLLOVER_PROGRESS.getEndpoint(), progress,
      APPLICATION_JSON, 422)
      .as(Errors.class);

    // Then return LedgerFiscalYearRollover with that id
    assertThat(errors.getErrors(), hasSize(1));
  }

  @Test
  void shouldReturnSuccessCode204WhenCallPutAndRolloverProgressServiceReturnLedgerRolloverProgress() {

    // Given LedgerRollover
    String progressId = UUID.randomUUID().toString();
    LedgerFiscalYearRolloverProgress progress = new LedgerFiscalYearRolloverProgress()
      .withId(progressId)
      .withLedgerRolloverId(UUID.randomUUID().toString());

    when(mockLedgerRolloverProgressService.updateLedgerRolloverProgressById(anyString(), any(LedgerFiscalYearRolloverProgress.class), any()))
      .thenReturn(CompletableFuture.completedFuture(null));

    // When call postFinanceLedgerRollover successfully
    verifyPut(TestEntities.LEDGER_ROLLOVER_PROGRESS.getEndpointWithId(progressId), progress,
      "", 204);
  }

  static class ContextConfiguration {
    @Bean
    public LedgerRolloverProgressService ledgerRolloverProgressService() {
      return Mockito.mock(LedgerRolloverProgressService.class);
    }
  }
}
