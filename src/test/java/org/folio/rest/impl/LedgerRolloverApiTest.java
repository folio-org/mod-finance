package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.folio.rest.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.rest.util.RestTestUtils.verifyGet;
import static org.folio.rest.util.RestTestUtils.verifyPostResponse;
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
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverCollection;
import org.folio.rest.util.TestEntities;
import org.folio.services.LedgerRolloverService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class LedgerRolloverApiTest {

  @Autowired
  private LedgerRolloverService mockLedgerRolloverService;

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
    }
    initSpringContext(LedgerRolloverApiTest.ContextConfiguration.class);
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterEach
  void resetMocks() {
    reset(mockLedgerRolloverService);
  }

  @Test
  void getFinanceLedgerRolloversSuccessfully() {
    LedgerFiscalYearRolloverCollection ledgerRollovers = new LedgerFiscalYearRolloverCollection()
      .withTotalRecords(1)
      .withLedgerFiscalYearRollovers(Arrays.asList(new LedgerFiscalYearRollover()));

    when(mockLedgerRolloverService.retrieveLedgerRollovers(any(), anyInt(), anyInt(), any()))
      .thenReturn(CompletableFuture.completedFuture(ledgerRollovers));

    // When call getFinanceLedgerRollovers successfully
    LedgerFiscalYearRolloverCollection rolloverCollection = verifyGet(TestEntities.LEDGER_ROLLOVER.getEndpoint(), APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverCollection.class);

    // Then return LedgerFiscalYearRolloverCollection
    assertThat(rolloverCollection.getLedgerFiscalYearRollovers(), hasSize(1));

  }

  @Test
  void getFinanceLedgerRolloversWhenServiceReturnError() {

    CompletableFuture<LedgerFiscalYearRolloverCollection> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverService.retrieveLedgerRollovers(any(), anyInt(), anyInt(), any()))
      .thenReturn(errorFuture);

    // When call getFinanceLedgerRollovers but ledgerRolloverService return error
    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER.getEndpoint(), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));

  }

  @Test
  void getFinanceLedgerRolloversByIdSuccessfully() {

    String ledgerRolloverId = UUID.randomUUID().toString();

    when(mockLedgerRolloverService.retrieveLedgerRolloverById(anyString(), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRollover().withId(ledgerRolloverId)));

    // When call getFinanceLedgerRolloversById successfully
    LedgerFiscalYearRollover ledgerRollover = verifyGet(TestEntities.LEDGER_ROLLOVER.getEndpointWithId(ledgerRolloverId),
      APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRollover.class);

    // Then return LedgerFiscalYearRollover with that id
    assertThat(ledgerRollover.getId(), is(ledgerRolloverId));
  }

  @Test
  void getFinanceLedgerRolloversByIdWithError() {

    String ledgerRolloverId = UUID.randomUUID().toString();

    CompletableFuture<LedgerFiscalYearRollover> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverService.retrieveLedgerRolloverById(anyString(), any()))
      .thenReturn(errorFuture);

    // When call getFinanceLedgerRolloversById but service return Error

    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER.getEndpointWithId(ledgerRolloverId), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  @Test
  void postFinanceLedgerRolloversSuccessfully() {

    // Given LedgerRollover
    String ledgerRolloverId = UUID.randomUUID().toString();
    LedgerFiscalYearRollover ledgerFiscalYearRollover = new LedgerFiscalYearRollover()
      .withId(ledgerRolloverId)
      .withLedgerId(UUID.randomUUID().toString())
      .withToFiscalYearId(UUID.randomUUID().toString())
      .withFromFiscalYearId(UUID.randomUUID().toString());

    when(mockLedgerRolloverService.createLedger(any(LedgerFiscalYearRollover.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRollover().withId(ledgerRolloverId)));

    // When call postFinanceLedgerRollover successfully
    LedgerFiscalYearRollover ledgerRollover = verifyPostResponse(TestEntities.LEDGER_ROLLOVER.getEndpoint(), ledgerFiscalYearRollover,
      APPLICATION_JSON, 201)
      .as(LedgerFiscalYearRollover.class);

    // Then return LedgerFiscalYearRollover with that id
    assertThat(ledgerRollover.getId(), is(ledgerRolloverId));
  }

  @Test
  void postFinanceLedgerRolloversWithoutRequiredFields() {

    // Given LedgerRollover
    String ledgerRolloverId = UUID.randomUUID().toString();
    LedgerFiscalYearRollover ledgerFiscalYearRollover = new LedgerFiscalYearRollover()
      .withId(ledgerRolloverId);

    when(mockLedgerRolloverService.createLedger(any(LedgerFiscalYearRollover.class), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRollover().withId(ledgerRolloverId)));

    // When call postFinanceLedgerRollover without required parameters
    Errors errors = verifyPostResponse(TestEntities.LEDGER_ROLLOVER.getEndpoint(), ledgerFiscalYearRollover,
      APPLICATION_JSON, 422)
      .as(Errors.class);

    // Then return LedgerFiscalYearRollover with that id
    assertThat(errors.getErrors(), hasSize(3));
  }

  static class ContextConfiguration {
    @Bean
    public LedgerRolloverService ledgerRolloverService() {
      return Mockito.mock(LedgerRolloverService.class);
    }
  }
}
