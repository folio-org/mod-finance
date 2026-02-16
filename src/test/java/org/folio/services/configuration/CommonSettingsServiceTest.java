package org.folio.services.configuration;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.util.CopilotGenerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.folio.services.configuration.CommonSettingsService.CURRENCY_SETTING;
import static org.folio.services.configuration.CommonSettingsService.DEFAULT_CURRENCY;
import static org.folio.services.configuration.CommonSettingsService.DEFAULT_TIMEZONE;
import static org.folio.services.configuration.CommonSettingsService.TIMEZONE_SETTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@CopilotGenerated(model = "o3-mini")
public class CommonSettingsServiceTest {
  @Mock
  private RestClient restClient;
  @Mock
  private RequestContext requestContext;

  private CommonSettingsService commonSettingsService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
    commonSettingsService = new CommonSettingsService(restClient);
  }

  @Test
  void systemTimeZoneReturnsConfiguredValue() {
    var localeResponse = new JsonObject()
      .put("locale", "en-US")
      .put(CURRENCY_SETTING, "USD")
      .put(TIMEZONE_SETTING, "Europe/Paris")
      .put("numberingSystem", "latn");

    when(restClient.getAsJsonObject(anyString(), eq(requestContext)))
      .thenReturn(Future.succeededFuture(localeResponse));

    String result = commonSettingsService.getSystemTimeZone(requestContext).result();
    assertEquals("Europe/Paris", result);
  }

  @Test
  void systemCurrencyReturnsConfiguredValue() {
    var localeResponse = new JsonObject()
      .put("locale", "en-GB")
      .put(CURRENCY_SETTING, "GBP")
      .put(TIMEZONE_SETTING, "Europe/London")
      .put("numberingSystem", "latn");

    when(restClient.getAsJsonObject(anyString(), eq(requestContext)))
      .thenReturn(Future.succeededFuture(localeResponse));

    String result = commonSettingsService.getSystemCurrency(requestContext).result();
    assertEquals("GBP", result);
  }

  @Test
  void systemTimeZoneReturnsDefaultWhenResponseIsNull() {
    when(restClient.getAsJsonObject(anyString(), eq(requestContext)))
      .thenReturn(Future.succeededFuture(null));

    String result = commonSettingsService.getSystemTimeZone(requestContext).result();
    assertEquals(DEFAULT_TIMEZONE, result);
  }

  @Test
  void systemCurrencyReturnsDefaultWhenResponseIsNull() {
    when(restClient.getAsJsonObject(anyString(), eq(requestContext)))
      .thenReturn(Future.succeededFuture(null));

    String result = commonSettingsService.getSystemCurrency(requestContext).result();
    assertEquals(DEFAULT_CURRENCY, result);
  }

  @Test
  void systemTimeZoneReturnsDefaultWhenFieldIsBlank() {
    var localeResponse = new JsonObject()
      .put(TIMEZONE_SETTING, "")
      .put(CURRENCY_SETTING, "USD");

    when(restClient.getAsJsonObject(anyString(), eq(requestContext)))
      .thenReturn(Future.succeededFuture(localeResponse));

    String result = commonSettingsService.getSystemTimeZone(requestContext).result();
    assertEquals(DEFAULT_TIMEZONE, result);
  }

  @Test
  void systemCurrencyReturnsDefaultWhenFieldIsMissing() {
    var localeResponse = new JsonObject()
      .put(TIMEZONE_SETTING, "Asia/Tokyo");

    when(restClient.getAsJsonObject(anyString(), eq(requestContext)))
      .thenReturn(Future.succeededFuture(localeResponse));

    String result = commonSettingsService.getSystemCurrency(requestContext).result();
    assertEquals(DEFAULT_CURRENCY, result);
  }

  @Test
  void systemTimeZoneReturnsDefaultWhenResponseIsEmpty() {
    when(restClient.getAsJsonObject(anyString(), eq(requestContext)))
      .thenReturn(Future.succeededFuture(new JsonObject()));

    String result = commonSettingsService.getSystemTimeZone(requestContext).result();
    assertEquals(DEFAULT_TIMEZONE, result);
  }
}
