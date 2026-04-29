package org.folio.rest.helper;

import static org.folio.HttpStatus.HTTP_UNPROCESSABLE_ENTITY;
import static org.javamoney.moneta.convert.ExchangeRateType.ECB;
import static org.javamoney.moneta.convert.ExchangeRateType.IDENTITY;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
    validateRequiredParameters(List.of("from", "to"), Arrays.asList(from, to));
    try {
      double exchangeRate = MonetaryConversions.getExchangeRateProvider(IDENTITY, ECB)
        .getExchangeRate(from, to)
        .getFactor()
        .doubleValue();
      log.debug("getExchangeRate:: Fetched exchange rate={}", exchangeRate);
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
    validateRequiredParameters(List.of("from", "to", "amount"), Arrays.asList(from, to, amount));
    Number rate = customRate == null ? getExchangeRate(from, to).getExchangeRate() : customRate;
    log.debug("calculateExchange:: Calculating exchange exchangeRate, currency from={}, to={}, "
      + "amount={}, exchangeRate={}", from, to, amount, rate);
    BigDecimal bdAmount = new BigDecimal(amount.toString());
    BigDecimal bdRate = new BigDecimal(rate.toString());
    BigDecimal newAmount = bdAmount.multiply(bdRate);;
    Double result = Money.of(newAmount, to)
      .with(Monetary.getDefaultRounding())
      .getNumber()
      .doubleValueExact();
    log.debug("calculateExchange:: result is {}", result);
    return result;
  }

  private void validateRequiredParameters(List<String> names, List<Object> values) {
    for (int i = 0; i < names.size(); i++) {
      if (values.get(i) == null) {
        throw new HttpException(HTTP_UNPROCESSABLE_ENTITY.toInt(), String.format("Missing required parameter: %s", names.get(i)));
      }
    }
  }
}
