package org.folio.services.exchange.handler;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.jaxrs.model.ExchangeRate;
import org.folio.rest.jaxrs.model.ExchangeRateSource;

import java.math.BigDecimal;
import java.net.http.HttpClient;

public class ConveraCustomJsonHandler extends AbstractCustomJsonHandler {

  public ConveraCustomJsonHandler(HttpClient httpClient, ExchangeRateSource rateSource) {
    super(httpClient, rateSource);
  }

  @Override
  public Pair<BigDecimal, ExchangeRate.OperationMode> getExchangeRateFromApi(String from, String to) {
    throw new UnsupportedOperationException("Convera exchange rate provider is unimplemented");
  }
}
