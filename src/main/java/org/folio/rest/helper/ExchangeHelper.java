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
import org.folio.rest.jaxrs.model.ExchangeRate.OperationMode;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.util.ErrorCodes;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
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
    validateRequiredParameters(List.of("from", "to"), Arrays.asList(from, to));
    Number rateAsNumber = getRateFactor(from, to);
    double rateAsDouble = rateAsNumber.doubleValue();
    log.debug("getExchangeRate:: Fetched exchange rate={}", rateAsDouble);
    return new ExchangeRate().withFrom(from)
      .withTo(to)
      .withExchangeRate(rateAsDouble);
  }

  public Double calculateExchange(String from, String to, Number amount,
                                  OperationMode operationMode, Number customRate) {
    validateRequiredParameters(List.of("from", "to", "amount"), Arrays.asList(from, to, amount));
    Number rate = customRate == null ? getRateFactor(from, to) : customRate;
    log.debug("calculateExchange:: Calculating exchange exchangeRate, currency from={}, to={}, "
      + "amount={}, exchangeRate={}, operationMode={}", from, to, amount, rate, operationMode);
    BigDecimal bdAmount = new BigDecimal(amount.toString());
    BigDecimal bdRate = new BigDecimal(rate.toString());
    BigDecimal newAmount;
    if (operationMode == OperationMode.DIVIDE) {
      newAmount = bdAmount.divide(bdRate, 4, RoundingMode.HALF_EVEN);
    } else {
      newAmount = bdAmount.multiply(bdRate);
    }
    Double result = Money.of(newAmount, to)
      .with(Monetary.getDefaultRounding())
      .getNumber()
      .doubleValueExact();
    log.debug("calculateExchange:: result is {}", result);
    return result;
  }

  private Number getRateFactor(String from, String to) {
    try {
      return MonetaryConversions.getExchangeRateProvider(IDENTITY, ECB)
        .getExchangeRate(from, to)
        .getFactor();
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

  private void validateRequiredParameters(List<String> names, List<Object> values) {
    for (int i=0; i<names.size(); i++) {
      if (values.get(i) == null) {
        throw new HttpException(422, String.format("Missing required parameter: %s", names.get(i)));
      }
    }
  }
}
