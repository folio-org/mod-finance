package org.folio.services.configuration;

import io.vertx.core.Future;
import org.folio.rest.acq.model.finance.CommonSetting;
import org.folio.rest.acq.model.finance.CommonSettingsCollection;
import org.folio.rest.acq.model.finance.Value;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.util.CopilotGenerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

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
    commonSettingsService = new CommonSettingsService(restClient);
  }

  @Test
  void systemTimeZoneReturnsConfiguredValue() {
    CommonSettingsCollection collection = new CommonSettingsCollection();
    CommonSetting settings = new CommonSetting();
    Value value = new Value();
    value.getAdditionalProperties().put(CommonSettingsService.TIMEZONE_SETTING, "Europe/Paris");
    settings.setValue(value);
    collection.getItems().add(settings);

    when(restClient.get(anyString(), eq(CommonSettingsCollection.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture(collection));

    String result = commonSettingsService.getSystemTimeZone(requestContext).result();
    assertEquals("Europe/Paris", result);
  }

  @Test
  void systemCurrencyReturnsConfiguredValue() {
    CommonSettingsCollection collection = new CommonSettingsCollection();
    CommonSetting settings = new CommonSetting();
    Value value = new Value();
    value.getAdditionalProperties().put(CURRENCY_SETTING, "GBP");
    settings.setValue(value);
    collection.getItems().add(settings);

    when(restClient.get(anyString(), eq(CommonSettingsCollection.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture(collection));

    String result = commonSettingsService.getSystemCurrency(requestContext).result();
    assertEquals("GBP", result);
  }

  @Test
  void systemTimeZoneReturnsDefaultWhenNoItemsFound() {
    CommonSettingsCollection collection = new CommonSettingsCollection();

    when(restClient.get(anyString(), eq(CommonSettingsCollection.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture(collection));

    String result = commonSettingsService.getSystemTimeZone(requestContext).result();
    assertEquals(DEFAULT_TIMEZONE, result);
  }

  @Test
  void systemCurrencyReturnsDefaultWhenValueIsNull() {
    CommonSettingsCollection collection = new CommonSettingsCollection();
    CommonSetting settings = new CommonSetting();
    settings.setValue(null);
    collection.getItems().add(settings);

    when(restClient.get(anyString(), eq(CommonSettingsCollection.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture(collection));

    String result = commonSettingsService.getSystemCurrency(requestContext).result();
    assertEquals(DEFAULT_CURRENCY, result);
  }

  @Test
  void systemTimeZoneReturnsDefaultWhenKeyIsMissing() {
    CommonSettingsCollection collection = new CommonSettingsCollection();
    CommonSetting settings = new CommonSetting();
    Value value = new Value();
    // Missing timezone key in additional properties.
    value.getAdditionalProperties().put(CURRENCY_SETTING, "AUD");
    settings.setValue(value);
    collection.getItems().add(settings);

    when(restClient.get(anyString(), eq(CommonSettingsCollection.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture(collection));

    String result = commonSettingsService.getSystemTimeZone(requestContext).result();
    assertEquals(DEFAULT_TIMEZONE, result);
  }

  @Test
  void systemCurrencyReturnsDefaultWhenKeyIsMissing() {
    CommonSettingsCollection collection = new CommonSettingsCollection();
    CommonSetting settings = new CommonSetting();
    Value value = new Value();
    // Missing currency key in additional properties.
    value.getAdditionalProperties().put(TIMEZONE_SETTING, "Asia/Tokyo");
    settings.setValue(value);
    collection.getItems().add(settings);

    when(restClient.get(anyString(), eq(CommonSettingsCollection.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture(collection));

    String result = commonSettingsService.getSystemCurrency(requestContext).result();
    assertEquals(DEFAULT_CURRENCY, result);
  }
}
