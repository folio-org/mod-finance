package org.folio.services.exchange.handler;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.jaxrs.model.ExchangeRate;
import org.folio.rest.jaxrs.model.ExchangeRateSource;

import java.math.BigDecimal;
import java.net.http.HttpClient;

public abstract class AbstractCustomJsonHandler {

  protected static final String APPLICATION_JSON_UTF_8 = "application/json; charset=utf-8";

  protected final HttpClient httpClient;
  protected final ExchangeRateSource rateSource;

  protected AbstractCustomJsonHandler(HttpClient httpClient, ExchangeRateSource rateSource) {
    this.httpClient = httpClient;
    this.rateSource = rateSource;
  }

  public abstract Pair<BigDecimal, ExchangeRate.OperationMode> getExchangeRateFromApi(String from, String to);
}
