package org.folio.services.configuration;

import io.vertx.core.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Configs;

import io.vertx.core.json.JsonObject;

public class ConfigurationEntriesService {
  private static final Logger logger = LogManager.getLogger();

  private static final String CONFIG_QUERY = "module==%s";
  public static final String LOCALE_SETTINGS = "localeSettings";

  public static final String SYSTEM_CONFIG_MODULE_NAME = "ORG";
  public static final String CURRENCY_CONFIG = "currency";
  public static final String DEFAULT_CURRENCY = "USD";

  public static final String TZ_CONFIG = "timezone";
  public static final String TZ_UTC = "UTC";

  private final RestClient configEntriesRestClient;

  public ConfigurationEntriesService(RestClient configEntriesRestClient) {
    this.configEntriesRestClient = configEntriesRestClient;
  }

  public Future<JsonObject> loadConfiguration(String moduleConfig, RequestContext requestContext) {
    String query = String.format(CONFIG_QUERY, moduleConfig);
    logger.info("GET request: {}", query);
    return configEntriesRestClient.get(query, 0, Integer.MAX_VALUE, requestContext, Configs.class)
      .map(configs -> {
        if (logger.isDebugEnabled()) {
          logger.debug("The response from mod-configuration: {}", JsonObject.mapFrom(configs).encodePrettily());
        }
        JsonObject config = new JsonObject();
        configs.getConfigs().forEach(entry -> config.put(entry.getConfigName(), entry.getValue()));
        return config;
      });
  }

  public Future<String> getSystemCurrency(RequestContext requestContext) {
    Future<String> future = new Future<>();
    loadConfiguration(SYSTEM_CONFIG_MODULE_NAME, requestContext)
      .map(jsonConfig -> extractLocalSettingConfigValueByName(jsonConfig, CURRENCY_CONFIG, DEFAULT_CURRENCY))
      .thenAccept(future::complete)
      .exceptionally(t -> {
        future.completeExceptionally(t);
        return null;
      });
    return future;
  }

  public Future<String> getSystemTimeZone(RequestContext requestContext) {
    Future<String> future = new Future<>();
    loadConfiguration(SYSTEM_CONFIG_MODULE_NAME, requestContext)
      .map(jsonConfig -> extractLocalSettingConfigValueByName(jsonConfig, TZ_CONFIG, TZ_UTC))
      .thenAccept(future::complete)
      .exceptionally(t -> {
        future.completeExceptionally(t);
        return null;
      });
    return future;
  }

  private String extractLocalSettingConfigValueByName(JsonObject config, String name, String defaultValue) {
    String localeSettings = config.getString(LOCALE_SETTINGS);
    String confValue;
    if (StringUtils.isEmpty(localeSettings)) {
      confValue = defaultValue;
    } else {
      confValue = new JsonObject(config.getString(LOCALE_SETTINGS)).getString(name, defaultValue);
    }
    return confValue;
  }
}
