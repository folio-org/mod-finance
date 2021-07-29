package org.folio.services.fund;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundsCollection;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.NOT_FOUND;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.ErrorCodes.FUND_NOT_FOUND_ERROR;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FundServiceTest {
  private RequestContext requestContext;

  @InjectMocks
  private FundService fundService;
  @Mock
  private RestClient fundStorageRestClient;
  @Mock
  private AcqUnitsService acqUnitsService;

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
    Fund actFund = fundService.retrieveFundById(fundId, requestContext).join();
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

  @Test
  void testShouldRetrieveFundsWithAcqUnits() {
    //Given
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FundsCollection fundsCollection = new FundsCollection().withFunds(List.of(fund)).withTotalRecords(1);
    doReturn(completedFuture(NO_ACQ_UNIT_ASSIGNED_CQL)).when(acqUnitsService).buildAcqUnitsCqlClause(requestContext);
    doReturn(completedFuture(fundsCollection)).when(fundStorageRestClient).get(NO_ACQ_UNIT_ASSIGNED_CQL, 0, 10, requestContext, FundsCollection.class);
    //When
    FundsCollection actFunds = fundService.getFundsWithAcqUnitsRestriction(StringUtils.EMPTY, 0,10, requestContext).join();
    //Then
    assertThat(fundsCollection, equalTo(actFunds));
    verify(fundStorageRestClient).get(NO_ACQ_UNIT_ASSIGNED_CQL, 0, 10, requestContext, FundsCollection.class);
  }

  @Test
  void testShouldRetrieveFundsWithoutAcqUnits() {
    //Given
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    Fund fund = new Fund().withId(fundId).withLedgerId(ledgerId);
    FundsCollection fundsCollection = new FundsCollection().withFunds(List.of(fund)).withTotalRecords(1);
    doReturn(completedFuture(fundsCollection)).when(fundStorageRestClient).get("test_query", 0, 10, requestContext, FundsCollection.class);
    //When
    FundsCollection actFunds = fundService.getFundsWithoutAcqUnitsRestriction("test_query", 0,10 , requestContext).join();
    //Then
    assertThat(fundsCollection, equalTo(actFunds));
    verify(fundStorageRestClient).get("test_query", 0, 10, requestContext, FundsCollection.class);
  }

  @Test
  void testGetFundsByIds() {
    FundsCollection fundsCollection = new FundsCollection();
    Fund fund1 = new Fund().withId("6");
    Fund fund2 = new Fund().withId("7");
    List<Fund> fundsList = new ArrayList<>();
    fundsList.add(fund1);
    fundsList.add(fund2);
    fundsCollection.setFunds(fundsList);
    when(fundStorageRestClient.get(any(), any(), eq(FundsCollection.class))).thenReturn(CompletableFuture.completedFuture(fundsCollection));
    assertEquals(fund1.getId(), fundsCollection.getFunds().get(0).getId());
    assertEquals(fund2.getId(), fundsCollection.getFunds().get(1).getId());
  }
}
