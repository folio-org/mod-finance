package org.folio.services.fund;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestConstants.NOT_FOUND;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.ErrorCodes.FUND_NOT_FOUND_ERROR;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class FundServiceTest {
  private RequestContext requestContext;
  @InjectMocks
  private FundService fundService;
  @Mock
  private RestClient restClient;
  @Mock
  private AcqUnitsService acqUnitsService;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(context, okapiHeaders);
  }

  @Test
  void testShouldRetrieveFundById(VertxTestContext vertxTestContext) {
    // Given
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    Fund fund = new Fund().withId(fundId)
      .withLedgerId(ledgerId);

    doReturn(succeededFuture(fund)).when(restClient)
      .get(anyString(), eq(Fund.class), eq(requestContext));
    // When
    var future = fundService.getFundById(fundId, requestContext);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        var actFund = result.result();
        assertThat(actFund, equalTo(fund));
        verify(restClient).get(assertQueryContains(fundId), eq(Fund.class), eq(requestContext));

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldThrowHttpExceptionAsCauseIfFundNotFound(VertxTestContext vertxTestContext) {
    // Given
    String fundId = UUID.randomUUID()
      .toString();

    Error expError = new Error().withCode(FUND_NOT_FOUND_ERROR.getCode())
      .withMessage(String.format(FUND_NOT_FOUND_ERROR.getDescription(), fundId));
    doReturn(Future.failedFuture(new HttpException(NOT_FOUND, expError))).when(restClient)
      .get(anyString(), eq(Fund.class), eq(requestContext));

    var future = fundService.getFundById(fundId, requestContext);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        HttpException actHttpException = (HttpException) result.cause();
        Error actError = actHttpException.getErrors().getErrors().get(0);
        assertEquals(actError.getCode(), expError.getCode());
        assertEquals(actError.getMessage(), String.format(FUND_NOT_FOUND_ERROR.getDescription(), fundId));
        assertEquals(NOT_FOUND, actHttpException.getCode());
        // Then
        verify(restClient).get(assertQueryContains(fundId), eq(Fund.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldThrowNotHttpExceptionIfFundNotFound(VertxTestContext vertxTestContext) {
    // Given
    String fundId = UUID.randomUUID().toString();
    doReturn(Future.failedFuture(new RuntimeException())).when(restClient)
      .get(anyString(), eq(Fund.class), eq(requestContext));
    var future = fundService.getFundById(fundId, requestContext);
    // When
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        assertThat(result.cause(), isA(RuntimeException.class));

        verify(restClient).get(assertQueryContains(fundId), eq(Fund.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldRetrieveFundsWithAcqUnits(VertxTestContext vertxTestContext) {
    // Given
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    Fund fund = new Fund().withId(fundId)
      .withLedgerId(ledgerId);
    FundsCollection fundsCollection = new FundsCollection().withFunds(List.of(fund))
      .withTotalRecords(1);
    doReturn(succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL)).when(acqUnitsService)
      .buildAcqUnitsCqlClause(requestContext);
    doReturn(succeededFuture(fundsCollection)).when(restClient)
      .get(anyString(), eq(FundsCollection.class), eq(requestContext));
    // When
    var future = fundService.getFundsWithAcqUnitsRestriction(StringUtils.EMPTY, 0, 10, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        var actFunds = result.result();
        assertThat(fundsCollection, equalTo(actFunds));
        verify(restClient).get(assertQueryContains(NO_ACQ_UNIT_ASSIGNED_CQL), eq(FundsCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testShouldRetrieveFundsWithoutAcqUnits(VertxTestContext vertxTestContext) {
    // Given
    String ledgerId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    Fund fund = new Fund().withId(fundId)
      .withLedgerId(ledgerId);
    FundsCollection fundsCollection = new FundsCollection().withFunds(List.of(fund))
      .withTotalRecords(1);
    when(restClient.get(anyString(), eq(FundsCollection.class), eq(requestContext))).thenReturn(succeededFuture(fundsCollection));
    // When
    var future = fundService.getFundsWithoutAcqUnitsRestriction("test_query", 0, 10, (requestContext));
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var actFunds = result.result();
        assertThat(fundsCollection, equalTo(actFunds));
        verify(restClient).get(assertQueryContains("test_query"), eq(FundsCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetFundsByIds(VertxTestContext vertxTestContext) {
    // Given
    FundsCollection fundsCollection = new FundsCollection();
    Fund fund1 = new Fund().withId("5");
    Fund fund2 = new Fund().withId("7");
    List<String> ids = Arrays.asList("5", "7");
    List<Fund> fundsList = new ArrayList<>();
    fundsList.add(fund1);
    fundsList.add(fund2);
    fundsCollection.setFunds(fundsList);
    // When
    when(restClient.get(anyString(), eq(FundsCollection.class), any())).thenReturn(succeededFuture(fundsCollection));
    var future = fundService.getFundsByIds(ids, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var funds = result.result();
        assertEquals(fund1.getId(), fundsCollection.getFunds().get(0).getId());
        assertEquals(fund2.getId(), fundsCollection.getFunds().get(1).getId());
        assertEquals(funds.get(0).getId(),fundsCollection.getFunds().get(0).getId());
        assertEquals(funds.get(1).getId(),fundsCollection.getFunds().get(1).getId());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetFundsByIdsTwo(VertxTestContext vertxTestContext) {
    // Given
    FundsCollection fundsCollection = new FundsCollection();
    Fund fund1 = new Fund().withId("5");
    Fund fund2 = new Fund().withId("7");
    List<String> ids = Arrays.asList("5", "7");
    List<Fund> fundsList = new ArrayList<>();
    fundsList.add(fund1);
    fundsList.add(fund2);
    fundsCollection.setFunds(fundsList);
    // When
    when(restClient.get(anyString(), eq(FundsCollection.class), eq(requestContext))).thenReturn(succeededFuture(fundsCollection));
    var future = fundService.getFundsByIds(ids, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var funds = result.result();
        assertEquals(fund1.getId(), fundsCollection.getFunds().get(0).getId());
        assertEquals(fund2.getId(), fundsCollection.getFunds().get(1).getId());
        assertEquals(funds.get(0).getId(), fundsCollection.getFunds().get(0).getId());
        assertEquals(funds.get(1).getId(), fundsCollection.getFunds().get(1).getId());
        vertxTestContext.completeNow();
      });

  }

  @Test
  void testGetFunds(VertxTestContext vertxTestContext) {
    // Given
    FundsCollection fundsCollection = new FundsCollection();
    Fund fund1 = new Fund().withId("5");
    Fund fund2 = new Fund().withId("7");
    List<String> ids = Arrays.asList("5", "7");
    List<Fund> fundsList = new ArrayList<>();
    fundsList.add(fund1);
    fundsList.add(fund2);
    fundsCollection.setFunds(fundsList);
    // When
    when(restClient.get(anyString(), eq(FundsCollection.class), any())).thenReturn(succeededFuture(fundsCollection));
    var future = fundService.getFundsByIds(ids, requestContext);
    // Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var funds = result.result();
        assertEquals(fundsCollection.getFunds().get(0).getId(), funds.get(0).getId());
        assertEquals(fundsCollection.getFunds().get(1).getId(),funds.get(1).getId());
        vertxTestContext.completeNow();
      });
  }
}
