package org.folio.services.exchange.handler;

import lombok.extern.log4j.Log4j2;
import org.folio.rest.jaxrs.model.ExchangeRateSource;

import java.math.BigDecimal;

@Log4j2
public class ConveraCustomJsonHandler extends AbstractCustomJsonHandler {

  public ConveraCustomJsonHandler(ExchangeRateSource rateSource) {
    super(rateSource);
  }

  @Override
  public BigDecimal getExchangeRateFromApi(String from, String to) {
    throw new UnsupportedOperationException("Convera exchange rate provider is unimplemented");
  }
}
