package org.folio.rest.helper;

import static org.javamoney.moneta.convert.ExchangeRateType.ECB;
import static org.javamoney.moneta.convert.ExchangeRateType.IDENTITY;

import javax.money.Monetary;
import javax.money.convert.CurrencyConversionException;
import javax.money.convert.MonetaryConversions;

import io.vertx.core.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.ExchangeRate;
import org.javamoney.moneta.Money;

public class ExchangeHelper extends AbstractHelper {

  private static final Logger log = LogManager.getLogger();

  public ExchangeHelper(Context ctx) {
    super(ctx);
  }

  public ExchangeRate getExchangeRate(String from, String to) {
    log.debug("getExchangeRate:: Getting exchange rate from={}, to={}", from, to);
    try {
      double exchangeRate = MonetaryConversions.getExchangeRateProvider(IDENTITY, ECB)
        .getExchangeRate(from, to)
        .getFactor()
        .doubleValue();
      log.info("getExchangeRate:: Fetched exchange rate={}", exchangeRate);
      return new ExchangeRate().withFrom(from)
        .withTo(to)
        .withExchangeRate(exchangeRate);
    } catch (CurrencyConversionException e) {
      log.error("Failed to converse currency", e);
      throw new HttpException(404, e.getMessage());
    } catch (Exception e) {
      log.error("Error while retrieving exchange rate", e);
      throw new HttpException(400, e.getMessage());
    }
  }

  public Double calculateExchange(String from, String to, Number amount, Number customRate) {
    log.debug("calculateExchange:: Calculating exchange sourceCurrency from={}, to={}, amount={} and customRate={}", from, to, amount, customRate);
    var initialAmount = Money.of(amount, from);
    var rate = customRate == null ? getExchangeRate(from, to).getExchangeRate() : customRate;
    log.info("calculateExchange:: rate is {}", rate);

    return initialAmount.multiply(rate)
      .with(Monetary.getDefaultRounding())
      .getNumber().doubleValue();
  }
}
