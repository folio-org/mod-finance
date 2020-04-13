package org.folio.rest.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.acq.model.finance.ExchangeRate;
import org.junit.jupiter.api.Test;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExchangeRateTest extends ApiTestBase {
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
  public void getExchangeRate() {
    logger.info("=== Test get exchange rate: Success ===");
    ExchangeRate exchangeRate = verifyGet(EXCHANGE_RATE_PATH + VALID_REQUEST, APPLICATION_JSON, 200).as(ExchangeRate.class);
    assertThat(exchangeRate.getFrom(), equalTo("USD"));
    assertThat(exchangeRate.getTo(), equalTo("EUR"));
    assertNotNull(exchangeRate.getExchangeRate());
  }

  @Test
  public void exchangeRateForSameCurrenciesIsOne() {
    logger.info("=== Test get exchange rate for same currency codes: Success, exchangeRate=1 ===");
    ExchangeRate exchangeRate = verifyGet(EXCHANGE_RATE_PATH + SAME_CURRENCIES, APPLICATION_JSON, 200).as(ExchangeRate.class);
    assertThat(exchangeRate.getFrom(), equalTo(exchangeRate.getTo()));
    assertThat(ONE, equalTo(exchangeRate.getExchangeRate()));
  }

  @Test
  public void getExchangeRateForNonexistentCurrency(){
    logger.info("=== Test get exchange rate for non-existent currency code: BAD_REQUEST ===");
    verifyGet(EXCHANGE_RATE_PATH + NON_EXISTENT_CURRENCY, "", 400);
  }

  @Test
  public void getExchangeRateMissingParameters() {
    logger.info("=== Test get exchange rate missing query parameters: BAD_REQUEST ===");
    verifyGet(EXCHANGE_RATE_PATH, "", 400);
  }

  @Test
  public void getExchangeRateMissingFromParameter() {
    logger.info("=== Test get exchange rate missing FROM parameter: BAD_REQUEST ===");
    verifyGet(EXCHANGE_RATE_PATH + MISSING_FROM, "", 400);
  }

  @Test
  public void getExchangeRateMissingToParameter() {
    logger.info("=== Test get exchange rate missing TO parameter: BAD_REQUEST ===");
    verifyGet(EXCHANGE_RATE_PATH + MISSING_TO, "", 400);
  }

  @Test
  public void getExchangeRateInvalidCurrencyCode() {
    logger.info("=== Test get exchange rate for invalid currency code: BAD_REQUEST ===");
    verifyGet(EXCHANGE_RATE_PATH + INVALID_CURRENCY, "", 400);
  }

  @Test
  public void getExchangeRateNoRate() {
    logger.info("=== Test get exchange rate from USD to ALL: NOT_FOUND ===");
    verifyGet(EXCHANGE_RATE_PATH + RATE_NOT_AVAILABLE, "", 404);
  }
}
