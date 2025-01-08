package org.folio.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransactionTotalSetting {
  FROM_FUND_ID("fromFundId"),
  TO_FUND_ID("toFundId");

  private final String value;
}
