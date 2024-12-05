package org.folio.services.financedata;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.Collections.singletonList;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FundTags;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.fund.FundUpdateLogService;
import org.folio.services.protection.AcqUnitsService;
import org.folio.services.transactions.TransactionApiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class FinanceDataServiceTest {

  @InjectMocks
  private FinanceDataService financeDataService;
  @Mock
  private RestClient restClient;
  @Mock
  private AcqUnitsService acqUnitsService;
  @Mock
  private FundUpdateLogService fundUpdateLogService;
  @Mock
  private TransactionApiService transactionApiService;
  @Mock
  private FiscalYearService fiscalYearService;

  private RequestContext requestContextMock;
  private AutoCloseable closeable;

  @BeforeEach
  void initMocks() {
    closeable = MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put("x-okapi-url", "http://localhost:9130"); // Ensure this URL is correct
    okapiHeaders.put("x-okapi-token", "token");
    okapiHeaders.put("x-okapi-tenant", "tenant");
    okapiHeaders.put("x-okapi-user-id", "userId");
    requestContextMock = new RequestContext(context, okapiHeaders);
  }

  @AfterEach
  void closeMocks() throws Exception {
    closeable.close();
  }

  @Test
  void positive_shouldGetFinanceDataWithAcqUnitsRestriction(VertxTestContext vertxTestContext) {
    String query = "fiscalYearId==db9c1ad6-026e-4b1a-9a99-032f41e7099b";
    String acqUnitIdsQuery = "fundAcqUnitIds=(1ee4b4e5-d621-4e43-8a76-0d904b0f491b) and budgetAcqUnitIds=(1ee4b4e5-d621-4e43-8a76-0d904b0f491b) or (" + NO_ACQ_UNIT_ASSIGNED_CQL + ")";
    String expectedQuery = "(" + acqUnitIdsQuery + ") and (" + query + ")";
    int offset = 0;
    int limit = 10;
    FyFinanceDataCollection fyFinanceDataCollection = new FyFinanceDataCollection();

    when(acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(any())).thenReturn(succeededFuture(acqUnitIdsQuery));
    when(restClient.get(anyString(), eq(FyFinanceDataCollection.class), any())).thenReturn(succeededFuture(fyFinanceDataCollection));

    var future = financeDataService.getFinanceDataWithAcqUnitsRestriction(query, offset, limit, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(FyFinanceDataCollection.class), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void negative_shouldReturnEmptyCollectionWhenFinanceDataNotFound(VertxTestContext vertxTestContext) {
    String query = "fiscalYearId==non-existent-id";
    String noFdUnitAssignedCql = "cql.allRecords=1 not fundAcqUnitIds <> [] and cql.allRecords=1 not budgetAcqUnitIds <> []";
    String expectedQuery = "(" + noFdUnitAssignedCql + ") and (" + query + ")";
    int offset = 0;
    int limit = 10;
    FyFinanceDataCollection emptyCollection = new FyFinanceDataCollection().withTotalRecords(0);

    when(acqUnitsService.buildAcqUnitsCqlClauseForFinanceData(any())).thenReturn(succeededFuture(noFdUnitAssignedCql));
    when(restClient.get(anyString(), eq(FyFinanceDataCollection.class), any())).thenReturn(succeededFuture(emptyCollection));

    var future = financeDataService.getFinanceDataWithAcqUnitsRestriction(query, offset, limit, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        assertEquals(0, (int) result.result().getTotalRecords());
        verify(restClient).get(assertQueryContains(expectedQuery), eq(FyFinanceDataCollection.class), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void positive_testPutFinanceData_PutFinanceDataSuccessfully(VertxTestContext vertxTestContext) {
    FyFinanceDataCollection financeDataCollection = new FyFinanceDataCollection();
    when(restClient.put(anyString(), any(), any())).thenReturn(succeededFuture());
    when(transactionApiService.processBatch(any(), any())).thenReturn(succeededFuture());
    when(fundUpdateLogService.createFundUpdateLog(any(), any())).thenReturn(succeededFuture());

    var future = financeDataService.putFinanceData(financeDataCollection, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        verify(fundUpdateLogService).createFundUpdateLog(argThat(log ->
          log.getStatus() == FundUpdateLog.Status.COMPLETED
        ), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void negative_testPutFinanceData_LogErrorWhenPutFinanceDataFails(VertxTestContext vertxTestContext) {
    FyFinanceDataCollection financeDataCollection = new FyFinanceDataCollection();
    when(restClient.put(anyString(), any(), any())).thenReturn(failedFuture("Error"));
    when(transactionApiService.processBatch(any(), any())).thenReturn(succeededFuture());
    when(fundUpdateLogService.createFundUpdateLog(any(), any())).thenReturn(succeededFuture());

    var future = financeDataService.putFinanceData(financeDataCollection, requestContextMock);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        assertTrue(result.failed());
        verify(fundUpdateLogService).createFundUpdateLog(any(), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void negative_testPutFinanceData_FailureInProcessAllocationTransaction(VertxTestContext vertxTestContext) {
    FyFinanceDataCollection financeData = new FyFinanceDataCollection();
    FyFinanceData data = createValidFyFinanceData();
    financeData.setFyFinanceData(singletonList(data));

    when(transactionApiService.processBatch(any(), any())).thenReturn(failedFuture("Process failed"));

    financeDataService.putFinanceData(financeData, requestContextMock)
      .onComplete(vertxTestContext.failing(error -> {
        verify(fundUpdateLogService).createFundUpdateLog(argThat(log ->
          log.getStatus() == FundUpdateLog.Status.ERROR
        ), eq(requestContextMock));
        vertxTestContext.completeNow();
      }));
  }

  @Test
  void testCreateAllocationTransaction(VertxTestContext vertxTestContext) {
    FyFinanceData data = createValidFyFinanceData();
    FiscalYear fiscalYear = new FiscalYear().withCurrency("USD");

    when(fiscalYearService.getFiscalYearById(any(), any())).thenReturn(succeededFuture(fiscalYear));

    financeDataService.createAllocationTransaction(data, requestContextMock)
      .onComplete(vertxTestContext.succeeding(transaction -> {
        assertEquals(Transaction.TransactionType.ALLOCATION, transaction.getTransactionType());
        assertEquals(data.getFundId(), transaction.getToFundId());
        assertEquals(fiscalYear.getCurrency(), transaction.getCurrency());
        assertEquals(150.0, transaction.getAmount()); // Assuming initial allocation is 100 and change is 50
        vertxTestContext.completeNow();
      }));
  }

  private FyFinanceData createValidFyFinanceData() {
    return new FyFinanceData()
      .withFundId(UUID.randomUUID().toString())
      .withFundCode("FUND-001")
      .withFundName("Test Fund")
      .withFundDescription("Test Fund Description")
      .withFundStatus(FyFinanceData.FundStatus.ACTIVE)
      .withBudgetId(UUID.randomUUID().toString())
      .withBudgetName("Test Budget")
      .withBudgetStatus(FyFinanceData.BudgetStatus.ACTIVE)
      .withBudgetInitialAllocation(100.0)
      .withBudgetAllocationChange(50.0)
      .withBudgetAllowableExpenditure(150.0)
      .withBudgetAllowableEncumbrance(150.0)
      .withTransactionDescription("Test Transaction")
      .withTransactionTag(new FundTags().withTagList(List.of("tag1", "tag2")))
      .withFiscalYearId(UUID.randomUUID().toString());
  }
}
