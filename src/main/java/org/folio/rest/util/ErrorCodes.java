package org.folio.rest.util;

import org.folio.rest.jaxrs.model.Error;

public enum ErrorCodes {

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
  LEDGER_FY_NOT_FOUND("ledgerFYNotFound", "No summary values for the specified fiscal year"),
  ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED("allowableEncumbranceLimitExceeded", "Allowable encumbrance limit exceeded"),
  ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED("allowableExpenditureLimitExceeded", "Allowable expenditures limit exceeded"),
  TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR("transactionIsPresentBudgetDeleteError",
    "Budget related transactions found. Deletion of the budget is forbidden.");

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
