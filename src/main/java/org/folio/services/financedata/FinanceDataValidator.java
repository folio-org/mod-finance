package org.folio.services.financedata;

import static org.folio.rest.util.ErrorCodes.BUDGET_STATUS_INCORRECT;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.services.budget.BudgetService;
import org.folio.services.fund.FundService;

@Log4j2
public class FinanceDataValidator {

  private final FundService fundService;
  private final BudgetService budgetService;

  public FinanceDataValidator(FundService fundService, BudgetService budgetService) {
    this.fundService = fundService;
    this.budgetService = budgetService;
  }

  public void validateFinanceDataCollection(FyFinanceDataCollection financeDataCollection, String fiscalYearId) {
    validateForDuplication(financeDataCollection);
    for (int i = 0; i < financeDataCollection.getFyFinanceData().size(); i++) {
      var financeData = financeDataCollection.getFyFinanceData().get(i);
      validateFinanceDataFields(financeData, i, fiscalYearId);

      var allocationChange = financeData.getBudgetAllocationChange();
      var currentAllocation = financeData.getBudgetCurrentAllocation();

      if (allocationChange < 0 && Math.abs(allocationChange) > currentAllocation) {
        var error = createError("Allocation change cannot be greater than current allocation",
          String.format("financeData[%s].budgetAllocationChange", i), String.valueOf(financeData.getBudgetAllocationChange()));
        throw new HttpException(422, new Errors().withErrors(List.of(error)));
      }
    }
  }

  private void validateForDuplication(FyFinanceDataCollection financeDataCollection) {
    var financeFundBudgetFiscalYearIds = financeDataCollection.getFyFinanceData()
      .stream().collect(Collectors.groupingBy(
        financeData -> financeData.getFundId() + financeData.getBudgetId() + financeData.getFiscalYearId()));

    if (financeFundBudgetFiscalYearIds.size() > 1) {
      var error = createError("Finance data collection contains duplicate fund, budget and fiscal year IDs",
        "financeData", "duplicate");
      log.warn("validateForDuplication:: Validation error: {}", error.getMessage());
      throw new HttpException(422, new Errors().withErrors(List.of(error)));
    }
  }

  private void validateFinanceDataFields(FyFinanceData financeData, int i, String fiscalYearId) {
    List<Error> combinedErrors = new ArrayList<>();

    if (!financeData.getFiscalYearId().equals(fiscalYearId)) {
      combinedErrors.add(createError(
        String.format("Fiscal year ID must be the same as other fiscal year ID(s) '[%s]' in the request", fiscalYearId),
        String.format("financeData[%s].fiscalYearId", i), financeData.getFiscalYearId())
      );
    }

    validateBudgetStatus(financeData.getBudgetStatus(), i);
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundCode", i), financeData.getFundCode(), "Fund code is required");
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundName", i), financeData.getFundName(), "Fund name is required");
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundDescription", i), financeData.getFundDescription(), "Fund description is required");
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundStatus", i), financeData.getFundStatus(), "Fund status is required");
    if (financeData.getBudgetId() != null) {
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetName", i), financeData.getBudgetName(), "Budget name is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetStatus", i), financeData.getBudgetStatus(), "Budget status is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetInitialAllocation", i), financeData.getBudgetInitialAllocation(), "Budget initial allocation is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetCurrentAllocation", i), financeData.getBudgetCurrentAllocation(), "Budget current allocation is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetAllowableExpenditure", i), financeData.getBudgetAllowableExpenditure(), "Budget allowable expenditure is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetAllowableEncumbrance", i), financeData.getBudgetAllowableEncumbrance(), "Budget allowable encumbrance is required");
    }

    if (CollectionUtils.isNotEmpty(combinedErrors)) {
      var errors = new Errors().withErrors(combinedErrors).withTotalRecords(combinedErrors.size());
      throw new HttpException(422, errors);
    }
  }

  private void validateBudgetStatus(String budgetStatus, int i) {
    if (StringUtils.isNotEmpty(budgetStatus)) {
      try {
        SharedBudget.BudgetStatus.fromValue(budgetStatus);
      } catch (IllegalArgumentException e) {
        var param = new Parameter().withKey(String.format("financeData[%s].budgetStatus", i)).withValue(budgetStatus);
        throw new HttpException(422, BUDGET_STATUS_INCORRECT.toError().withParameters(List.of(param)));
      }
    }
  }

  private void validateRequiredField(List<Error> combinedErrors, String fieldName, Object fieldValue, String errorMessage) {
    if (fieldValue == null) {
      combinedErrors.add(createError(errorMessage, fieldName, "null"));
    }
  }

  public Future<Void> validateIds(FyFinanceDataCollection financeDataCollection, RequestContext requestContext) {
    List<Future<Void>> validationFutures = new ArrayList<>();
    List<Error> errors = new ArrayList<>();

    for (int i = 0; i < financeDataCollection.getFyFinanceData().size(); i++) {
      var financeData = financeDataCollection.getFyFinanceData().get(i);
      validationFutures.add(validateFundId(financeData, i, errors, requestContext));
      if (financeData.getBudgetId() != null) {
        validationFutures.add(validateBudgetId(financeData, i, errors, requestContext));
      }
    }

    return collectResultsOnSuccess(validationFutures)
      .map(compositeFuture -> {
        if (!errors.isEmpty()) {
          throw new HttpException(422, new Errors().withErrors(errors).withTotalRecords(errors.size()));
        }
        return null;
      });
  }

  private Future<Void> validateFundId(FyFinanceData financeData, int index, List<Error> errors,
                                      RequestContext requestContext) {
    return fundService.getFundById(financeData.getFundId(), requestContext)
      .map(fund -> {
        if (financeData.getLedgerId() != null &&
          !Objects.equals(fund.getLedgerId(), financeData.getLedgerId())) {
          errors.add(createError("Fund ledger ID must be the same as ledger ID",
            String.format("financeData[%s].fundId", index),
            financeData.getFundId()));
        }
        return null;
      });
  }

  private Future<Void> validateBudgetId(FyFinanceData financeData, int index, List<Error> errors,
                                        RequestContext requestContext) {
    return budgetService.getBudgetById(financeData.getBudgetId(), requestContext)
      .map(budget -> {
        if (!Objects.equals(budget.getFundId(), financeData.getFundId())) {
          errors.add(createError("Budget fund ID must be the same as fund ID",
            String.format("financeData[%s].budgetId", index),
            financeData.getBudgetId()));
        }
        return null;
      });
  }

  private Error createError(String message, String key, String value) {
    log.warn("Validation error: {}", message);
    var param = new Parameter().withKey(key).withValue(value);
    return new Error().withMessage(message).withParameters(List.of(param));
  }
}

