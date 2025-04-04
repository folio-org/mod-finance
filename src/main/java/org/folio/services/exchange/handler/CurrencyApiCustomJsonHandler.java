package org.folio.services.exchange.handler;

import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.rest.jaxrs.model.ExchangeRateSource;

import javax.ws.rs.core.HttpHeaders;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Log4j2
public class CurrencyApiCustomJsonHandler extends AbstractCustomJsonHandler {

  private static final String URI_TEMPLATE = "%s?base_currency=%s&currencies=%s";
  private static final String API_KEY = "apikey";
  private static final String DATA = "data";
  private static final String VALUE = "value";

  public CurrencyApiCustomJsonHandler(HttpClient httpClient, ExchangeRateSource rateSource) {
    super(httpClient, rateSource);
  }

  @Override
  @SneakyThrows
  public BigDecimal getExchangeRateFromApi(String from, String to) {
    var preparedUri = String.format(URI_TEMPLATE, rateSource.getProviderUri(), from, to);
    var httpRequest = HttpRequest.newBuilder()
      .uri(new URI(preparedUri))
      .headers(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF_8, API_KEY, rateSource.getApiKey()).GET()
      .build();

    var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    log.debug("getExchangeRateFromApi:: Status code: {}, body: {}", httpResponse.statusCode(), httpResponse.body());

    var exchangeRate = new JsonObject(httpResponse.body())
      .getJsonObject(DATA)
      .getJsonObject(to)
      .getString(VALUE);

    return new BigDecimal(exchangeRate);
  }
}
