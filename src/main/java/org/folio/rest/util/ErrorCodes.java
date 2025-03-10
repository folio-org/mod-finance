package org.folio.rest.util;

import org.folio.rest.jaxrs.model.Error;

public enum ErrorCodes {

  LEDGER_NOT_FOUND_FOR_TRANSACTION("ledgerNotFoundForTransaction", "Ledger not found for transaction"),
  GENERIC_ERROR_CODE("genericError", "Generic error"),
  MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY("idMismatch", "Mismatch between id in path and request body"),
  FISCAL_YEARS_NOT_FOUND("fiscalYearsNotFound", "Cannot find current fiscal year for specified ledger"),
  GROUP_NOT_FOUND("groupNotFound", "Cannot find group"),
  INVALID_TRANSACTION_TYPE("invalidTransactionType", "Invalid transaction type"),
  INVALID_INVOICE_TRANSACTION_COUNT("invalidInvoiceTransactionCount",
      "Number of numPaymentsCredits transactions have to be greater than 1 and numEncumbrances have to greater than or equal to 0"),
  INVALID_ORDER_TRANSACTION_COUNT("invalidOrderTransactionCount", "Number of order transactions have to be greater than 0"),
  CURRENCY_NOT_FOUND("currencyNotFound", "Failed to fetch currency from Locale"),
  ALLOCATION_TRANSFER_FAILED("failedAllocationTransaction", "Failed to create allocation transaction for a budget"),
  FISCAL_YEAR_NOT_FOUND("fiscalYearNotFound", "Fiscal year not found for the specified fiscalYearId"),
  ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED("allowableEncumbranceLimitExceeded", "Allowable encumbrance limit exceeded"),
  ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED("allowableExpenditureLimitExceeded", "Allowable expenditures limit exceeded"),
  TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR("transactionIsPresentBudgetDeleteError",
    "Budget related transactions found. Deletion of the budget is forbidden."),
  MISSING_FUND_ID("missingFundId", "Missing to/from fund id"),
  ALLOCATION_IDS_MISMATCH("allowableAllocationIdsMismatch", "Allowable allocation ids mismatch"),
  TRANSACTION_IS_PRESENT_BUDGET_EXPENSE_CLASS_DELETE_ERROR("transactionIsPresentBudgetExpenseClassDeleteError", "Budget expense class related transactions found. Deletion of the budget expense class is forbidden."),
  CURRENT_BUDGET_NOT_FOUND("currentBudgetNotFound", "Current budget doesn't exist"),
  CURRENT_FISCAL_YEAR_NOT_FOUND("currentFiscalYearNotFound", "Current fiscal year doesn't exist"),
  PLANNED_FISCAL_YEAR_NOT_FOUND("plannedFiscalYearNotFound", "Planned fiscal year doesn't exist"),
  MISSING_FISCAL_YEAR_ID("missingFiscalYearId", "fiscalYearId must not be null"),
  NEGATIVE_VALUE("negativeValue", "Value must be greater or equal to zero"),
  FUND_NOT_FOUND_ERROR("fundNotFoundError", "Fund with UUID %s does not exist"),
  FUND_UNITS_NOT_FOUND("fundAcqUnitsNotFound", "Acquisitions units assigned to fund cannot be found"),
  USER_HAS_NO_PERMISSIONS("userHasNoPermission", "User does not have permissions - operation is restricted"),
  USER_HAS_NO_ACQ_PERMISSIONS("userHasNoAcqUnitsPermission", "User does not have permissions to manage acquisition units assignments - operation is restricted"),
  FISCAL_YEAR_INVALID_PERIOD("fiscalYearInvalidPeriod", "Fiscal year period end date is earlier than start date"),
  FISCAL_YEAR_INVALID_CODE("invalidFiscalYearCode", "Fiscal year code must be in proper format"),
  DELETE_CONNECTED_TO_INVOICE("deleteConnectedToInvoice", "Cannot delete an encumbrance connected to an invoice"),
  TRANSACTION_NOT_RELEASED("transactionNotReleased", "Encumbrance should be released before deletion"),
  UPDATE_PAYMENT_TO_CANCEL_INVOICE("updatePaymentToCancelInvoice", "A payment can only be updated to cancel an invoice"),
  UPDATE_CREDIT_TO_CANCEL_INVOICE("updateCreditToCancelInvoice", "A credit can only be updated to cancel an invoice"),
  CONFLICT("conflict", "Conflict when updating a record"),
  BUDGET_STATUS_INCORRECT("budgetStatusIncorrect", "Budget status is incorrect"),
  FUND_STATUS_INCORRECT("fundStatusIncorrect", "Fund status is incorrect"),
  NEGATIVE_ALLOCATION("negativeBudgetAllocation", "A negative budget allocation is not allowed");

  private final String code;
  private final String description;

  ErrorCodes(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getCode() {
    return code;
  }

  @Override
  public String toString() {
    return code + ": " + description;
  }

  public Error toError() {
    return new Error().withCode(code).withMessage(description);
  }
}
