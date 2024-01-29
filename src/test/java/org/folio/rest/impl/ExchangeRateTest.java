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
import org.folio.rest.acq.model.finance.ExchangeRate;
import org.folio.rest.util.RestTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ExchangeRateTest {

  private static final Logger logger = LogManager.getLogger(ExchangeRateTest.class);
  private static final double ONE = 1.0;
  private static final double ONE_HUNDRED = 100.0;
  private static final String EXCHANGE_RATE_PATH = "finance/exchange-rate";
  private static final String VALID_REQUEST = "?from=USD&to=EUR";
  private static final String SAME_CURRENCIES = "?from=USD&to=USD";
  private static final String NON_EXISTENT_CURRENCY = "?from=ABC&to=USD";
  private static final String MISSING_FROM = "?to=USD";
  private static final String MISSING_TO = "?from=USD";
  private static final String INVALID_CURRENCY = "?from=US&to=USD";
  private static final String RATE_NOT_AVAILABLE = "?from=USD&to=ALL";
  private static final String CALCULATE_EXCHANGE_PATH = "finance/calculate_exchange";
  private static final String VALID_REQUEST_FOR_CALCULATE_EXCHANGE = "?from=USD&to=EUR&amount=100.0";
  private static final String SAME_CURRENCIES_FOR_CALCULATE_EXCHANGE = "?from=USD&to=USD&amount=100.0";
  private static final String NON_EXISTENT_CURRENCY_FOR_CALCULATE_EXCHANGE = "?from=ABC&to=EUR&amount=100.0";
  private static final String MISSING_FROM_FOR_CALCULATE_EXCHANGE = "?to=EUR&amount=100.0";
  private static final String MISSING_TO_FOR_CALCULATE_EXCHANGE = "?from=USD&amount=100.0";
  private static final String MISSING_AMOUNT = "?from=USD&to=EUR";
  private static final String INVALID_CURRENCY_FOR_CALCULATE_EXCHANGE = "?from=US&to=USD&amount=100.0";
  private static final String EXCHANGE_NOT_AVAILABLE = "?from=USD&to=ALL&amount=100.0";
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
  void getExchangeRate() {
    logger.info("=== Test get exchange rate: Success ===");
    ExchangeRate exchangeRate = RestTestUtils.verifyGet(EXCHANGE_RATE_PATH + VALID_REQUEST, APPLICATION_JSON, 200).as(ExchangeRate.class);
    assertThat(exchangeRate.getFrom(), equalTo("USD"));
    assertThat(exchangeRate.getTo(), equalTo("EUR"));
    assertNotNull(exchangeRate.getExchangeRate());
  }

  @Test
  void exchangeRateForSameCurrenciesIsOne() {
    logger.info("=== Test get exchange rate for same currency codes: Success, exchangeRate=1 ===");
    ExchangeRate exchangeRate = RestTestUtils.verifyGet(EXCHANGE_RATE_PATH + SAME_CURRENCIES, APPLICATION_JSON, 200).as(ExchangeRate.class);
    assertThat(exchangeRate.getFrom(), equalTo(exchangeRate.getTo()));
    assertThat(ONE, equalTo(exchangeRate.getExchangeRate()));
  }

  @Test
  void getExchangeRateForNonexistentCurrency(){
    logger.info("=== Test get exchange rate for non-existent currency code: BAD_REQUEST ===");
    RestTestUtils.verifyGet(EXCHANGE_RATE_PATH + NON_EXISTENT_CURRENCY, "", 400);
  }

  @Test
  void getExchangeRateMissingParameters() {
    logger.info("=== Test get exchange rate missing query parameters: BAD_REQUEST ===");
    RestTestUtils.verifyGet(EXCHANGE_RATE_PATH, "", 400);
  }

  @Test
  void getExchangeRateMissingFromParameter() {
    logger.info("=== Test get exchange rate missing FROM parameter: BAD_REQUEST ===");
    RestTestUtils.verifyGet(EXCHANGE_RATE_PATH + MISSING_FROM, "", 400);
  }

  @Test
  void getExchangeRateMissingToParameter() {
    logger.info("=== Test get exchange rate missing TO parameter: BAD_REQUEST ===");
    RestTestUtils.verifyGet(EXCHANGE_RATE_PATH + MISSING_TO, "", 400);
  }

  @Test
  void getExchangeRateInvalidCurrencyCode() {
    logger.info("=== Test get exchange rate for invalid currency code: BAD_REQUEST ===");
    RestTestUtils.verifyGet(EXCHANGE_RATE_PATH + INVALID_CURRENCY, "", 400);
  }

  @Test
  void getExchangeRateNoRate() {
    logger.info("=== Test get exchange rate from USD to ALL : NOT_FOUND ===");
    RestTestUtils.verifyGet(EXCHANGE_RATE_PATH + RATE_NOT_AVAILABLE, "", 404);
  }

  @Test
  void calculateExchange() {
    logger.info("=== Test get exchange calculation: Success ===");
    var exchangeCalculation = RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH + VALID_REQUEST_FOR_CALCULATE_EXCHANGE, APPLICATION_JSON, 200).as(Double.class);
    assertNotNull(exchangeCalculation);
  }

  @Test
  void calculateExchangeForSameCurrencies() {
    logger.info("=== Test exchange calculation for same currency codes: Success, Amount=100.0 ===");
    var exchangeCalculation = RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH + SAME_CURRENCIES_FOR_CALCULATE_EXCHANGE, APPLICATION_JSON, 200).as(Double.class);
    assertThat(ONE_HUNDRED, equalTo(exchangeCalculation));
  }

  @Test
  void calculateExchangeForNonexistentCurrency(){
    logger.info("=== Test exchange calculation for non-existent currency code: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH + NON_EXISTENT_CURRENCY_FOR_CALCULATE_EXCHANGE, "", 500);
  }

  @Test
  void calculateExchangeMissingParameters() {
    logger.info("=== Test exchange calculation missing query parameters: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH, "", 500);
  }

  @Test
  void calculateExchangeMissingSourceCurrencyParameter() {
    logger.info("=== Test exchange calculation missing FROM parameter: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH + MISSING_FROM_FOR_CALCULATE_EXCHANGE, "", 500);
  }

  @Test
  void calculateExchangeMissingTargetCurrencyParameter() {
    logger.info("=== Test exchange calculation missing TO parameter: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH + MISSING_TO_FOR_CALCULATE_EXCHANGE, "", 400);
  }

  @Test
  void calculateExchangeMissingAmountParameter() {
    logger.info("=== Test exchange calculation missing AMOUNT parameter: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH + MISSING_AMOUNT, "", 500);
  }

  @Test
  void calculateExchangeInvalidCurrencyCode() {
    logger.info("=== Test exchange calculation for invalid currency code: BAD_REQUEST ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH + INVALID_CURRENCY_FOR_CALCULATE_EXCHANGE, "", 500);
  }

  @Test
  void getExchangeNoCalculation() {
    logger.info("=== Test exchange calculation FROM currency USD, TO currency ALL : NOT_FOUND ===");
    RestTestUtils.verifyGet(CALCULATE_EXCHANGE_PATH + EXCHANGE_NOT_AVAILABLE, "", 404);
  }
}
