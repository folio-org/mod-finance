package org.folio.rest.helper;

import static org.javamoney.moneta.convert.ExchangeRateType.ECB;
import static org.javamoney.moneta.convert.ExchangeRateType.IDENTITY;

import javax.money.Monetary;
import javax.money.convert.CurrencyConversionException;
import javax.money.convert.MonetaryConversions;

import io.vertx.core.Context;
import lombok.extern.log4j.Log4j2;
import org.folio.HttpStatus;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.ExchangeRate;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.util.ErrorCodes;
import org.javamoney.moneta.Money;

import java.util.List;

@Log4j2
public class ExchangeHelper extends AbstractHelper {

  private static final String FROM = "from";
  private static final String TO = "to";

  public ExchangeHelper(Context ctx) {
    super(ctx);
  }

  public ExchangeRate getExchangeRate(String from, String to) {
    log.debug("getExchangeRate:: Getting exchange rate from={}, to={}", from, to);
    try {
      var exchangeRate = MonetaryConversions.getExchangeRateProvider(IDENTITY, ECB)
        .getExchangeRate(from, to)
        .getFactor()
        .doubleValue();
      log.debug("getExchangeRate:: Fetched exchange rate={}", exchangeRate);
      return new ExchangeRate().withFrom(from)
        .withTo(to)
        .withExchangeRate(exchangeRate);
    } catch (CurrencyConversionException e) {
      var errors = List.of(ErrorCodes.CANNOT_CONVERT_AMOUNT_INVALID_CURRENCY.toError()
        .withParameters(List.of(
          new Parameter().withKey(FROM).withValue(from),
          new Parameter().withKey(TO).withValue(to))));
      throw new HttpException(HttpStatus.HTTP_NOT_FOUND.toInt(),
        new Errors().withErrors(errors).withTotalRecords(errors.size()));
    } catch (Exception e) {
      log.error("Error while retrieving exchange rate", e);
      throw new HttpException(HttpStatus.HTTP_BAD_REQUEST.toInt(), e.getMessage());
    }
  }

  public Double calculateExchange(String from, String to, Number amount, Number customRate) {
    log.debug("calculateExchange:: Calculating exchange sourceCurrency from={}, to={}, amount={} and customRate={}", from, to, amount, customRate);
    var initialAmount = Money.of(amount, from);
    var rate = customRate == null ? getExchangeRate(from, to).getExchangeRate() : customRate;
    log.debug("calculateExchange:: rate is {}", rate);
    return initialAmount.multiply(rate)
      .with(Monetary.getDefaultRounding())
      .getNumber()
      .doubleValueExact();
  }
}
