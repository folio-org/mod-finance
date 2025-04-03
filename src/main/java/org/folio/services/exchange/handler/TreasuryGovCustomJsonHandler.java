package org.folio.services.exchange.handler;

import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.ExchangeRateSource;

import javax.ws.rs.core.HttpHeaders;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

@Log4j2
public class TreasuryGovCustomJsonHandler extends AbstractCustomJsonHandler {

  private static final String DATA = "data";
  private static final String EXCHANGE_RATE = "exchange_rate";

  public TreasuryGovCustomJsonHandler(HttpClient httpClient, ExchangeRateSource rateSource) {
    super(httpClient, rateSource);
  }

  @Override
  @SneakyThrows
  public BigDecimal getExchangeRateFromApi(String from, String to) {
    if (!StringUtils.equals(from, "USD")) {
      throw new IllegalStateException("Treasury custom exchange rate API only supports USD as a 'from' currency");
    }

    var currentQuarterLastDate = getFiscalQuarterLastDate();
    var preparedUri = String.format("%s?fields=country_currency_desc,exchange_rate,record_date"
        + "&filter=country_currency_desc:in:(%s),record_date:gte:%s"
        + "&sort=-record_date"
        + "&page[size]=1",
      super.rateSource.getProviderUri(), CountryCurrency.valueOf(to).value, currentQuarterLastDate).replace(" " , "+");
    var httpRequest = HttpRequest.newBuilder()
      .uri(new URI(preparedUri))
      .headers(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF_8).GET()
      .build();

    var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    log.debug("getExchangeRateFromApi:: Status code: {}, body: {}", httpResponse.statusCode(), httpResponse.body());

    var exchangeRate = new JsonObject(httpResponse.body())
      .getJsonArray(DATA)
      .getJsonObject(0)
      .getString(EXCHANGE_RATE);

    return new BigDecimal(exchangeRate);
  }

  enum CountryCurrency {
    EUR("Euro Zone-Euro"),
    CAD("Canada-Dollar"),
    ILS("Israel-Shekel");

    private final String value;

    CountryCurrency(String value) {
      this.value = value;
    }
  }

  private String getFiscalQuarterLastDate() {
    return ZonedDateTime.now()
      .minus(1, IsoFields.QUARTER_OF_YEAR.getBaseUnit())
      .with(TemporalAdjusters.firstDayOfMonth())
      .minusDays(1)
      .format(DateTimeFormatter.ISO_LOCAL_DATE);
  }
}
