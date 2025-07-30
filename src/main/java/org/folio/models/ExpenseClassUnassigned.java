package org.folio.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.ExpenseClass;

@Getter
@AllArgsConstructor
public enum ExpenseClassUnassigned {
  ID("UNASSIGNED"),
  NAME("Unassigned");

  private final String value;

  public static String getExpenseClassName(ExpenseClass expenseClass) {
    return StringUtils.equals(expenseClass.getId(), ID.getValue()) ? NAME.getValue() : expenseClass.getName();
  }
}
