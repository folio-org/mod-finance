package org.folio.services.exchange;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.HttpStatus;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExchangeRateSource;
import org.folio.util.CopilotGenerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.folio.rest.acq.model.finance.ExchangeRate.OperationMode.DIVIDE;
import static org.folio.rest.acq.model.finance.ExchangeRate.OperationMode.MULTIPLY;
import static org.folio.rest.jaxrs.model.ExchangeRateSource.ProviderType.CURRENCYAPI_COM;
import static org.folio.rest.jaxrs.model.ExchangeRateSource.ProviderType.TREASURY_GOV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@ExtendWith(VertxExtension.class)
@CopilotGenerated(partiallyGenerated = true)
public class ExchangeServiceTest {

  @Mock private RestClient restClient;
  @Mock private HttpClient httpClient;
  @Mock private HttpResponse<String> httpResponse;
  @Mock private RequestContext requestContext;
  @InjectMocks private ExchangeService exchangeService;

  private AutoCloseable openMocks;

  @BeforeEach
  void setUp() {
    openMocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (openMocks != null) {
      openMocks.close();
    }
  }

  @ParameterizedTest
  @CsvSource({"TREASURY_GOV,0.961d", "CURRENCYAPI_COM,0.9052401139"})
  void testGetExchangeRateUsingCustomJsonExchangeRateProvider(ExchangeRateSource.ProviderType providerType, double expectedAmount,
                                                              VertxTestContext testContext) throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(providerType));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(providerType)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    exchangeService.getExchangeRate("USD", "EUR", requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertEquals("USD", result.getFrom());
        assertEquals("EUR", result.getTo());
        assertEquals(expectedAmount, result.getExchangeRate());
        testContext.completeNow();
      })));
  }

  @Test
  void testGetExchangeRateUsingCustomJsonExchangeRateProviderWithUsdInBothSidesUsingTreasureGov(VertxTestContext testContext)
    throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(TREASURY_GOV));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(TREASURY_GOV)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    exchangeService.getExchangeRate("USD", "USD", requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertEquals(result.getFrom(), result.getTo());
        assertEquals(1, result.getExchangeRate());
        assertEquals(MULTIPLY.name(), result.getOperationMode().name());
        testContext.completeNow();
      })));
  }

  @Test
  void testGetExchangeRateUsingCustomJsonExchangeRateProviderWithDifferentTreasureGovFromCurrency(VertxTestContext testContext)
    throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(TREASURY_GOV));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(TREASURY_GOV)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    exchangeService.getExchangeRate("EUR", "USD", requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertEquals("EUR", result.getFrom());
        assertEquals("USD", result.getTo());
        assertNotEquals(0, result.getExchangeRate(), 0.0);
        assertEquals(DIVIDE.name(), result.getOperationMode().name());
        testContext.completeNow();
      })));
  }

  @ParameterizedTest
  @CsvSource({"TREASURY_GOV,9.61d", "CURRENCYAPI_COM,9.052401139"})
  void testCalculateExchangeRateUsingCustomJsonExchangeRateProvider(ExchangeRateSource.ProviderType providerType, double expectedAmount,
                                                                    VertxTestContext testContext) throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(providerType));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(providerType)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    exchangeService.calculateExchange("USD", "EUR", 10, null, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertEquals(expectedAmount, result);
        testContext.completeNow();
      })));
  }

  private ExchangeRateSource createExchangeRateSource(ExchangeRateSource.ProviderType providerType) {
    return switch (providerType) {
      case TREASURY_GOV -> new ExchangeRateSource()
        .withId(UUID.randomUUID().toString()).withEnabled(true)
        .withProviderType(TREASURY_GOV)
        .withProviderUri("https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange");
      case CURRENCYAPI_COM -> new ExchangeRateSource()
        .withId(UUID.randomUUID().toString()).withEnabled(true)
        .withProviderType(CURRENCYAPI_COM)
        .withProviderUri("https://api.currencyapi.com/v3/latest")
        .withApiKey("apiKey");
      case CONVERA_COM -> null;
    };
  }

  private String createResponseBody(ExchangeRateSource.ProviderType providerType) {
    return switch (providerType) {
      case TREASURY_GOV -> new JsonObject().put("data", new JsonArray()
        .add(new JsonObject()
          .put("country_currency_desc", "Euro Zone-Euro")
          .put("exchange_rate", "0.961")
          .put("record_date", "2024-12-31")))
        .toString();
      case CURRENCYAPI_COM -> new JsonObject().put("data", new JsonObject()
        .put("EUR", new JsonObject()
          .put("code", "EUR")
          .put("value", "0.9052401139")))
        .toString();
      case CONVERA_COM -> null;
    };
  }
}
