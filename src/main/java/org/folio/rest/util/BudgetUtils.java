package org.folio.rest.util;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import java.util.List;
import java.util.stream.Collectors;
import io.vertx.core.json.JsonObject;

public final class BudgetUtils {

  private BudgetUtils() {

  }

  public static SharedBudget buildSharedBudget(Budget budget, List<BudgetExpenseClass> budgetExpenseClasses) {
    List<StatusExpenseClass> statusExpenseClasses = budgetExpenseClasses.stream()
      .map(ExpenseClassConverterUtils::buildStatusExpenseClass)
      .collect(Collectors.toList());
    return convertToSharedBudget(budget).withStatusExpenseClasses(statusExpenseClasses);
  }

  public static SharedBudget convertToSharedBudget(Budget budget) {
    return JsonObject.mapFrom(budget).mapTo(SharedBudget.class);
  }

  public static Budget convertToBudget(SharedBudget budget) {
    JsonObject jsonSharedBudget =  JsonObject.mapFrom(budget);
    jsonSharedBudget.remove("statusExpenseClasses");
    return jsonSharedBudget.mapTo(Budget.class);
  }
}
