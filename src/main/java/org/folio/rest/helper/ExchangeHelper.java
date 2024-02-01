package org.folio.rest.helper;

import static org.javamoney.moneta.convert.ExchangeRateType.ECB;
import static org.javamoney.moneta.convert.ExchangeRateType.IDENTITY;

import javax.money.Monetary;
import javax.money.convert.CurrencyConversionException;
import javax.money.convert.MonetaryConversions;

import io.vertx.core.Context;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.ExchangeRate;
import org.javamoney.moneta.Money;

public class ExchangeHelper extends AbstractHelper {

  public ExchangeHelper(Context ctx) {
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

  public Double calculateExchange(String sourceCurrency, String targetCurrency, Number amount, Number customRate) {
    var initialAmount = Money.of(amount, sourceCurrency);
    var rate = customRate == null ? getExchangeRate(sourceCurrency, targetCurrency).getExchangeRate() : customRate;

    return initialAmount.multiply(rate)
      .with(Monetary.getDefaultRounding())
      .getNumber().doubleValue();
  }
}
