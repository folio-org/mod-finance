package org.folio.rest.helper;

import static org.javamoney.moneta.convert.ExchangeRateType.ECB;
import static org.javamoney.moneta.convert.ExchangeRateType.IDENTITY;

import javax.money.convert.CurrencyConversionException;
import javax.money.convert.MonetaryConversions;

import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.ExchangeRate;

import io.vertx.core.Context;

public class ExchangeRateHelper extends AbstractHelper {
  public ExchangeRateHelper(Context ctx) {
    super(ctx);
  }

  public ExchangeRate getExchangeRate(String from, String to) {
    try {
      double exchangeRate = MonetaryConversions.getExchangeRateProvider(IDENTITY, ECB)
        .getExchangeRate(from, to)
        .getFactor()
        .doubleValue();

      return new ExchangeRate().withFrom(from)
        .withTo(to)
        .withExchangeRate(exchangeRate);
    } catch (CurrencyConversionException e) {
      throw new HttpException(404, e.getMessage());
    } catch (Exception e) {
      throw new HttpException(400, e.getMessage());
    }
  }

  public Double calculateExchange(Double rate, Double amount) {
    return amount * rate;
  }
}
