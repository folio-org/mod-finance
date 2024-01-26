package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.ApiTestSuite;
import org.folio.rest.util.RestTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CalculateExchangeApiTest {
  private static final Logger logger = LogManager.getLogger(ExchangeRateTest.class);

  private static final double ONE_HUNDRED = 100.0;
  private static final String CALCULATE_EXCHANGE_RATE_PATH = "finance/calculate_exchange";
  private static final String VALID_REQUEST = "?source_currency=USD&target_currency=EUR&amount=100.0";
  private static final String SAME_CURRENCIES = "?source_currency=USD&target_currency=USD&amount=100.0";
  private static final String NON_EXISTENT_CURRENCY = "?source_currency=ABC&target_currency=EUR&amount=100.0";
  private static final String MISSING_SOURCE_CURRENCY = "?target_currency=EUR&amount=100.0";
  private static final String MISSING_TARGET_CURRENCY = "?source_currency=USD&amount=100.0";
  private static final String MISSING_AMOUNT = "?source_currency=USD&target_currency=EUR";
  private static final String INVALID_CURRENCY = "?source_currency=US&target_currency=USD&amount=100.0";
  private static final String EXCHANGE_NOT_AVAILABLE = "?source_currency=USD&target_currency=ALL&amount=100.0";
  private static boolean runningOnOwn;
  @BeforeAll
  static void beforeAll() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
  }

  @AfterAll
  static void after() {
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
  }

  @Test
  void calculateExchange() {
    logger.info("=== Test get exchange rate: Success ===");
    var exchangeCalculation = RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH + VALID_REQUEST, APPLICATION_JSON, 200).as(Double.class);
    assertNotNull(exchangeCalculation);
  }

  @Test
  void calculateExchangeForSameCurrencies() {
    logger.info("=== Test exchange calculation for same currency codes: Success, Amount=100.0 ===");
    var exchangeCalculation = RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH + SAME_CURRENCIES, APPLICATION_JSON, 200).as(Double.class);
    assertThat(ONE_HUNDRED, equalTo(exchangeCalculation));
  }

  @Test
  void calculateExchangeForNonexistentCurrency(){
    logger.info("=== Test exchange calculation for non-existent currency code: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH + NON_EXISTENT_CURRENCY, "", 400);
  }

  @Test
  void calculateExchangeMissingParameters() {
    logger.info("=== Test exchange calculation missing query parameters: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH, "", 400);
  }

  @Test
  void calculateExchangeMissingSourceCurrencyParameter() {
    logger.info("=== Test exchange calculation missing SOURCE_CURRENCY parameter: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH + MISSING_SOURCE_CURRENCY, "", 400);
  }

  @Test
  void calculateExchangeMissingTargetCurrencyParameter() {
    logger.info("=== Test exchange calculation missing TARGET_CURRENCY parameter: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH + MISSING_TARGET_CURRENCY, "", 400);
  }

  @Test
  void calculateExchangeMissingAmountParameter() {
    logger.info("=== Test exchange calculation missing TARGET_CURRENCY parameter: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH + MISSING_AMOUNT, "", 400);
  }


  @Test
  void calculateExchangeInvalidCurrencyCode() {
    logger.info("=== Test exchange calculation for invalid currency code: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH + INVALID_CURRENCY, "", 400);
  }

  @Test
  void getExchangeRateNoRate() {
    logger.info("=== Test exchange calculation source currency USD target currency ALL : NOT_FOUND ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_RATE_PATH + EXCHANGE_NOT_AVAILABLE, "", 404);
  }
}
