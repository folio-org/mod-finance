package org.folio.services.financedata;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.requireNonNullElse;
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
import org.folio.rest.jaxrs.model.Fund;
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

      if (financeData.getBudgetAllocationChange() != null) {
        double allocationChange = requireNonNullElse(financeData.getBudgetAllocationChange(), 0.0);
        double currentAllocation = requireNonNullElse(financeData.getBudgetCurrentAllocation(), 0.0);

        if (allocationChange < 0 && Math.abs(allocationChange) > currentAllocation) {
          var error = createError("Allocation change cannot be greater than current allocation",
            String.format("financeData[%s].budgetAllocationChange", i), String.valueOf(financeData.getBudgetAllocationChange()));
          throw new HttpException(422, new Errors().withErrors(List.of(error)));
        }
      }
    }
  }

  private void validateForDuplication(FyFinanceDataCollection financeDataCollection) {
    var financeFundBudgetFiscalYearIds = financeDataCollection.getFyFinanceData()
      .stream().collect(Collectors.groupingBy(
        financeData -> financeData.getFundId() + financeData.getBudgetId() + financeData.getFiscalYearId()));

    financeFundBudgetFiscalYearIds.values().forEach(duplicates -> {
      if (duplicates.size() > 1) {
        var error = createError("Finance data collection contains duplicate fund, budget and fiscal year IDs", "financeData", "duplicate");
        log.warn("validateForDuplication:: Validation error: {}", error.getMessage());
        throw new HttpException(422, new Errors().withErrors(List.of(error)));
      }
    });
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
    validateRequiredField(combinedErrors, String.format("financeData[%s].fundStatus", i), financeData.getFundStatus(), "Fund status is required");
    if (financeData.getBudgetId() != null) {
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetName", i), financeData.getBudgetName(), "Budget name is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetStatus", i), financeData.getBudgetStatus(), "Budget status is required");
      validateRequiredField(combinedErrors, String.format("financeData[%s].budgetInitialAllocation", i), financeData.getBudgetInitialAllocation(), "Budget initial allocation is required");
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

  public Future<Void> compareWithExistingData(FyFinanceDataCollection financeDataCollection, RequestContext requestContext) {
    List<Future<Void>> validationFutures = new ArrayList<>();
    List<Error> errors = new ArrayList<>();

    for (int i = 0; i < financeDataCollection.getFyFinanceData().size(); i++) {
      var financeData = financeDataCollection.getFyFinanceData().get(i);
      validationFutures.add(compareFund(financeData, i, errors, requestContext));
      validationFutures.add(compareBudget(financeData, i, errors, requestContext));
    }

    return collectResultsOnSuccess(validationFutures)
      .map(compositeFuture -> {
        if (!errors.isEmpty()) {
          throw new HttpException(422, new Errors().withErrors(errors).withTotalRecords(errors.size()));
        }
        return null;
      });
  }

  private Future<Void> compareFund(FyFinanceData financeData, int index, List<Error> errors, RequestContext requestContext) {
    return fundService.getFundById(financeData.getFundId(), requestContext)
      .compose(existingFund -> {
        if (!Objects.equals(existingFund.getCode(), financeData.getFundCode())) {
          errors.add(createError("fundCode must be the same as existing fund code",
            String.format("financeData[%s].fundCode", index), financeData.getFundCode()));
        }
        if (!Objects.equals(existingFund.getName(), financeData.getFundName())) {
          errors.add(createError("fundName must be the same as existing fund name",
            String.format("financeData[%s].fundName", index), financeData.getFundName()));
        }
        if (financeData.getLedgerId() != null && !Objects.equals(existingFund.getLedgerId(), financeData.getLedgerId())) {
          errors.add(createError("Fund ledger ID must be the same as ledger ID",
            String.format("financeData[%s].fundId", index), financeData.getFundId()));
        }

        financeData.setIsFundChanged(isFundChanged(financeData, existingFund));
        return succeededFuture();
      })
      .recover(throwable -> {
        errors.add(createError("Fund ID not found", String.format("financeData[%s].fundId", index), financeData.getFundId()));
        return failedFuture(new HttpException(422, new Errors().withErrors(errors)));
      }).mapEmpty();
  }

  /**
   * Checks if the fund has changed by comparing the provided finance data with the existing fund.
   * <p>
   * The method performs the following checks with existing fund:
   * 1. Status, check if the status has changed.
   * 2. Tags, check if the tags have changed. Edge case - Not changed if the both new and existing tags are null or empty.
   * 3. Description, check if the description has updated. Edge case - Not changed if the description is null.
   * </p>
   * @param financeData   the finance data to be validated
   * @param existingFund the existing fund to compare against
   * @return true if the fund has changed, false otherwise
   */
  private static boolean isFundChanged(FyFinanceData financeData, Fund existingFund) {
    var newTags = financeData.getFundTags() != null ? financeData.getFundTags().getTagList() : new ArrayList<>();
    var existingTags = existingFund.getTags() != null ? existingFund.getTags().getTagList() : new ArrayList<>();
    return !Objects.equals(financeData.getFundStatus().value(), existingFund.getFundStatus().value())
      || (!newTags.isEmpty() || !existingTags.isEmpty()) && !Objects.equals(newTags, existingTags)
      || (financeData.getFundDescription() != null && !Objects.equals(financeData.getFundDescription(), existingFund.getDescription()));
  }

  private Future<Void> compareBudget(FyFinanceData financeData, int index, List<Error> errors, RequestContext requestContext) {
    if (financeData.getBudgetId() == null) {
      boolean isBudgetChanged = financeData.getBudgetStatus() != null
        || requireNonNullElse(financeData.getBudgetAllocationChange(), 0.0) != 0;
      financeData.setIsBudgetChanged(isBudgetChanged);
      return succeededFuture();
    }

    return budgetService.getBudgetById(financeData.getBudgetId(), requestContext)
      .compose(existingBudget -> {
        if (!Objects.equals(existingBudget.getName(), financeData.getBudgetName())) {
          errors.add(createError("budgetName must be the same as existing budget name",
            String.format("financeData[%s].budgetName", index), financeData.getBudgetName()));
        }
        if (!Objects.equals(existingBudget.getFundId(), financeData.getFundId())) {
          errors.add(createError("Budget fund ID must be the same as fund ID",
            String.format("financeData[%s].budgetId", index), financeData.getBudgetId()));
        }

        financeData.setIsBudgetChanged(isBudgetChanged(financeData, existingBudget));
        return succeededFuture();
      })
      .recover(t -> {
        errors.add(createError("Budget ID not found", String.format("financeData[%s].budgetId", index), financeData.getBudgetId()));
        return failedFuture(new HttpException(422, new Errors().withErrors(errors)));
      }).mapEmpty();
  }

  /**
   * Checks if the budget has changed by comparing the provided finance data with the existing budget.
   * <p>
   * The method performs the following checks:
   * 1. Compares the budget status of the finance data with the existing budget.
   * 2. Compares the allowable encumbrance of the finance data with the existing budget.
   * 3. Compares the allowable expenditure of the finance data with the existing budget.
   * 4. Check if allocationChange have updated. Not changed if the allocation change is zero or null
   * 5. Checks if the budget change flag in the finance data is set to true previously.
   * </p>
   * @param financeData   the finance data to be validated
   * @param existingBudget the existing budget to compare against
   * @return true if the budget has changed, false otherwise
   */
  private static boolean isBudgetChanged(FyFinanceData financeData, SharedBudget existingBudget) {
    return !Objects.equals(financeData.getBudgetStatus(), String.valueOf(existingBudget.getBudgetStatus()))
      || !Objects.equals(financeData.getBudgetAllowableEncumbrance(), existingBudget.getAllowableEncumbrance())
      || !Objects.equals(financeData.getBudgetAllowableExpenditure(), existingBudget.getAllowableExpenditure())
      || requireNonNullElse(financeData.getBudgetAllocationChange(), 0.0) != 0
      || financeData.getIsBudgetChanged() != null && financeData.getIsBudgetChanged();
  }

  private Error createError(String message, String key, String value) {
    log.warn("Validation error: {}", message);
    var param = new Parameter().withKey(key).withValue(value);
    return new Error().withMessage(message).withParameters(List.of(param));
  }
}

