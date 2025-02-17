package org.folio.services.financedata;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.BUDGET_STATUS_INCORRECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.folio.services.fund.FundService;
import org.folio.services.budget.BudgetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    assertEquals("Budget current allocation is required", exception.getErrors().getErrors().get(1).getMessage());
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
  void positive_validateIds_ValidIds(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData();
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    financeDataValidator.validateIds(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(ar.cause());
        }
      });
  }

  @Test
  void negative_validateIds_MismatchFundId(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData()
      .withBudgetInitialAllocation(null)
      .withBudgetCurrentAllocation(null)
      .withFundId(UUID.randomUUID().toString());
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    financeDataValidator.validateIds(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.failed()) {
          var exception = (HttpException) ar.cause();
          assertEquals("Budget fund ID must be the same as fund ID", exception.getErrors().getErrors().get(0).getMessage());
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(new AssertionError("Expected HttpException to be thrown, but nothing was thrown."));
        }
      });
  }

  @Test
  void negative_validateIds_MismatchLedgerId(VertxTestContext vertxTestContext) {
    var financeData = createValidFyFinanceData()
      .withBudgetInitialAllocation(null)
      .withBudgetCurrentAllocation(null)
      .withLedgerId(UUID.randomUUID().toString());
    var financeDataCollection = new FyFinanceDataCollection()
      .withFyFinanceData(Collections.singletonList(financeData))
      .withUpdateType(FyFinanceDataCollection.UpdateType.PREVIEW);

    when(fundService.getFundById(any(), any())).thenReturn(succeededFuture(createValidFund()));
    when(budgetService.getBudgetById(any(), any())).thenReturn(succeededFuture(createValidBudget()));

    financeDataValidator.validateIds(financeDataCollection, requestContextMock)
      .onComplete(ar -> {
        if (ar.failed()) {
          var exception = (HttpException) ar.cause();
          assertEquals("Fund ledger ID must be the same as ledger ID", exception.getErrors().getErrors().get(0).getMessage());
          vertxTestContext.completeNow();
        } else {
          vertxTestContext.failNow(new AssertionError("Expected HttpException to be thrown, but nothing was thrown."));
        }
      });
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
