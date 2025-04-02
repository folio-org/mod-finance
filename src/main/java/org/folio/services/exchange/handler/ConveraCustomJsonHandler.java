package org.folio.services.exchange.handler;

import org.folio.rest.jaxrs.model.ExchangeRateSource;

import java.math.BigDecimal;

public class ConveraCustomJsonHandler extends AbstractCustomJsonHandler {

  public ConveraCustomJsonHandler(ExchangeRateSource rateSource) {
    super(rateSource);
  }

  @Override
  public BigDecimal getExchangeRateFromApi(String from, String to) {
    throw new UnsupportedOperationException("Convera exchange rate provider is unimplemented");
  }
}
