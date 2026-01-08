package org.folio.rest.util;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.StatusExpenseClass;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionType;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class BudgetUtils {

  public static final List<Transaction.TransactionType> TRANSFER_TRANSACTION_TYPES = List.of(Transaction.TransactionType.TRANSFER, Transaction.TransactionType.ROLLOVER_TRANSFER);
  public static final List<TransactionType> TRANSFER_TRANSACTION_TOTAL_TYPES = List.of(TransactionType.TRANSFER, TransactionType.ROLLOVER_TRANSFER);

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
    JsonObject jsonSharedBudget = JsonObject.mapFrom(budget);
    jsonSharedBudget.remove("statusExpenseClasses");
    return jsonSharedBudget.mapTo(Budget.class);
  }
}
