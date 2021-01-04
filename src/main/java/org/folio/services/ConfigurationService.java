package org.folio.services;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Configs;
import org.folio.rest.util.ErrorCodes;

import io.vertx.core.json.JsonObject;

public class ConfigurationService {

    private static final String CURRENCY = "currency";
    private static final String LOCALE_SETTINGS = "localeSettings";
    private static final String DEFAULT_CURRENCY = "USD";

    public static final String CONFIG_QUERY = "module==%s and configName==%s";
    public static final String SYSTEM_CONFIG_MODULE_NAME = "ORG";
    public static final String SYSTEM_CONFIG_QUERY = String.format(CONFIG_QUERY, SYSTEM_CONFIG_MODULE_NAME, LOCALE_SETTINGS);

    private final RestClient configEntriesRestClient;

    public ConfigurationService(RestClient configEntriesRestClient) {
        this.configEntriesRestClient = configEntriesRestClient;
    }


    public CompletableFuture<String> getSystemCurrency(RequestContext requestContext) {
        return configEntriesRestClient.get(SYSTEM_CONFIG_QUERY, 0, 1, requestContext, Configs.class)
                .thenApply(this::getCurrency);
    }

    private String getCurrency(Configs configs) {
        if (configs.getConfigs().isEmpty()) {
            return DEFAULT_CURRENCY;
        }
        return Optional.ofNullable(configs.getConfigs().get(0).getValue())
                .map(settings -> new JsonObject(settings).getString(CURRENCY))
                .orElseThrow(() -> new HttpException(500, ErrorCodes.CURRENCY_NOT_FOUND));
    }


}
