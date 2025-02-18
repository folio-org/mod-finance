package org.folio.services.financedata;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.BUDGET_STATUS_INCORRECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundTags;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.services.fund.FundService;
import org.folio.services.budget.BudgetService;
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
public class FinanceDataValidatorTest {
  private static final String FISCAL_YEAR_ID = UUID.randomUUID().toString();
  private static final String FUND_ID = UUID.randomUUID().toString();
  private static final String BUDGET_ID = UUID.randomUUID().toString();
  private static final String LEDGER_ID = UUID.randomUUID().toString();

  @InjectMocks
  private FinanceDataValidator financeDataValidator;
  @Mock
  private FundService fundService;
  @Mock
  private BudgetService budgetService;

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
  void positive_testPutFinanceData_ValidIds(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData();
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    financeDataValidator.validateFinanceDataCollection(financeDataCollection, FISCAL_YEAR_ID);
    vertxTestContext.completeNow();
  }

  @Test
  void negative_validateFinanceDataCollection_DuplicateFinanceData() {
    var financeData = createValidFyFinanceData();
    var duplicateFinanceData = createValidFyFinanceData();
    var collection = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeData, duplicateFinanceData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT)
      .withTotalRecords(2);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    var exception = assertThrows(HttpException.class,
      () -> financeDataValidator.validateFinanceDataCollection(collection, FISCAL_YEAR_ID));
    assertEquals("Finance data collection contains duplicate fund, budget and fiscal year IDs", exception.getErrors().getErrors().get(0).getMessage());
    assertEquals("financeData", exception.getErrors().getErrors().get(0).getParameters().get(0).getKey());
    assertEquals("duplicate", exception.getErrors().getErrors().get(0).getParameters().get(0).getValue());
  }

  @Test
  void negative_validateFinanceDataCollection_InvalidAllocationChange() {
    var financeData = createValidFyFinanceData();
    financeData.setBudgetCurrentAllocation(100.0);
    financeData.setBudgetAllocationChange(-150.0);
    var collection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT)
      .withTotalRecords(1);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    var exception = assertThrows(HttpException.class,
      () -> financeDataValidator.validateFinanceDataCollection(collection, FISCAL_YEAR_ID));
    assertEquals("Allocation change cannot be greater than current allocation", exception.getErrors().getErrors().get(0).getMessage());
  }

  @Test
  void negative_validateFinanceDataCollection_MissingRequiredField() {
    var financeData = createValidFyFinanceData();
    financeData.setBudgetInitialAllocation(null);
    var collection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT)
      .withTotalRecords(1);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    var exception = assertThrows(HttpException.class,
      () -> financeDataValidator.validateFinanceDataCollection(collection, FISCAL_YEAR_ID));
    assertEquals("Budget initial allocation is required", exception.getErrors().getErrors().get(0).getMessage());
  }

  @Test
  void negative_validateFinanceDataCollection_PreviewMode_MissingRequiredFields(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData()
      .withBudgetInitialAllocation(null)
      .withBudgetCurrentAllocation(null);
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    var exception = assertThrows(HttpException.class,
      () -> financeDataValidator.validateFinanceDataCollection(financeDataCollection, FISCAL_YEAR_ID));
    assertEquals("Budget initial allocation is required", exception.getErrors().getErrors().get(0).getMessage());
    vertxTestContext.completeNow();
  }

  @Test
  void negative_validateFinanceDataCollection_InvalidBudgetStatus() {
    var financeData = createValidFyFinanceData();
    financeData.setBudgetStatus("InvalidStatus");
    var collection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.COMMIT)
      .withTotalRecords(1);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    var exception = assertThrows(HttpException.class,
      () -> financeDataValidator.validateFinanceDataCollection(collection, FISCAL_YEAR_ID));
    assertEquals("Budget status is incorrect", exception.getErrors().getErrors().get(0).getMessage());
    assertEquals(BUDGET_STATUS_INCORRECT.getCode(), exception.getErrors().getErrors().get(0).getCode());
  }

  @Test
  void positive_compareWithExistingData_ValidIds(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData();
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    financeDataValidator.compareWithExistingData(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(ar.cause());
        }
      });
  }

  @ParameterizedTest
  @MethodSource("provideFinanceDataCollections")
  void compareWithExistingData_checkIsChanged(FyFinanceDataCollection financeDataCollection, boolean expectedFundChanged, boolean expectedBudgetChanged, VertxTestContext vertxTestContext) {
    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    financeDataValidator.compareWithExistingData(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          var financeData = financeDataCollection.getFyFinanceData().get(0);
          assertEquals(expectedFundChanged, financeData.getIsFundChanged());
          assertEquals(expectedBudgetChanged, financeData.getIsBudgetChanged());
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(ar.cause());
        }
      });
  }

  private static Stream<Arguments> provideFinanceDataCollections() {
    FinanceDataValidatorTest testInstance = new FinanceDataValidatorTest();

    var financeData = testInstance.createValidFyFinanceData()
      .withFundDescription("New fund description");
    var financeDataWithNullBudgetId = testInstance.createValidFyFinanceData()
      .withFundDescription("New fund description")
      .withBudgetId(null).withBudgetAllocationChange(null).withBudgetStatus(null);
    var financeDataWithBudgedCreation = testInstance.createValidFyFinanceData()
      .withBudgetId(null).withBudgetStatus("Active");
    var financeDataWithBudgetAllocationChange = testInstance.createValidFyFinanceData().withBudgetAllocationChange(50.0);
    var financeDataWithDifferentBudgetStatus = testInstance.createValidFyFinanceData().withBudgetStatus("Frozen");

    var collection1 = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    var collection2 = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeDataWithNullBudgetId))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    var collection3 = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeDataWithBudgedCreation))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    var collection4 = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeDataWithBudgetAllocationChange))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    var collection5 = new FyFinanceDataCollection()
      .withFyFinanceData(List.of(financeDataWithDifferentBudgetStatus))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    return Stream.of(
      arguments(collection1, true, true),
      arguments(collection2, true, false),
      arguments(collection3, false, true),
      arguments(collection4, false, true),
      arguments(collection5, false, true)
    );
  }

  @Test
  void negative_compareWithExistingData_MismatchFundDetails(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData()
      .withBudgetInitialAllocation(null)
      .withBudgetCurrentAllocation(null)
      .withFundId(UUID.randomUUID().toString())
      .withFundName("Updated name")
      .withFundCode("FUND-UPDATED")
      .withLedgerId(UUID.randomUUID().toString())
      .withBudgetName("Updated budget");
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    financeDataValidator.compareWithExistingData(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.failed()) {
          var exception = (HttpException) ar.cause();
          assertTrue(exception.getErrors().getErrors().stream()
            .anyMatch(error -> "Budget fund ID must be the same as fund ID".equals(error.getMessage())));
          assertTrue(exception.getErrors().getErrors().stream()
            .anyMatch(error -> "budgetName must be the same as existing budget name".equals(error.getMessage())));
          assertTrue(exception.getErrors().getErrors().stream()
            .anyMatch(error -> "fundCode must be the same as existing fund code".equals(error.getMessage())));
          assertTrue(exception.getErrors().getErrors().stream()
            .anyMatch(error -> "fundName must be the same as existing fund name".equals(error.getMessage())));
          assertTrue(exception.getErrors().getErrors().stream()
            .anyMatch(error -> "Fund ledger ID must be the same as ledger ID".equals(error.getMessage())));
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(new AssertionError("Expected HttpException to be thrown, but nothing was thrown."));
        }
      });
  }

  private Fund createValidFund() {
    return new Fund()
      .withId(FUND_ID)
      .withLedgerId(LEDGER_ID)
      .withFundStatus(Fund.FundStatus.ACTIVE)
      .withCode("FUND-001")
      .withName("Test Fund")
      .withDescription("Test Fund Description");
  }

  private SharedBudget createValidBudget() {
    return new SharedBudget()
      .withId(BUDGET_ID)
      .withName("Test Budget")
      .withFundId(FUND_ID)
      .withBudgetStatus(SharedBudget.BudgetStatus.ACTIVE)
      .withAllowableExpenditure(150.0)
      .withAllowableEncumbrance(150.0);
  }

  private FyFinanceData createValidFyFinanceData() {
    return new FyFinanceData()
      .withFundId(FUND_ID)
      .withFundCode("FUND-001")
      .withFundName("Test Fund")
      .withFundDescription("Test Fund Description")
      .withFundStatus(FyFinanceData.FundStatus.ACTIVE)
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
      .withLedgerId(LEDGER_ID);
  }
}
