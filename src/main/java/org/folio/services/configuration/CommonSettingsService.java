package org.folio.services.configuration;

import static org.folio.rest.util.ResourcePathResolver.LOCALE_SETTINGS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;

import io.vertx.core.Future;

public class CommonSettingsService {
  private static final Logger log = LogManager.getLogger();

  public static final String CURRENCY_SETTING = "currency";
  public static final String DEFAULT_CURRENCY = "USD";

  public static final String TIMEZONE_SETTING = "timezone";
  public static final String DEFAULT_TIMEZONE = "UTC";

  private final RestClient restClient;

  public CommonSettingsService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<String> getSystemTimeZone(RequestContext requestContext) {
    return getLocaleSetting(TIMEZONE_SETTING, DEFAULT_TIMEZONE, requestContext);
  }

  public Future<String> getSystemCurrency(RequestContext requestContext) {
    return getLocaleSetting(CURRENCY_SETTING, DEFAULT_CURRENCY, requestContext);
  }

  private Future<String> getLocaleSetting(String settingName, String defaultValue, RequestContext requestContext) {
    log.debug("getSystemSetting:: Trying to load {} from locale settings", settingName);
    return restClient.getJsonObject(resourcesPath(LOCALE_SETTINGS), requestContext)
      .map(jsonObject -> {
        if (jsonObject == null) {
          return defaultValue;
        }
        var value = jsonObject.getString(settingName);
        return StringUtils.isNotBlank(value) ? value : defaultValue;
      });
  }
}
