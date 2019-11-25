package org.folio.rest.util;

import org.folio.rest.jaxrs.model.Error;

public enum ErrorCodes {

  GENERIC_ERROR_CODE("genericError", "Generic error"),
  MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY("idMismatch", "Mismatch between id in path and request body"),
  FISCAL_YEARS_NOT_FOUND("fiscalYearsNotFound", "Cannot find current fiscal year for specified ledger"),
  GROUP_NOT_FOUND("groupNotFound", "Cannot find group"),
  INVALID_TRANSACTION_TYPE("invalidTransactionType", "Invalid transaction type"),
  INVALID_TRANSACTION_COUNT("invalidTransactionCount", "Number of transactions have to be greater than or equal to 1");

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
