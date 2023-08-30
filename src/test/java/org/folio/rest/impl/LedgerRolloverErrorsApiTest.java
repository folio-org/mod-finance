package org.folio.rest.impl;

import org.folio.ApiTestSuite;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverError;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverErrorCollection;
import org.folio.services.ledger.LedgerRolloverErrorsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.UUID;
import io.vertx.core.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.RestTestUtils.*;
import static org.folio.rest.util.TestConfig.*;
import static org.folio.rest.util.TestEntities.LEDGER_ROLLOVER_ERRORS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
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
      .withLedgerFiscalYearRolloverErrors(List.of(new LedgerFiscalYearRolloverError()));

    when(mockLedgerRolloverErrorsService.getLedgerRolloverErrors(any(), anyInt(), anyInt(), anyString(), any()))
      .thenReturn(succeededFuture(ledgerErrors));

    // When call getFinanceLedgerRollovers successfully
    LedgerFiscalYearRolloverErrorCollection rolloverErrorCollection = verifyGet(LEDGER_ROLLOVER_ERRORS.getEndpoint(), APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverErrorCollection.class);

    // Then return LedgerFiscalYearRolloverCollection
    assertThat(rolloverErrorCollection.getLedgerFiscalYearRolloverErrors(), hasSize(1));
  }

  @Test
  void shouldReturnErrorWhenCallGetAndRolloverErrorsServiceReturnError() {

    Future<LedgerFiscalYearRolloverErrorCollection> errorFuture = new Future<>();
    errorFuture.completeExceptionally(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverErrorsService.getLedgerRolloverErrors(any(), anyInt(), anyInt(), anyString(), any()))
      .thenReturn(errorFuture);

    // When call getFinanceLedgerRollovers but ledgerRolloverService return error
    Errors errors = verifyGet(LEDGER_ROLLOVER_ERRORS.getEndpoint(), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  @Test
  void shouldCallCreateLedgerRolloverErrorOnPostApi() {
    LedgerFiscalYearRolloverError rolloverError = new LedgerFiscalYearRolloverError()
      .withLedgerRolloverId(UUID.randomUUID().toString())
      .withErrorType(LedgerFiscalYearRolloverError.ErrorType.OTHER)
      .withErrorMessage("test")
      .withFailedAction("action");

    when(mockLedgerRolloverErrorsService.createLedgerRolloverError(any(LedgerFiscalYearRolloverError.class),
        any(RequestContext.class)))
      .thenAnswer(invocation -> succeededFuture(invocation.getArgument(0)));

    LedgerFiscalYearRolloverError responseRolloverError = verifyPostResponse(LEDGER_ROLLOVER_ERRORS.getEndpoint(),
      rolloverError, APPLICATION_JSON, 201).as(LedgerFiscalYearRolloverError.class);

    ArgumentCaptor<LedgerFiscalYearRolloverError> argumentCaptor = ArgumentCaptor.forClass(
      LedgerFiscalYearRolloverError.class);
    verify(mockLedgerRolloverErrorsService).createLedgerRolloverError(argumentCaptor.capture(),
      any(RequestContext.class));
    LedgerFiscalYearRolloverError rolloverErrorFromArgument = argumentCaptor.getValue();

    assertEquals(rolloverErrorFromArgument, responseRolloverError);
    assertThat(responseRolloverError, hasProperty("metadata"));
  }

  @Test
  void shouldCallDeleteLedgerRolloverErrorMethodOnDeleteApi() {
    String rolloverErrorId = UUID.randomUUID().toString();

    when(mockLedgerRolloverErrorsService.deleteLedgerRolloverError(anyString(), any()))
      .thenReturn(succeededFuture(null));

    verifyDeleteResponse(LEDGER_ROLLOVER_ERRORS.getEndpointWithId(rolloverErrorId), "", 204);
    verify(mockLedgerRolloverErrorsService).deleteLedgerRolloverError(eq(rolloverErrorId), any(RequestContext.class));
  }

  static class ContextConfiguration {
    @Bean
    public LedgerRolloverErrorsService ledgerRolloverErrorsService() {
      return Mockito.mock(LedgerRolloverErrorsService.class);
    }
  }
}
