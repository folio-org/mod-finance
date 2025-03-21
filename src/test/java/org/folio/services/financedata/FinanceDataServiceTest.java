package org.folio.services.financedata;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.HttpStatus.HTTP_NOT_FOUND;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundTags;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.JobDetails;
import org.folio.rest.jaxrs.model.JobNumber;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.services.budget.BudgetService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.fund.FundService;
import org.folio.services.fund.FundUpdateLogService;
import org.folio.services.ledger.LedgerService;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@ExtendWith(VertxExtension.class)
public class FinanceDataServiceTest {

  private static final String FISCAL_YEAR_ID = UUID.randomUUID().toString();
  private static final String FUND_ID = UUID.randomUUID().toString();
  private static final String BUDGET_ID = UUID.randomUUID().toString();
  private static final String LEDGER_ID = UUID.randomUUID().toString();

  @Mock private RestClient restClient;
  @Mock private LedgerService ledgerService;
  @Mock private AcqUnitsService acqUnitsService;
  @Mock private FundUpdateLogService fundUpdateLogService;
  @Mock private FiscalYearService fiscalYearService;
  @Mock private FundService fundService;
  @Mock private BudgetService budgetService;
  @Mock private FinanceDataValidator financeDataValidator;
  @InjectMocks private FinanceDataService financeDataService;

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
    var fyFinanceDataCollection = new FyFinanceDataCollection();

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
    var emptyCollection = new FyFinanceDataCollection().withTotalRecords(0);

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
    var financeData = createValidFyFinanceData();
    var financeData2 = createValidFyFinanceData() // set to null non-required fields
      .withBudgetId(null).withBudgetAllocationChange(null).withFundDescription(null)
      .withTransactionTag(null).withTransactionDescription(null);
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeData, financeData2))
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT);
    var fiscalYear = new FiscalYear().withCurrency("USD");
    var ledger = new Ledger()
      .withId(LEDGER_ID)
      .withAcqUnitIds(List.of());

    when(ledgerService.retrieveLedgerById(eq(LEDGER_ID), any())).thenReturn(succeededFuture(ledger));
    when(restClient.put(anyString(), any(), any(), any())).thenReturn(succeededFuture(financeDataCollection));
    when(fundUpdateLogService.createFundUpdateLog(any(), any())).thenReturn(succeededFuture());
    when(fundUpdateLogService.getFundUpdateLogById(any(), any())).thenReturn(succeededFuture(new FundUpdateLog()));
    when(fundUpdateLogService.getJobNumber(any())).thenReturn(succeededFuture(new JobNumber().withSequenceNumber("1")));
    when(fundUpdateLogService.updateFundUpdateLog(any(), any())).thenReturn(succeededFuture());
    when(fiscalYearService.getFiscalYearById(any(), any())).thenReturn(succeededFuture(fiscalYear));
    when(financeDataValidator.compareWithExistingData(any(), any())).thenReturn(succeededFuture());

    var future = financeDataService.putFinanceData(financeDataCollection, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        verify(fundUpdateLogService).createFundUpdateLog(argThat(log ->
          log.getStatus() == FundUpdateLog.Status.IN_PROGRESS
        ), eq(requestContextMock));
        verify(fundUpdateLogService).updateFundUpdateLog(argThat(log ->
          log.getStatus() == FundUpdateLog.Status.COMPLETED
        ), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void negative_testPutFinanceData_LedgerNotFound(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData();
    var financeData2 = createValidFyFinanceData() // set to null non-required fields
      .withBudgetId(null).withBudgetAllocationChange(null).withFundDescription(null)
      .withTransactionTag(null).withTransactionDescription(null);
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeData, financeData2))
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT);
    var fiscalYear = new FiscalYear().withCurrency("USD");

    when(ledgerService.retrieveLedgerById(any(), any())).thenReturn(failedFuture(new HttpException(HTTP_NOT_FOUND.toInt(), "Not found")));
    when(restClient.put(anyString(), any(), any(), any())).thenReturn(succeededFuture(financeDataCollection));
    when(fundUpdateLogService.getFundUpdateLogById(any(), any())).thenReturn(succeededFuture(new FundUpdateLog()));
    when(fundUpdateLogService.getJobNumber(any())).thenReturn(succeededFuture(new JobNumber().withSequenceNumber("1")));
    when(fundUpdateLogService.updateFundUpdateLog(any(), any())).thenReturn(succeededFuture());
    when(fiscalYearService.getFiscalYearById(any(), any())).thenReturn(succeededFuture(fiscalYear));
    when(financeDataValidator.compareWithExistingData(any(), any())).thenReturn(succeededFuture());

    financeDataService.putFinanceData(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.failed()) {
          var exception = (HttpException) ar.cause();
          assertEquals("Not found", exception.getErrors().getErrors().getFirst().getMessage());
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(new AssertionError("Expected HttpException to be thrown, but nothing was thrown."));
        }
      });
  }

  @Test
  void negative_testPutFinanceData_LogErrorWhenPutFinanceDataFails(VertxTestContext vertxTestContext) {
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(createValidFyFinanceData()))
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT);
    var fiscalYear = new FiscalYear().withCurrency("USD");
    var ledger = new Ledger()
      .withId(LEDGER_ID)
      .withAcqUnitIds(List.of());

    when(ledgerService.retrieveLedgerById(eq(LEDGER_ID), any())).thenReturn(succeededFuture(ledger));
    when(fiscalYearService.getFiscalYearById(any(), any())).thenReturn(succeededFuture(fiscalYear));
    when(restClient.put(anyString(), any(), any(), any())).thenReturn(failedFuture("Error"));
    when(fundUpdateLogService.createFundUpdateLog(any(), any())).thenReturn(succeededFuture());
    when(fundUpdateLogService.getFundUpdateLogById(any(), any())).thenReturn(succeededFuture(new FundUpdateLog()));
    when(fundUpdateLogService.getJobNumber(any())).thenReturn(succeededFuture(new JobNumber().withSequenceNumber("1")));
    when(fundUpdateLogService.updateFundUpdateLog(any(), any())).thenReturn(succeededFuture());
    when(financeDataValidator.compareWithExistingData(any(), any())).thenReturn(succeededFuture());

    var future = financeDataService.putFinanceData(financeDataCollection, requestContextMock);
    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        assertTrue(result.failed());
        verify(fundUpdateLogService).createFundUpdateLog(any(), any());
        verify(fundUpdateLogService).updateFundUpdateLog(argThat(log ->
          log.getStatus() == FundUpdateLog.Status.ERROR
        ), any());
        vertxTestContext.completeNow();
      });
  }

  private static Stream<Arguments> provideTestParameters() {
    return Stream.of(
      arguments("Test Worksheet", "Test Worksheet"),
      arguments("FY2024LedgerACode (4).csv-log.csv", "FY2024LedgerACode (4)-log.csv"), // test to remove extra .csv
      arguments("FY2024LedgerACode (4).csv-2.csv-log.csv", "FY2024LedgerACode (4)-2-log.csv"),
      arguments("FY2024Fund1.csv", "FY2024Fund1.csv"),
      arguments(null, "FY2024-LEDGER-001") // set worksheetName to null to test FY code + Ledger code + date format
    );
  }

  @SneakyThrows
  @ParameterizedTest
  @SuppressWarnings("unchecked")
  @MethodSource("provideTestParameters")
  void positive_testPutFinanceData_processLogs(String worksheetName, String expectedJobName, VertxTestContext vertxTestContext) {
    var fundUpdateLogId = UUID.randomUUID().toString();
    var financeData = createValidFyFinanceData();
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeData))
      .withWorksheetName(worksheetName);
    var jobNumber = new JobNumber().withSequenceNumber("1");
    var expectedLog = new FundUpdateLog()
      .withId(fundUpdateLogId)
      .withJobName(expectedJobName)
      .withStatus(FundUpdateLog.Status.IN_PROGRESS)
      .withRecordsCount(financeDataCollection.getTotalRecords())
      .withJobDetails(new JobDetails().withAdditionalProperty("fyFinanceData", financeDataCollection.getFyFinanceData()))
      .withJobNumber(1);
    var ledger = new Ledger()
      .withId(LEDGER_ID)
      .withAcqUnitIds(List.of());

    doReturn(succeededFuture(ledger)).when(ledgerService).retrieveLedgerById(eq(LEDGER_ID), any());
    doReturn(succeededFuture(jobNumber)).when(fundUpdateLogService).getJobNumber(any());
    doReturn(succeededFuture(expectedLog)).when(fundUpdateLogService).createFundUpdateLog(any(), any());

    var processLogsMethod = FinanceDataService.class.getDeclaredMethod("processLogs", String.class, FyFinanceDataCollection.class, RequestContext.class);
    processLogsMethod.setAccessible(true);
    var future = (Future<FundUpdateLog>) processLogsMethod.invoke(financeDataService, fundUpdateLogId, financeDataCollection, requestContextMock);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        assertEquals(expectedLog, result.result());
        verify(fundUpdateLogService).getJobNumber(any());
        verify(fundUpdateLogService).createFundUpdateLog(argThat(log ->
          log.getId().equals(fundUpdateLogId) &&
            log.getJobName().contains(expectedLog.getJobName()) &&
            log.getStatus() == FundUpdateLog.Status.IN_PROGRESS &&
            Objects.equals(log.getRecordsCount(), financeDataCollection.getTotalRecords()) &&
            log.getJobDetails().getAdditionalProperties().get("fyFinanceData").equals(financeDataCollection.getFyFinanceData()) &&
            log.getJobNumber() == 1
        ), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void positive_testUpdateLogs_updateLogStatusAndDetails(VertxTestContext vertxTestContext)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    String fundUpdateLogId = UUID.randomUUID().toString();
    var status = FundUpdateLog.Status.COMPLETED;

    var financeData = createValidFyFinanceData();
    var updateFdCollection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeData));

    var existingLog = new FundUpdateLog()
      .withId(fundUpdateLogId)
      .withJobName("Test")
      .withStatus(FundUpdateLog.Status.IN_PROGRESS)
      .withRecordsCount(10)
      .withJobNumber(1);

    var expectedUpdatedLog = new FundUpdateLog()
      .withId(fundUpdateLogId)
      .withJobName("Test")
      .withStatus(FundUpdateLog.Status.COMPLETED)
      .withRecordsCount(10)
      .withJobDetails(new JobDetails().withAdditionalProperty("fyFinanceData", updateFdCollection.getFyFinanceData()))
      .withJobNumber(1);

    when(fundUpdateLogService.getFundUpdateLogById(eq(fundUpdateLogId), eq(requestContextMock)))
      .thenReturn(succeededFuture(existingLog));
    when(fundUpdateLogService.updateFundUpdateLog(eq(expectedUpdatedLog), eq(requestContextMock)))
      .thenReturn(succeededFuture());

    var updateLogsMethod = FinanceDataService.class.getDeclaredMethod("updateLogs", String.class, FundUpdateLog.Status.class,
      FyFinanceDataCollection.class, RequestContext.class);
    updateLogsMethod.setAccessible(true);
    updateLogsMethod.invoke(financeDataService, fundUpdateLogId, status, updateFdCollection, requestContextMock);

    verify(fundUpdateLogService).getFundUpdateLogById(eq(fundUpdateLogId), eq(requestContextMock));
    verify(fundUpdateLogService).updateFundUpdateLog(argThat(log ->
      log.getId().equals(fundUpdateLogId) &&
        log.getStatus() == FundUpdateLog.Status.COMPLETED &&
        log.getJobDetails().getAdditionalProperties().get("fyFinanceData").equals(updateFdCollection.getFyFinanceData())
    ), eq(requestContextMock));

    vertxTestContext.completeNow();
  }

  @Test
  void positive_testPutFinanceData_PreviewMode(VertxTestContext vertxTestContext) {
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(createValidFyFinanceData()))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(financeDataValidator.compareWithExistingData(any(), any())).thenReturn(succeededFuture());

    var future = financeDataService.putFinanceData(financeDataCollection, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        assertEquals(financeDataCollection, result.result());
        result.result().getFyFinanceData().forEach(financeData ->
          assertEquals(
            financeData.getBudgetCurrentAllocation() + financeData.getBudgetAllocationChange(),
            financeData.getBudgetAfterAllocation()));
        verify(restClient, never()).put(anyString(), any(), any());
        verify(fundUpdateLogService, never()).createFundUpdateLog(any(), any());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void positive_testPutFinanceData_PreviewModeWithEmptyData(VertxTestContext vertxTestContext) {
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.emptyList())
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    var future = financeDataService.putFinanceData(financeDataCollection, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());
        assertEquals(financeDataCollection, result.result());
        verify(restClient, never()).put(anyString(), any(), any());
        verify(fundUpdateLogService, never()).createFundUpdateLog(any(), any());
        vertxTestContext.completeNow();
      });
  }

  @Test
  void negative_testPutFinanceData_NotFoundFundId(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData().withFundId(null);
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    doReturn(failedFuture(new HttpException(400, "Fund ID is required"))).when(financeDataValidator).compareWithExistingData(any(), any());

    financeDataService.putFinanceData(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.failed()) {
          var exception = (HttpException) ar.cause();
          assertEquals("Fund ID is required", exception.getErrors().getErrors().getFirst().getMessage());
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(new AssertionError("Expected HttpException to be thrown, but nothing was thrown."));
        }
      });
  }

  @Test
  void negative_testPutFinanceData_NotFoundLedgerId(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData().withLedgerId(null);
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    doReturn(failedFuture(new HttpException(400, "Ledger ID is required"))).when(financeDataValidator).compareWithExistingData(any(), any());

    financeDataService.putFinanceData(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.failed()) {
          var exception = (HttpException) ar.cause();
          assertEquals("Ledger ID is required", exception.getErrors().getErrors().getFirst().getMessage());
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(new AssertionError("Expected HttpException to be thrown, but nothing was thrown."));
        }
      });
  }

  @Test
  void negative_testPutFinanceData_InvalidFiscalYearId() {
    var financeData = createValidFyFinanceData().withFiscalYearId("invalid-fiscal-year-id");
    var collection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT)
      .withTotalRecords(1);

    doThrow(new HttpException(422, "Invalid fiscal year ID")).when(financeDataValidator).validateFinanceDataCollection(any(), anyString());

    var exception = assertThrows(HttpException.class,
      () -> financeDataValidator.validateFinanceDataCollection(collection, FISCAL_YEAR_ID));
    assertEquals("Invalid fiscal year ID", exception.getErrors().getErrors().getFirst().getMessage());
  }

  private Fund createValidFund() {
    return new Fund()
      .withId(FUND_ID)
      .withLedgerId(LEDGER_ID);
  }

  private SharedBudget createValidBudget() {
    return new SharedBudget()
      .withId(BUDGET_ID)
      .withFundId(FUND_ID);
  }

  private FyFinanceData createValidFyFinanceData() {
    return new FyFinanceData()
      .withFundId(FUND_ID)
      .withFundCode("FUND-001")
      .withFundName("Test Fund")
      .withFundDescription("Test Fund Description")
      .withFundStatus(Fund.FundStatus.ACTIVE.value())
      .withBudgetId(BUDGET_ID)
      .withBudgetName("Test Budget")
      .withBudgetStatus("Active")
      .withBudgetInitialAllocation(25.0)
      .withBudgetCurrentAllocation(100.0)
      .withBudgetAllocationChange(50.0)
      .withBudgetAllowableExpenditure(150.0)
      .withBudgetAllowableEncumbrance(150.0)
      .withTransactionDescription("Test Transaction")
      .withTransactionTag(new FundTags().withTagList(List.of("tag1", "tag2")))
      .withFiscalYearId(FISCAL_YEAR_ID)
      .withFiscalYearCode("FY2024")
      .withLedgerId(LEDGER_ID)
      .withLedgerCode("LEDGER-001");
  }
}
