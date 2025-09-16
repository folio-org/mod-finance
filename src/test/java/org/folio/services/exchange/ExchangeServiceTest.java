package org.folio.services.exchange;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;
import org.folio.HttpStatus;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.ExchangeRateCalculation;
import org.folio.rest.jaxrs.model.ExchangeRateCalculations;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.folio.rest.acq.model.finance.ExchangeRate.OperationMode.DIVIDE;
import static org.folio.rest.acq.model.finance.ExchangeRate.OperationMode.MULTIPLY;
import static org.folio.rest.jaxrs.model.ExchangeRateSource.ProviderType.CURRENCYAPI_COM;
import static org.folio.rest.jaxrs.model.ExchangeRateSource.ProviderType.TREASURY_GOV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unchecked")
@ExtendWith(VertxExtension.class)
@CopilotGenerated(partiallyGenerated = true, model = "Claude Sonnet 4", mode = "Agent")
public class ExchangeServiceTest {

  @Mock
  private RestClient restClient;
  @Mock
  private HttpClient httpClient;
  @Mock
  private HttpResponse<String> httpResponse;
  @Mock
  private RequestContext requestContext;
  @InjectMocks
  private ExchangeService exchangeService;

  private AutoCloseable openMocks;

  @BeforeEach
  void setUp() {
    openMocks = MockitoAnnotations.openMocks(this);
    exchangeService.init();
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
  @CsvSource({"TREASURY_GOV,0d,false,9.61d", "TREASURY_GOV,10d,true,100d", "CURRENCYAPI_COM,0d,false,9.052401139"})
  void testCalculateExchangeRateUsingCustomJsonExchangeRateProvider(ExchangeRateSource.ProviderType providerType, double exchangeRate, boolean manual,
                                                                    double expectedAmount, VertxTestContext testContext) throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(providerType));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(providerType)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    exchangeService.calculateExchange("USD", "EUR", 10, exchangeRate == 0d ? null : exchangeRate, manual, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertEquals(expectedAmount, result);
        testContext.completeNow();
      })));
  }

  @ParameterizedTest
  @CsvSource({"TREASURY_GOV,0d,false,9.61d", "TREASURY_GOV,10d,true,100d", "CURRENCYAPI_COM,0d,false,9.052401139"})
  void testCalculateExchangeRateBatchUsingCustomJsonExchangeRateProvider(ExchangeRateSource.ProviderType providerType, double exchangeRate, boolean manual,
                                                                         double expectedAmount, VertxTestContext testContext) throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(providerType));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(providerType)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    var calculations = new ExchangeRateCalculations()
      .withExchangeRateCalculations(Arrays.asList(
        new ExchangeRateCalculation()
          .withFrom("USD")
          .withTo("EUR")
          .withAmount(10.0)
          .withRate(exchangeRate == 0d ? null : exchangeRate),
        new ExchangeRateCalculation()
          .withFrom("GBP")
          .withTo("EUR")
          .withAmount(50.0)
          .withRate(null)
      ));

    exchangeService.calculateExchangeBatch(calculations, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertNotNull(result);
        assertNotNull(result.getExchangeRateCalculations());
        assertEquals(2, result.getExchangeRateCalculations().size());

        var firstCalculation = result.getExchangeRateCalculations().getFirst();
        log.info("Provider: {}, first rate: {}, calculation: {}", providerType.name(), firstCalculation.getRate(), firstCalculation.getCalculation());
        assertNotNull(firstCalculation.getCalculation());
        if (manual) {
          assertEquals(exchangeRate, firstCalculation.getRate());
          assertEquals(expectedAmount, firstCalculation.getCalculation());
        } else {
          assertNotNull(firstCalculation.getCalculation());
          assertNull(firstCalculation.getRate());
          assertNotEquals(0.0, firstCalculation.getCalculation());
        }

        var secondCalculation = result.getExchangeRateCalculations().getLast();
        log.info("Provider: {}, second rate: {}, calculation: {}", providerType.name(), secondCalculation.getRate(), secondCalculation.getCalculation());
        assertNotNull(secondCalculation.getCalculation());
        assertNull(secondCalculation.getRate());
        assertNotEquals(0.0, secondCalculation.getCalculation());

        testContext.completeNow();
      })));
  }

  @ParameterizedTest
  @CsvSource({"TREASURY_GOV", "CURRENCYAPI_COM"})
  void testCalculateExchangeBatchUsingCustomJsonExchangeRateProvider(ExchangeRateSource.ProviderType providerType,
                                                                     VertxTestContext testContext) throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(providerType));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(providerType)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    var calculations = new ExchangeRateCalculations()
      .withExchangeRateCalculations(Arrays.asList(
        new ExchangeRateCalculation()
          .withFrom("USD")
          .withTo("EUR")
          .withAmount(100.0)
          .withRate(null),
        new ExchangeRateCalculation()
          .withFrom("GBP")
          .withTo("EUR")
          .withAmount(50.0)
          .withRate(null),
        new ExchangeRateCalculation()
          .withFrom("EUR")
          .withTo("EUR")
          .withAmount(75.0)
          .withRate(null)
      ));

    exchangeService.calculateExchangeBatch(calculations, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertNotNull(result);
        assertNotNull(result.getExchangeRateCalculations());
        assertEquals(3, result.getExchangeRateCalculations().size());

        // Verify each calculation has a result
        result.getExchangeRateCalculations().forEach(calculation -> {
          assertNotNull(calculation.getCalculation());
          assertNotEquals(0.0, calculation.getCalculation());
        });

        testContext.completeNow();
      })));
  }

  @Test
  void testCalculateExchangeBatchWithEmptyCalculations(VertxTestContext testContext) throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(TREASURY_GOV));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(TREASURY_GOV)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    var emptyCalculations = new ExchangeRateCalculations().withExchangeRateCalculations(List.of());

    exchangeService.calculateExchangeBatch(emptyCalculations, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertNotNull(result);
        assertNotNull(result.getExchangeRateCalculations());
        assertEquals(0, result.getExchangeRateCalculations().size());
        testContext.completeNow();
      })));
  }

  @Test
  void testCalculateExchangeBatchWithSingleCalculation(VertxTestContext testContext) throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(TREASURY_GOV));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(TREASURY_GOV)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    var singleCalculation = new ExchangeRateCalculations()
      .withExchangeRateCalculations(List.of(
        new ExchangeRateCalculation()
          .withFrom("USD")
          .withTo("EUR")
          .withAmount(100.0)
          .withRate(null)
      ));

    exchangeService.calculateExchangeBatch(singleCalculation, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertNotNull(result);
        assertNotNull(result.getExchangeRateCalculations());
        assertEquals(1, result.getExchangeRateCalculations().size());

        var calculation = result.getExchangeRateCalculations().getFirst();
        assertEquals("USD", calculation.getFrom());
        assertEquals("EUR", calculation.getTo());
        assertEquals(100.0, calculation.getAmount());
        assertEquals(96.1d, calculation.getCalculation());

        testContext.completeNow();
      })));
  }

  @Test
  void testCalculateExchangeBatchWithCustomRates(VertxTestContext testContext) throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(HttpStatus.HTTP_OK.toInt());
    when(httpResponse.body()).thenReturn(createResponseBody(TREASURY_GOV));
    when(restClient.get(any(), any(), any())).thenReturn(Future.succeededFuture(createExchangeRateSource(TREASURY_GOV)));
    when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(httpResponse);

    var calculationsWithCustomRates = new ExchangeRateCalculations()
      .withExchangeRateCalculations(List.of(
        new ExchangeRateCalculation()
          .withFrom("USD")
          .withTo("EUR")
          .withAmount(100.0)
          .withRate(0.85), // Custom rate
        new ExchangeRateCalculation()
          .withFrom("GBP")
          .withTo("USD")
          .withAmount(50.0)
          .withRate(1.25) // Custom rate
      ));

    exchangeService.calculateExchangeBatch(calculationsWithCustomRates, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertNotNull(result);
        assertNotNull(result.getExchangeRateCalculations());
        assertEquals(2, result.getExchangeRateCalculations().size());

        var firstCalculation = result.getExchangeRateCalculations().getFirst();
        log.info("First rate: {}, calculation: {}", firstCalculation.getRate(), firstCalculation.getCalculation());
        assertNotNull(firstCalculation.getCalculation());
        assertEquals(0.85, firstCalculation.getRate());
        assertEquals(85.0, firstCalculation.getCalculation());

        var secondCalculation = result.getExchangeRateCalculations().getLast();
        log.info("Second rate: {}, calculation: {}", secondCalculation.getRate(), secondCalculation.getCalculation());
        assertNotNull(secondCalculation.getCalculation());
        assertEquals(1.25, secondCalculation.getRate());
        assertEquals(62.5, secondCalculation.getCalculation());

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
