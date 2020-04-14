package org.folio.rest.helper;

import io.vertx.core.Context;
import org.folio.rest.acq.model.finance.ExchangeRate;
import org.folio.rest.exception.HttpException;

import javax.money.convert.CurrencyConversionException;
import javax.money.convert.MonetaryConversions;

public class ExchangeRateHelper extends AbstractHelper {
  public ExchangeRateHelper(Context ctx) {
    super(ctx);
  }

  public ExchangeRate getExchangeRate(String from, String to) {
    try {
      double exchangeRate = MonetaryConversions.getExchangeRateProvider().getExchangeRate(from, to).getFactor().doubleValue();
      return new ExchangeRate().withFrom(from).withTo(to).withExchangeRate(exchangeRate);
    } catch (CurrencyConversionException e) {
      throw new HttpException(404, e.getMessage());
    } catch (Exception e) {
      throw new HttpException(400, e.getMessage());
    }
  }
}
