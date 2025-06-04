package org.folio.services.configuration;

import static org.folio.rest.util.ResourcePathResolver.COMMON_SETTINGS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.finance.CommonSettingsCollection;
import org.folio.rest.acq.model.finance.Value;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class CommonSettingsService {
  private static final Logger log = LogManager.getLogger();

  private static final String TENANT_LOCALE_QUERY = "(scope==stripes-core.prefs.manage and key==tenantLocaleSettings)";

  public static final String CURRENCY_SETTING = "currency";
  public static final String DEFAULT_CURRENCY = "USD";

  public static final String TIMEZONE_SETTING = "timezone";
  public static final String DEFAULT_TIMEZONE = "UTC";

  private final RestClient restClient;

  public CommonSettingsService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<String> getSystemTimeZone(RequestContext requestContext) {
    return loadTenantLocaleSettings(TIMEZONE_SETTING, DEFAULT_TIMEZONE, requestContext);
  }

  public Future<String> getSystemCurrency(RequestContext requestContext) {
    return loadTenantLocaleSettings(CURRENCY_SETTING, DEFAULT_CURRENCY, requestContext);
  }

  private Future<String> loadTenantLocaleSettings(String key, String defaultValue,  RequestContext requestContext) {
    log.debug("loadConfiguration:: Trying to load common settings by moduleConfig={}", key);
    return Future.succeededFuture()
      .map(v -> new RequestEntry(resourcesPath(COMMON_SETTINGS))
        .withOffset(0)
        .withLimit(Integer.MAX_VALUE)
        .withQuery(TENANT_LOCALE_QUERY))
      .compose(requestEntry -> restClient.get(requestEntry.buildEndpoint(), CommonSettingsCollection.class, requestContext))
      .map(settings -> {
        if (log.isDebugEnabled()) {
          log.debug("loadSettings:: The response from mod-settings: {}", JsonObject.mapFrom(settings).encodePrettily());
        }
        if (CollectionUtils.isEmpty(settings.getItems())) {
          return defaultValue;
        }
        Value value = settings.getItems().getFirst().getValue();
        if (value == null) {
          return defaultValue;
        }
        Object result = value.getAdditionalProperties().get(key);
        if (result == null) {
          return defaultValue;
        }
        return result.toString();
      });
  }
}
