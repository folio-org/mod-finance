package org.folio.rest.impl;

import org.folio.ApiTestSuite;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLog;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLogCollection;
import org.folio.rest.util.TestEntities;
import org.folio.services.ledger.LedgerRolloverLogsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.UUID;
import io.vertx.core.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.RestTestUtils.verifyGet;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LedgerRolloverLogsApiTest {

  @Autowired
  private LedgerRolloverLogsService mockLedgerRolloverLogsService;

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
    }
    initSpringContext(LedgerRolloverLogsApiTest.ContextConfiguration.class);
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterEach
  void resetMocks() {
    reset(mockLedgerRolloverLogsService);
  }

  @Test
  void shouldReturnLedgerRolloverLogsCollectionWhenCallGetAndRolloverLogsServiceReturnLedgerRolloverLogs() {
    LedgerFiscalYearRolloverLogCollection ledgerLogs = new LedgerFiscalYearRolloverLogCollection()
      .withTotalRecords(1)
      .withLedgerFiscalYearRolloverLogs(List.of(new LedgerFiscalYearRolloverLog()));

    when(mockLedgerRolloverLogsService.retrieveLedgerRolloverLogs(any(), anyInt(), anyInt(), any()))
      .thenReturn(succeededFuture(ledgerLogs));

    // When call getFinanceLedgerRolloversLogs successfully
    LedgerFiscalYearRolloverLogCollection rolloverLogCollection = verifyGet(TestEntities.LEDGER_ROLLOVER_LOGS.getEndpoint(), APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverLogCollection.class);

    // Then return LedgerFiscalYearRolloverLogCollection
    assertThat(rolloverLogCollection.getLedgerFiscalYearRolloverLogs(), hasSize(1));
  }

  @Test
  void shouldReturnErrorWhenCallGetAndRolloverLogsServiceReturnError() {

    Future<LedgerFiscalYearRolloverLogCollection> logFuture = Future.failedFuture(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverLogsService.retrieveLedgerRolloverLogs(any(), anyInt(), anyInt(), any()))
      .thenReturn(logFuture);

    // When call getFinanceLedgerRolloversLogs but ledgerRolloverLogsService return error
    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER_LOGS.getEndpoint(), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  @Test
  void shouldReturnLedgerRolloverLogsWhenCallGetByIdAndRolloverLogsServiceReturnLedgerRolloverLogs() {

    String ledgerRolloverId = UUID.randomUUID().toString();

    when(mockLedgerRolloverLogsService.retrieveLedgerRolloverLogById(anyString(), any()))
      .thenReturn(succeededFuture(new LedgerFiscalYearRolloverLog().withLedgerRolloverId(ledgerRolloverId)));

    // When call getFinanceLedgerLogsById successfully
    LedgerFiscalYearRolloverLog logs = verifyGet(TestEntities.LEDGER_ROLLOVER_LOGS.getEndpointWithId(ledgerRolloverId),
      APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverLog.class);

    // Then return LedgerFiscalYearRollover with that id
    assertThat(logs.getLedgerRolloverId(), is(ledgerRolloverId));
  }

  @Test
  void shouldReturnErrorWhenCallGetByIdAndRolloverLogsServiceReturnError() {

    String ledgerRolloverId = UUID.randomUUID().toString();

    Future<LedgerFiscalYearRolloverLog> errorFuture = Future.failedFuture(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverLogsService.retrieveLedgerRolloverLogById(anyString(), any()))
      .thenReturn(errorFuture);

    // When call getFinanceLedgerLogsById but service return Error

    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER_LOGS.getEndpointWithId(ledgerRolloverId), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  static class ContextConfiguration {
    @Bean
    public LedgerRolloverLogsService ledgerRolloverService() {
      return Mockito.mock(LedgerRolloverLogsService.class);
    }
  }

}
