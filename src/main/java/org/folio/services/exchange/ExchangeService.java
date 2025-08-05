package org.folio.services.exchange;

import com.github.benmanes.caffeine.cache.Cache;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import one.util.streamex.StreamEx;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.HttpStatus;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.helper.ExchangeHelper;
import org.folio.rest.jaxrs.model.ExchangeRate;
import org.folio.rest.jaxrs.model.ExchangeRate.OperationMode;
import org.folio.rest.jaxrs.model.ExchangeRateCalculation;
import org.folio.rest.jaxrs.model.ExchangeRateCalculations;
import org.folio.rest.jaxrs.model.ExchangeRateSource;
import org.folio.rest.jaxrs.model.ExchangeRateSource.ProviderType;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.money.convert.ConversionQueryBuilder;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static org.folio.rest.util.CacheUtils.buildCache;
import static org.folio.rest.util.ResourcePathResolver.EXCHANGE_RATE_SOURCE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.services.exchange.handler.TreasuryGovCustomJsonHandler.getOperationMode;

@Service
public class ExchangeService {

  private final RestClient restClient;
  private final HttpClient httpClient;
  private Map<ProviderType, Cache<String, Pair<BigDecimal, OperationMode>>> exchangeProviderCaches;

  @Value("${finance.cache.exchange-rate.expiration.time.seconds:300}")
  private long cacheExpirationTime;

  @PostConstruct
  void init() {
    var context = Vertx.currentContext();
    this.exchangeProviderCaches = StreamEx.of(ExchangeRateSource.ProviderType.values())
      .toMap(Function.identity(), providerType -> buildCache(context, cacheExpirationTime));
  }

  public ExchangeService(RestClient restClient, HttpClient httpClient) {
    this.restClient = restClient;
    this.httpClient = httpClient;
  }

  public Future<ExchangeRate> getExchangeRate(String from, String to, RequestContext requestContext) {
    return getExchangeRateSource(requestContext)
      .map(rateSource -> {
        if (isRateSourceUnavailable(rateSource)) {
          return new ExchangeHelper(requestContext.context()).getExchangeRate(from, to);
        }
        var provider = new CustomJsonExchangeRateProvider(httpClient, rateSource, exchangeProviderCaches.get(rateSource.getProviderType()));
        var exchangeRatePair = provider.getCachedExchangeRate(from, to);
        return new ExchangeRate().withFrom(from).withTo(to)
          .withExchangeRate(exchangeRatePair.getLeft().doubleValue())
          .withOperationMode(exchangeRatePair.getRight());
      });
  }

  public Future<Double> calculateExchange(String from, String to, Number amount, Number customRate, RequestContext requestContext) {
    var exchangeHelper = new ExchangeHelper(requestContext.context());
    return getExchangeRateSource(requestContext)
      .map(rateSource -> doCalculateExchange(from, to, amount, customRate, rateSource, exchangeHelper));
  }

  public Future<ExchangeRateCalculations> calculateExchangeBatch(ExchangeRateCalculations exchangeRateCalculations, RequestContext requestContext) {
    var exchangeHelper = new ExchangeHelper(requestContext.context());
    return getExchangeRateSource(requestContext)
      .map(rateSource -> StreamEx.of(exchangeRateCalculations.getExchangeRateCalculations())
        .peek(calculation -> calculation.withCalculation(doCalculateExchange(calculation, rateSource, exchangeHelper)))
        .toList())
      .map(exchangeRateCalculations::withExchangeRateCalculations);
  }

  private Double doCalculateExchange(ExchangeRateCalculation calculation, ExchangeRateSource rateSource, ExchangeHelper exchangeHelper) {
    return doCalculateExchange(calculation.getFrom(), calculation.getTo(), calculation.getAmount(), calculation.getRate(), rateSource, exchangeHelper);
  }

  private Double doCalculateExchange(String from, String to, Number amount, Number customRate, ExchangeRateSource rateSource, ExchangeHelper exchangeHelper) {
    if (isRateSourceUnavailable(rateSource)) {
      return exchangeHelper.calculateExchange(from, to, amount, customRate);
    }
    var operationMode = getOperationMode(rateSource.getProviderType() == ProviderType.TREASURY_GOV, from);
    var provider = new CustomJsonExchangeRateProvider(httpClient, rateSource, operationMode, exchangeProviderCaches.get(rateSource.getProviderType()));
    var query = ConversionQueryBuilder.of()
      .setBaseCurrency(from).setTermCurrency(to)
      .build();
    return Money.of(amount, from)
      .with(provider.getCurrencyConversion(query))
      .getNumber()
      .doubleValueExact();
  }

  private Future<ExchangeRateSource> getExchangeRateSource(RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(EXCHANGE_RATE_SOURCE));
    return restClient.get(requestEntry.buildEndpoint(), ExchangeRateSource.class, requestContext)
      .recover(e -> e instanceof HttpException ex && ex.getCode() == HttpStatus.HTTP_NOT_FOUND.toInt()
        ? Future.succeededFuture()
        : Future.failedFuture(e));
  }

  private boolean isRateSourceUnavailable(ExchangeRateSource rateSource) {
    return Objects.isNull(rateSource) || Boolean.FALSE.equals(rateSource.getEnabled());
  }

}
