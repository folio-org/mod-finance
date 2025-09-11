package org.folio.services.exchange;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.jaxrs.model.ExchangeRateSource;
import org.folio.services.exchange.handler.ConveraCustomJsonHandler;
import org.folio.services.exchange.handler.CurrencyApiCustomJsonHandler;
import org.folio.services.exchange.handler.TreasuryGovCustomJsonHandler;
import org.javamoney.moneta.convert.ExchangeRateBuilder;
import org.javamoney.moneta.spi.AbstractRateProvider;
import org.javamoney.moneta.spi.DefaultNumberValue;

import javax.money.convert.ConversionContext;
import javax.money.convert.ConversionQuery;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ProviderContext;
import javax.money.convert.ProviderContextBuilder;
import javax.money.convert.RateType;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import org.folio.rest.jaxrs.model.ExchangeRate.OperationMode;

@Log4j2
public class CustomJsonExchangeRateProvider extends AbstractRateProvider {

  private static final ProviderContext CONTEXT = ProviderContextBuilder.of("CUSTOM", RateType.REALTIME)
    .set("providerDescription", "Custom exchange rate provider")
    .build();

  private final HttpClient httpClient;
  private final ExchangeRateSource rateSource;
  private final OperationMode operationMode;
  private final Cache<@NonNull String, Pair<BigDecimal, OperationMode>> exchangeRateCache;

  public CustomJsonExchangeRateProvider(HttpClient httpClient, ExchangeRateSource rateSource,
                                        Cache<@NonNull String, Pair<BigDecimal, OperationMode>> exchangeRateCache) {
    this(httpClient, rateSource, OperationMode.MULTIPLY, exchangeRateCache);
  }

  public CustomJsonExchangeRateProvider(HttpClient httpClient, ExchangeRateSource rateSource, OperationMode operationMode,
                                        Cache<@NonNull String, Pair<BigDecimal, OperationMode>> exchangeRateCache) {
    super(CONTEXT);
    this.httpClient = httpClient;
    this.rateSource = rateSource;
    this.operationMode = operationMode;
    this.exchangeRateCache = exchangeRateCache;
  }

  @Override
  public ExchangeRate getExchangeRate(ConversionQuery query) {
    var from = query.getBaseCurrency();
    var to = query.getCurrency();
    var builder = new ExchangeRateBuilder(ConversionContext.of());
    builder.setBase(from);
    builder.setTerm(to);
    var exchangeRatePair = getCachedExchangeRate(from.getCurrencyCode(), to.getCurrencyCode());
    builder.setFactor(DefaultNumberValue.of(exchangeRatePair.getLeft()));
    return builder.build();
  }

  @Override
  public CurrencyConversion getCurrencyConversion(ConversionQuery conversionQuery) {
    return new ManualCurrencyConversion(conversionQuery, this, ConversionContext.of(this.getContext().getProviderName(), RateType.ANY), operationMode);
  }

  public Pair<BigDecimal, OperationMode> getCachedExchangeRate(String from, String to) {
    var cacheKey = "%s-%s".formatted(from, to);
    var exchangeRate = exchangeRateCache.get(cacheKey, key -> getExchangeRateFromHandler(from, to));
    log.info("getExchangeRateFromHandler:: Using {} handler with exchange rate {} -> {}: {}", rateSource.getProviderType().name(), from, to, exchangeRate);
    return exchangeRate;
  }

  private Pair<BigDecimal, OperationMode> getExchangeRateFromHandler(String from, String to) {
    var handler = switch (rateSource.getProviderType()) {
      case CURRENCYAPI_COM -> new CurrencyApiCustomJsonHandler(httpClient, rateSource);
      case TREASURY_GOV -> new TreasuryGovCustomJsonHandler(httpClient, rateSource);
      case CONVERA_COM -> new ConveraCustomJsonHandler(httpClient, rateSource);
    };
    return handler.getExchangeRateFromApi(from, to);
  }
}
