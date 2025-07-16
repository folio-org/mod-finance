package org.folio.services.exchange;

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

@Log4j2
public class CustomJsonExchangeRateProvider extends AbstractRateProvider {

  private static final ProviderContext CONTEXT = ProviderContextBuilder.of("CUSTOM", RateType.REALTIME)
    .set("providerDescription", "Custom exchange rate provider")
    .build();

  private final HttpClient httpClient;
  private final ExchangeRateSource rateSource;
  private final org.folio.rest.jaxrs.model.ExchangeRate.OperationMode operationMode;

  public CustomJsonExchangeRateProvider(HttpClient httpClient, ExchangeRateSource rateSource) {
    super(CONTEXT);
    this.httpClient = httpClient;
    this.rateSource = rateSource;
    this.operationMode = org.folio.rest.jaxrs.model.ExchangeRate.OperationMode.MULTIPLY;
  }

  public CustomJsonExchangeRateProvider(HttpClient httpClient, ExchangeRateSource rateSource,
                                        org.folio.rest.jaxrs.model.ExchangeRate.OperationMode operationMode) {
    super(CONTEXT);
    this.httpClient = httpClient;
    this.rateSource = rateSource;
    this.operationMode = operationMode;
  }

  @Override
  public ExchangeRate getExchangeRate(ConversionQuery query) {
    var from = query.getBaseCurrency();
    var to = query.getCurrency();

    var exchangeRatePair = getExchangeRateFromHandler(from.getCurrencyCode(), to.getCurrencyCode());
    var builder = new ExchangeRateBuilder(ConversionContext.of());
    builder.setBase(from);
    builder.setTerm(to);
    builder.setFactor(DefaultNumberValue.of(exchangeRatePair.getLeft()));

    return builder.build();
  }

  @Override
  public CurrencyConversion getCurrencyConversion(ConversionQuery conversionQuery) {
    return new ManualCurrencyConversion(conversionQuery, this, ConversionContext.of(this.getContext().getProviderName(), RateType.ANY), operationMode);
  }

  public Pair<BigDecimal, org.folio.rest.jaxrs.model.ExchangeRate.OperationMode> getExchangeRateFromHandler(String from, String to) {
    var handler = switch (rateSource.getProviderType()) {
      case CURRENCYAPI_COM -> new CurrencyApiCustomJsonHandler(httpClient, rateSource);
      case TREASURY_GOV -> new TreasuryGovCustomJsonHandler(httpClient, rateSource);
      case CONVERA_COM -> new ConveraCustomJsonHandler(httpClient, rateSource);
    };

    var exchangeRate = handler.getExchangeRateFromApi(from, to);
    log.info("getExchangeRateFromHandler:: Using {} handler with exchange rate {} -> {}: {}",
      rateSource.getProviderType().name(), from, to, exchangeRate);

    return exchangeRate;
  }
}
