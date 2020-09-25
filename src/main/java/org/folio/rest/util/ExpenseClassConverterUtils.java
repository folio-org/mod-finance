package org.folio.rest.util;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.StatusExpenseClass;

public final class ExpenseClassConverterUtils {

  private ExpenseClassConverterUtils() {

  }

  public static List<StatusExpenseClass> buildStatusExpenseClassList(List<BudgetExpenseClass> budgetExpenseClassList) {
    if (!CollectionUtils.isEmpty(budgetExpenseClassList)) {
      return budgetExpenseClassList.stream()
        .map(ExpenseClassConverterUtils::buildStatusExpenseClass)
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public static StatusExpenseClass buildStatusExpenseClass(BudgetExpenseClass budgetExpenseClass) {
    return new StatusExpenseClass()
      .withExpenseClassId(budgetExpenseClass.getExpenseClassId())
      .withStatus(StatusExpenseClass.Status.fromValue(budgetExpenseClass.getStatus().value()));
  }

}
