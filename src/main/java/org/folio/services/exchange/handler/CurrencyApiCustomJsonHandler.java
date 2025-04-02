package org.folio.services.exchange.handler;

import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.rest.jaxrs.model.ExchangeRateSource;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Log4j2
public class CurrencyApiCustomJsonHandler extends AbstractCustomJsonHandler {

  public CurrencyApiCustomJsonHandler(ExchangeRateSource rateSource) {
    super(rateSource);
  }

  @Override
  @SneakyThrows
  public BigDecimal getExchangeRateFromApi(String from, String to) {
    var httpRequest = HttpRequest.newBuilder()
      .uri(new URI(String.format(rateSource.getProviderUri(), rateSource.getApiKey(), from, to)))
      .headers(CONTENT_TYPE, APPLICATION_JSON_UTF_8).GET()
      .build();

    var httpResponse = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    log.info("getExchangeRateFromCurrencyApiCom:: Status code: {}, body: {}", httpResponse.statusCode(), httpResponse.body());

    var exchangeRate = new JsonObject(httpResponse.body())
      .getJsonObject("data")
      .getJsonObject(to)
      .getString("exchangeRate");

    return new BigDecimal(exchangeRate);
  }
}
