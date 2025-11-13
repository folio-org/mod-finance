package org.folio.services.financedata;

import static java.util.Objects.requireNonNullElse;
import static org.folio.rest.util.ErrorCodes.BUDGET_STATUS_INCORRECT;
import static org.folio.rest.util.ErrorCodes.FUND_STATUS_INCORRECT;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundTags;
import org.folio.rest.jaxrs.model.FyFinanceData;
import org.folio.rest.jaxrs.model.FyFinanceDataCollection;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.services.budget.BudgetService;
import org.folio.services.fund.FundService;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

@Log4j2
public class FinanceDataValidator {

  private static final String ERROR_KEY_FORMAT = "financeData[%s].%s";

  private final FundService fundService;
  private final BudgetService budgetService;

  public FinanceDataValidator(FundService fundService, BudgetService budgetService) {
    this.fundService = fundService;
    this.budgetService = budgetService;
  }

  public void validateFinanceDataCollection(FyFinanceDataCollection financeDataCollection, String fiscalYearId) {
    validateForDuplication(financeDataCollection);

    List<Error> combinedErrors = new ArrayList<>();
    IntStream.range(0, financeDataCollection.getFyFinanceData().size())
      .forEach(i -> validateFinanceDataFields(combinedErrors, financeDataCollection.getFyFinanceData().get(i), i, fiscalYearId));

    if (CollectionUtils.isNotEmpty(combinedErrors)) {
      var errors = new Errors().withErrors(combinedErrors).withTotalRecords(combinedErrors.size());
      throw new HttpException(422, errors);
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

  private void validateFinanceDataFields(List<Error> combinedErrors, FyFinanceData financeData, int i, String fiscalYearId) {
    if (!financeData.getFiscalYearId().equals(fiscalYearId)) {
      combinedErrors.add(createError(
        String.format("Fiscal year ID must be the same as other fiscal year ID(s) '[%s]' in the request", fiscalYearId),
        String.format("financeData[%s].fiscalYearId", i), financeData.getFiscalYearId())
      );
    }

    validateFundStatus(combinedErrors, financeData.getFundStatus(), i);
    validateBudgetStatus(combinedErrors, financeData.getBudgetStatus(), i);
    validateRequiredField(combinedErrors, "fundCode", i, financeData.getFundCode());
    validateRequiredField(combinedErrors, "fundName", i, financeData.getFundName());
    validateRequiredField(combinedErrors, "fundStatus", i, financeData.getFundStatus());
    validateRequiredField(combinedErrors, "ledgerId", i, financeData.getLedgerId());

    if (StringUtils.isNotEmpty(financeData.getBudgetId())) {
      validateUuid(combinedErrors, "budgetId", i, financeData.getBudgetId());
      validateRequiredField(combinedErrors, "budgetName", i, financeData.getBudgetName());
      validateRequiredField(combinedErrors, "budgetStatus", i, financeData.getBudgetStatus());
      validateRequiredField(combinedErrors, "budgetInitialAllocation", i, financeData.getBudgetInitialAllocation());
      validateNonNullAndNonNegative(combinedErrors, "budgetAllowableExpenditure", i, financeData.getBudgetAllowableExpenditure());
      validateNonNullAndNonNegative(combinedErrors, "budgetAllowableEncumbrance", i, financeData.getBudgetAllowableEncumbrance());
    } else {
      financeData.setBudgetId(null); // to avoid being process as empty string
    }
  }

  private void validateFundStatus(List<Error> combinedErrors, String fundStatus, int i) {
    if (StringUtils.isNotEmpty(fundStatus)) {
      try {
        Fund.FundStatus.fromValue(fundStatus);
      } catch (IllegalArgumentException e) {
        var param = new Parameter().withKey(String.format("financeData[%s].fundStatus", i)).withValue(fundStatus);
        combinedErrors.add(FUND_STATUS_INCORRECT.toError().withParameters(List.of(param)));
      }
    }
  }

  private void validateBudgetStatus(List<Error> combinedErrors, String budgetStatus, int i) {
    if (StringUtils.isNotEmpty(budgetStatus)) {
      try {
        SharedBudget.BudgetStatus.fromValue(budgetStatus);
      } catch (IllegalArgumentException e) {
        var param = new Parameter().withKey(String.format("financeData[%s].budgetStatus", i)).withValue(budgetStatus);
        combinedErrors.add(BUDGET_STATUS_INCORRECT.toError().withParameters(List.of(param)));
      }
    }
  }

  private void validateRequiredField(List<Error> combinedErrors, String fieldName, int index, Object fieldValue) {
    if (fieldValue == null) {
      combinedErrors.add(createError(fieldName + " is required", String.format(ERROR_KEY_FORMAT, index, fieldName), "null"));
    }
  }

  private void validateUuid(List<Error> combinedErrors, String fieldName, int index, String fieldValue) {
    try {
      UUID.fromString(fieldValue);
    } catch (Exception ex) {
      combinedErrors.add(createError("Invalid UUID format", String.format(ERROR_KEY_FORMAT, index, fieldName), fieldValue));
    }
  }

  private void validateNonNullAndNonNegative(List<Error> combinedErrors, String fieldName, int index, Double fieldValue) {
    if (fieldValue != null && fieldValue < 0) {
      combinedErrors.add(createError(fieldName + " cannot be negative", String.format(ERROR_KEY_FORMAT, index, fieldName), fieldValue.toString()));
    }
  }

  public Future<Void> compareWithExistingData(FyFinanceDataCollection financeDataCollection, RequestContext requestContext) {
    return fetchFundsAndBudgetsForFinanceData(financeDataCollection, requestContext)
      .compose(comparisonHolderMap -> {
        List<Error> errors = new ArrayList<>();

        comparisonHolderMap.forEach((index, holder) -> {
          compareFund(holder.fund(), holder.financeData(), index, errors);
          compareBudget(holder.budget(), holder.financeData(), index, errors);
        });

        IntStreamEx.range(financeDataCollection.getFyFinanceData().size()).forEach(i ->
          verifyAllocationChange(errors, financeDataCollection.getFyFinanceData().get(i), i));

        return !errors.isEmpty()
          ? Future.failedFuture(new HttpException(422, new Errors().withErrors(errors).withTotalRecords(errors.size())))
          : Future.succeededFuture();
      });
  }

  private Future<Map<Integer, FinanceDataComparisonHolder>> fetchFundsAndBudgetsForFinanceData(FyFinanceDataCollection financeDataCollection, RequestContext requestContext) {
    var fundIds = StreamEx.of(financeDataCollection.getFyFinanceData()).map(FyFinanceData::getFundId).toSet();
    var budgetIds = StreamEx.of(financeDataCollection.getFyFinanceData()).map(FyFinanceData::getBudgetId).toSet();
    return fundService.getFundsBatch(fundIds, requestContext)
      .compose(funds -> budgetService.getBudgetsBatch(budgetIds, requestContext)
        .map(budgets -> {
          var fundMap = StreamEx.of(funds.getFunds()).valuesToMap(Fund::getId);
          var budgetMap = StreamEx.of(budgets.getBudgets()).valuesToMap(Budget::getId);

          return IntStreamEx.range(financeDataCollection.getFyFinanceData().size()).boxed()
            .toMap(i -> new FinanceDataComparisonHolder(financeDataCollection.getFyFinanceData().get(i),
              fundMap.getOrDefault(financeDataCollection.getFyFinanceData().get(i).getFundId(), null),
              budgetMap.getOrDefault(financeDataCollection.getFyFinanceData().get(i).getBudgetId(), null)));
        }));
  }

  private void compareFund(Fund fund, FyFinanceData financeData, int index, List<Error> errors) {
    if (fund == null) {
      errors.add(createError("Fund ID not found", String.format("financeData[%s].fundId", index), financeData.getFundId()));
      return;
    }
    if (!Objects.equals(fund.getCode(), financeData.getFundCode())) {
      errors.add(createError("fundCode must be the same as existing fund code",
        String.format("financeData[%s].fundCode", index), financeData.getFundCode()));
    }
    if (!Objects.equals(fund.getName(), financeData.getFundName())) {
      errors.add(createError("fundName must be the same as existing fund name",
        String.format("financeData[%s].fundName", index), financeData.getFundName()));
    }
    if (financeData.getLedgerId() != null && !Objects.equals(fund.getLedgerId(), financeData.getLedgerId())) {
      errors.add(createError("Fund ledger ID must be the same as ledger ID",
        String.format("financeData[%s].fundId", index), financeData.getFundId()));
    }

    financeData.setIsFundChanged(isFundChanged(financeData, fund));
  }

  /**
   * Checks if the fund has changed by comparing finance data with existing fund.
   * <p>
   * Changes detected include:
   * - Fund status (if not empty)
   * - Tags (if either new or existing tags are not empty)
   * - Description (if not empty)
   *
   * @param financeData The finance data to check
   * @param existingFund The existing fund to compare against
   * @return true if any fund property has changed
   */
  private static boolean isFundChanged(FyFinanceData financeData, Fund existingFund) {
    var newTags = Optional.ofNullable(financeData.getFundTags())
      .map(FundTags::getTagList)
      .orElse(Collections.emptyList());

    var existingTags = Optional.ofNullable(existingFund.getTags())
      .map(Tags::getTagList)
      .orElse(Collections.emptyList());

    boolean statusChanged = StringUtils.isNotEmpty(financeData.getFundStatus()) &&
      !Objects.equals(financeData.getFundStatus(), existingFund.getFundStatus().value());

    boolean tagsChanged = (!newTags.isEmpty() || !existingTags.isEmpty()) &&
      !Objects.equals(newTags, existingTags);

    boolean descriptionChanged = StringUtils.isNotEmpty(financeData.getFundDescription()) &&
      !Objects.equals(financeData.getFundDescription(), existingFund.getDescription());

    return statusChanged || tagsChanged || descriptionChanged;
  }

  private void compareBudget(Budget budget, FyFinanceData financeData, int index, List<Error> errors) {
    if (StringUtils.isEmpty(financeData.getBudgetId())) {
      financeData.setIsBudgetChanged(financeData.getBudgetStatus() != null || requireNonNullElse(financeData.getBudgetAllocationChange(), 0.0) != 0);
      return;
    }

    if (budget == null) {
      errors.add(createError("Budget ID not found", String.format("financeData[%s].budgetId", index), financeData.getBudgetId()));
      return;
    }
    if (!Objects.equals(budget.getName(), financeData.getBudgetName())) {
      errors.add(createError("budgetName must be the same as existing budget name",
        String.format("financeData[%s].budgetName", index), financeData.getBudgetName()));
    }
    if (!Objects.equals(budget.getFundId(), financeData.getFundId())) {
      errors.add(createError("Budget fund ID must be the same as fund ID",
        String.format("financeData[%s].budgetId", index), financeData.getBudgetId()));
    }

    financeData.withIsBudgetChanged(isBudgetChanged(financeData, budget))
      .withBudgetInitialAllocation(budget.getInitialAllocation())
      .withBudgetCurrentAllocation(budget.getAllocated());
  }

  /**
   * Checks if the budget has changed by comparing finance data with existing budget.
   * <p>
   * Changes detected include:
   * - Budget status (if not empty)
   * - Allowable encumbrance
   * - Allowable expenditure
   * - Allocation change (if not zero or null)
   * - Budget change flag
   *
   * @param financeData The finance data to check
   * @param existingBudget The existing budget to compare against
   * @return true if any budget property has changed
   */
  private static boolean isBudgetChanged(FyFinanceData financeData, Budget existingBudget) {
    boolean statusChanged = StringUtils.isNotEmpty(financeData.getBudgetStatus()) &&
      !Objects.equals(financeData.getBudgetStatus(), String.valueOf(existingBudget.getBudgetStatus()));

    boolean allowableChanged = !Objects.equals(financeData.getBudgetAllowableEncumbrance(), existingBudget.getAllowableEncumbrance()) ||
      !Objects.equals(financeData.getBudgetAllowableExpenditure(), existingBudget.getAllowableExpenditure());

    boolean allocationChanged = requireNonNullElse(financeData.getBudgetAllocationChange(), 0.0) != 0;
    boolean flaggedAsChanged = Boolean.TRUE.equals(financeData.getIsBudgetChanged());

    return statusChanged || allowableChanged || allocationChanged || flaggedAsChanged;
  }

  private void verifyAllocationChange(List<Error> errors, FyFinanceData financeData, int i) {
    if (financeData.getBudgetAllocationChange() != null) {
      var allocationChange = BigDecimal.valueOf(requireNonNullElse(financeData.getBudgetAllocationChange(), 0.0));
      var currentAllocation = BigDecimal.valueOf(requireNonNullElse(financeData.getBudgetCurrentAllocation(), 0.0));

      if (allocationChange.compareTo(BigDecimal.ZERO) < 0 && allocationChange.abs().compareTo(currentAllocation) > 0) {
        errors.add(createError("New total allocation cannot be negative",
          String.format("financeData[%s].budgetAllocationChange", i), String.valueOf(financeData.getBudgetAllocationChange())));
      }
    }
  }

  private Error createError(String message, String key, String value) {
    log.warn("Validation error: {}", message);
    var param = new Parameter().withKey(key).withValue(value);
    return new Error().withMessage(message).withParameters(List.of(param));
  }

  private record FinanceDataComparisonHolder(FyFinanceData financeData, Fund fund, Budget budget) { }

}

