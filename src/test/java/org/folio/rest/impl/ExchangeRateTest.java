package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.rest.acq.model.finance.ExchangeRate;
import org.folio.rest.util.RestTestUtils;
import org.junit.jupiter.api.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ExchangeRateTest {
  private static final Logger logger = LoggerFactory.getLogger(ExchangeRateTest.class);

  private static final double ONE = 1.0;
  private static final String EXCHANGE_RATE_PATH = "finance/exchange-rate";
  private static final String VALID_REQUEST = "?from=USD&to=EUR";
  private static final String SAME_CURRENCIES = "?from=USD&to=USD";
  private static final String NON_EXISTENT_CURRENCY = "?from=ABC&to=USD";
  private static final String MISSING_FROM = "?to=USD";
  private static final String MISSING_TO = "?from=USD";
  private static final String INVALID_CURRENCY = "?from=US&to=USD";
  private static final String RATE_NOT_AVAILABLE = "?from=USD&to=ALL";

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
}
