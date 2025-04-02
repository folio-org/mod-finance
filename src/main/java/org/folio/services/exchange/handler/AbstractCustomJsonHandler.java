package org.folio.services.exchange.handler;

import org.folio.rest.jaxrs.model.ExchangeRateSource;

import java.math.BigDecimal;
import java.net.http.HttpClient;

public abstract class AbstractCustomJsonHandler {

  protected static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  protected static final String CONTENT_TYPE = "Content-Type";
  protected static final String APPLICATION_JSON_UTF_8 = "application/json; charset=utf-8";

  protected final ExchangeRateSource rateSource;

  protected AbstractCustomJsonHandler(ExchangeRateSource rateSource) {
    this.rateSource = rateSource;
  }

  public abstract BigDecimal getExchangeRateFromApi(String from, String to);
}
