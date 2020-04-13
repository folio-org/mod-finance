package org.folio.rest.helper;

import io.vertx.core.Context;
import org.folio.rest.acq.model.finance.ExchangeRate;
import org.folio.rest.exception.HttpException;

import javax.money.convert.CurrencyConversionException;
import javax.money.convert.MonetaryConversions;
import java.util.concurrent.CompletableFuture;

public class ExchangeRateHelper extends AbstractHelper {
  public ExchangeRateHelper(Context ctx) {
    super(ctx);
  }

  public CompletableFuture<ExchangeRate> getExchangeRate(String from, String to) {
    CompletableFuture<ExchangeRate> future = new CompletableFuture<>();
    try {
      double exchangeRate = MonetaryConversions.getExchangeRateProvider().getExchangeRate(from, to).getFactor().doubleValue();
      future.complete(new ExchangeRate().withFrom(from).withTo(to).withExchangeRate(exchangeRate));
    } catch (CurrencyConversionException e) {
      future.completeExceptionally(new HttpException(404, e.getMessage()));
    } catch (Exception e) {
      future.completeExceptionally(new HttpException(400, e.getMessage()));
    }
    return future;
  }
}
