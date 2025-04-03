package org.folio.services.exchange;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.HttpStatus;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.helper.ExchangeHelper;
import org.folio.rest.jaxrs.model.ExchangeRate;
import org.folio.rest.jaxrs.model.ExchangeRateSource;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;

import javax.money.convert.ConversionQueryBuilder;
import java.net.http.HttpClient;
import java.util.Objects;

import static org.folio.rest.util.ResourcePathResolver.EXCHANGE_RATE_SOURCE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

@Service
public class ExchangeService {

  private final RestClient restClient;
  private final HttpClient httpClient;

  public ExchangeService(RestClient restClient, HttpClient httpClient) {
    this.restClient = restClient;
    this.httpClient = httpClient;
  }

  public Future<ExchangeRate> getExchangeRate(String from, String to, Context context, RequestContext requestContext) {
    return getExchangeRateSource(requestContext)
      .flatMap(rateSource -> {
        if (Objects.isNull(rateSource) || Boolean.FALSE.equals(rateSource.getEnabled())) {
          var exchangeRate = new ExchangeHelper(context).getExchangeRate(from, to);
          return Future.succeededFuture(exchangeRate);
        }
        var provider = new CustomJsonExchangeRateProvider(httpClient, rateSource);
        var exchangeRate = new ExchangeRate().withFrom(from).withTo(to)
          .withExchangeRate(provider.getExchangeRateFromHandler(from, to).doubleValue());
        return Future.succeededFuture(exchangeRate);
      });
  }

  public Future<Double> calculateExchange(String from, String to, Number amount, Number customRate, Context context,
                                          RequestContext requestContext) {
    return getExchangeRateSource(requestContext)
      .flatMap(rateSource -> {
        if (Objects.isNull(rateSource) || Boolean.FALSE.equals(rateSource.getEnabled()) || Objects.nonNull(customRate)) {
          var convertedAmount = new ExchangeHelper(context).calculateExchange(from, to, amount, customRate);
          return Future.succeededFuture(convertedAmount);
        }
        var provider = new CustomJsonExchangeRateProvider(httpClient, rateSource);
        var query = ConversionQueryBuilder.of()
          .setBaseCurrency(from).setTermCurrency(to)
          .build();
        var convertedAmount = Money.of(amount, from)
          .with(provider.getCurrencyConversion(query))
          .getNumber()
          .doubleValueExact();
        return Future.succeededFuture(convertedAmount);
      });
  }

  public Future<ExchangeRateSource> getExchangeRateSource(RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(EXCHANGE_RATE_SOURCE));
    return restClient.get(requestEntry.buildEndpoint(), ExchangeRateSource.class, requestContext)
      .recover(e -> {
        if (e instanceof HttpException ex) {
          if (ex.getCode() == HttpStatus.HTTP_NOT_FOUND.toInt()) {
            return Future.succeededFuture(null);
          }
        }
        return Future.failedFuture(e);
      });
  }
}
