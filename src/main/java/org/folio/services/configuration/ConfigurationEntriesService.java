package org.folio.services.configuration;

import static org.folio.rest.util.ResourcePathResolver.CONFIGURATIONS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Configs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class ConfigurationEntriesService {
  private static final Logger log = LogManager.getLogger();

  private static final String CONFIG_QUERY = "module==%s";
  public static final String LOCALE_SETTINGS = "localeSettings";

  public static final String SYSTEM_CONFIG_MODULE_NAME = "ORG";
  public static final String CURRENCY_CONFIG = "currency";
  public static final String DEFAULT_CURRENCY = "USD";

  public static final String TZ_CONFIG = "timezone";
  public static final String TZ_UTC = "UTC";

  private final RestClient restClient;

  public ConfigurationEntriesService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<JsonObject> loadConfiguration(String moduleConfig, RequestContext requestContext) {
    return Future.succeededFuture()
      .map(v -> {
        String query = String.format(CONFIG_QUERY, moduleConfig);
        log.info("GET request: {}", query);
        return new RequestEntry(resourcesPath(CONFIGURATIONS))
          .withOffset(0)
          .withLimit(Integer.MAX_VALUE)
          .withQuery(query);
      })
      .compose(requestEntry -> restClient.get(requestEntry.buildEndpoint(), Configs.class, requestContext))
      .map(configs -> {
        if (log.isDebugEnabled()) {
          log.debug("The response from mod-configuration: {}", JsonObject.mapFrom(configs).encodePrettily());
        }
        JsonObject config = new JsonObject();
        configs.getConfigs().forEach(entry -> config.put(entry.getConfigName(), entry.getValue()));
        return config;
      });
  }

  public Future<String> getSystemCurrency(RequestContext requestContext) {
    return loadConfiguration(SYSTEM_CONFIG_MODULE_NAME, requestContext)
      .map(jsonConfig -> extractLocalSettingConfigValueByName(jsonConfig, CURRENCY_CONFIG, DEFAULT_CURRENCY));
  }

  public Future<String> getSystemTimeZone(RequestContext requestContext) {
    return loadConfiguration(SYSTEM_CONFIG_MODULE_NAME, requestContext)
      .map(jsonConfig -> extractLocalSettingConfigValueByName(jsonConfig, TZ_CONFIG, TZ_UTC));
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
