package org.folio.rest.helper;

import io.vertx.core.Context;
import org.folio.rest.acq.model.finance.ExchangeRate;

import javax.money.convert.MonetaryConversions;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ExchangeRateHelper extends AbstractHelper {
  public ExchangeRateHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<ExchangeRate> getExchangeRate(String from, String to) {
    CompletableFuture<ExchangeRate> future = new CompletableFuture<>();
    double exchangeRate = MonetaryConversions.getExchangeRateProvider().getExchangeRate(from, to).getFactor().doubleValue();
    String exchangeRateString = new DecimalFormat("#.###", new DecimalFormatSymbols(Locale.US)).format(exchangeRate);
    future.complete(new ExchangeRate().withFrom(from).withTo(to).withExchangeRate(exchangeRateString));
    return future;
  }
}
