package org.folio.rest.impl;

import org.folio.ApiTestSuite;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudget;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudgetCollection;
import org.folio.rest.util.TestEntities;
import org.folio.services.ledger.LedgerRolloverBudgetsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LedgerRolloverBudgetsApiTest {

  @Autowired
  private LedgerRolloverBudgetsService mockLedgerRolloverBudgetsService;

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
    }
    initSpringContext(LedgerRolloverBudgetsApiTest.ContextConfiguration.class);
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterEach
  void resetMocks() {
    reset(mockLedgerRolloverBudgetsService);
  }

  @Test
  void shouldReturnLedgerRolloverBudgetsCollectionWhenCallGetAndRolloverBudgetsServiceReturnLedgerRolloverBudgets() {
    LedgerFiscalYearRolloverBudgetCollection ledgerBudgets = new LedgerFiscalYearRolloverBudgetCollection()
      .withTotalRecords(1)
      .withLedgerFiscalYearRolloverBudgets(List.of(new LedgerFiscalYearRolloverBudget()));

    when(mockLedgerRolloverBudgetsService.retrieveLedgerRolloverBudgets(any(), anyInt(), anyInt(), any()))
      .thenReturn(CompletableFuture.completedFuture(ledgerBudgets));

    // When call getFinanceLedgerRolloversBudgets successfully
    LedgerFiscalYearRolloverBudgetCollection rolloverBudgetCollection = verifyGet(TestEntities.LEDGER_ROLLOVER_BUDGETS.getEndpoint(), APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverBudgetCollection.class);

    // Then return LedgerFiscalYearRolloverBudgetCollection
    assertThat(rolloverBudgetCollection.getLedgerFiscalYearRolloverBudgets(), hasSize(1));
  }

  @Test
  void shouldReturnErrorWhenCallGetAndRolloverBudgetsServiceReturnError() {

    CompletableFuture<LedgerFiscalYearRolloverBudgetCollection> budgetFuture = new CompletableFuture<>();
    budgetFuture.completeExceptionally(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverBudgetsService.retrieveLedgerRolloverBudgets(any(), anyInt(), anyInt(), any()))
      .thenReturn(budgetFuture);

    // When call getFinanceLedgerRolloversBudgets but ledgerRolloverBudgetsService return error
    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER_BUDGETS.getEndpoint(), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  @Test
  void shouldReturnLedgerRolloverBudgetsWhenCallGetByIdAndRolloverBudgetsServiceReturnLedgerRolloverBudgets() {

    String ledgerRolloverId = UUID.randomUUID().toString();

    when(mockLedgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(anyString(), any()))
      .thenReturn(CompletableFuture.completedFuture(new LedgerFiscalYearRolloverBudget().withLedgerRolloverId(ledgerRolloverId)));

    // When call getFinanceLedgerBudgetsById successfully
    LedgerFiscalYearRolloverBudget budgets = verifyGet(TestEntities.LEDGER_ROLLOVER_BUDGETS.getEndpointWithId(ledgerRolloverId),
      APPLICATION_JSON,
      OK.getStatusCode())
      .as(LedgerFiscalYearRolloverBudget.class);

    // Then return LedgerFiscalYearRollover with that id
    assertThat(budgets.getLedgerRolloverId(), is(ledgerRolloverId));
  }

  @Test
  void shouldReturnErrorWhenCallGetByIdAndRolloverBudgetsServiceReturnError() {

    String ledgerRolloverId = UUID.randomUUID().toString();

    CompletableFuture<LedgerFiscalYearRolloverBudget> errorFuture = new CompletableFuture<>();
    errorFuture.completeExceptionally(new HttpException(500, INTERNAL_SERVER_ERROR.getReasonPhrase()));

    when(mockLedgerRolloverBudgetsService.retrieveLedgerRolloverBudgetById(anyString(), any()))
      .thenReturn(errorFuture);

    // When call getFinanceLedgerBudgetsById but service return Error

    Errors errors = verifyGet(TestEntities.LEDGER_ROLLOVER_BUDGETS.getEndpointWithId(ledgerRolloverId), APPLICATION_JSON,
      INTERNAL_SERVER_ERROR.getStatusCode()).as(Errors.class);

    // Then return ERROR
    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors().get(0).getCode(), is(GENERIC_ERROR_CODE.getCode()));
  }

  static class ContextConfiguration {
    @Bean
    public LedgerRolloverBudgetsService ledgerRolloverService() {
      return Mockito.mock(LedgerRolloverBudgetsService.class);
    }
  }

}
