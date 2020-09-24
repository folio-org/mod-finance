package org.folio.services.fund;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.NOT_FOUND;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.ErrorCodes.FUND_NOT_FOUND_ERROR;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Fund;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class FundServiceTest {
  private RequestContext requestContext;

  @InjectMocks
  private FundService fundService;
  @Mock
  private RestClient fundStorageRestClient;


  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @Test
  void testShouldRetrieveFundById() {
    //Given
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);

    doReturn(completedFuture(fund)).when(fundStorageRestClient).getById(fundId, requestContext, Fund.class);
    //When
    Fund actFund = fundService.retrieveFundById(fundId,requestContext).join();
    //Then
    assertThat(actFund, equalTo(fund));
    verify(fundStorageRestClient).getById(fundId, requestContext, Fund.class);
  }

  @Test
  void testShouldThrowHttpExceptionAsCauseIfFundNotFound() {
    //Given
    String fundId = UUID.randomUUID().toString();

    Error expError = new Error().withCode(FUND_NOT_FOUND_ERROR.getCode()).withMessage(String.format(FUND_NOT_FOUND_ERROR.getDescription(), fundId));
    doThrow(new CompletionException(new HttpException(NOT_FOUND, expError))).when(fundStorageRestClient).getById(fundId, requestContext, Fund.class);
    //When
    CompletionException thrown = assertThrows(
      CompletionException.class,
      () -> fundService.retrieveFundById(fundId,requestContext).join(),      "Expected exception"
    );
    HttpException actHttpException = (HttpException)thrown.getCause();
    Error actError = actHttpException.getErrors().getErrors().get(0);
    assertEquals(actError.getCode(), expError.getCode());
    assertEquals(actError.getMessage(), String.format(FUND_NOT_FOUND_ERROR.getDescription(), fundId));
    assertEquals(NOT_FOUND, actHttpException.getCode());
    //Then
    verify(fundStorageRestClient).getById(fundId, requestContext, Fund.class);
  }

  @Test
  void testShouldThrowNotHttpExceptionIfFundNotFound() {
    //Given
    String fundId = UUID.randomUUID().toString();
    doThrow(new CompletionException(new RuntimeException())).when(fundStorageRestClient).getById(fundId, requestContext, Fund.class);
    //When
    CompletionException thrown = assertThrows(
      CompletionException.class,
      () -> fundService.retrieveFundById(fundId,requestContext).join(),"Expected exception"
    );
    assertEquals(RuntimeException.class, thrown.getCause().getClass());
    //Then
    verify(fundStorageRestClient).getById(fundId, requestContext, Fund.class);
  }
}
