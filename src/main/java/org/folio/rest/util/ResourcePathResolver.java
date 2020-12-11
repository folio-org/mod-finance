package org.folio.rest.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourcePathResolver {

  private ResourcePathResolver() {
  }

  public static final String BUDGETS_STORAGE = "budgets";
  public static final String FUNDS_STORAGE = "funds";
  public static final String FUND_TYPES = "fundTypes";
  public static final String GROUP_FUND_FISCAL_YEARS = "groupFundFiscalYears";
  public static final String FISCAL_YEARS_STORAGE = "fiscalYears";
  public static final String LEDGERS_STORAGE = "ledgers";
  public static final String LEDGER_ROLLOVERS_STORAGE = "ledgerRollovers";
  public static final String LEDGER_ROLLOVERS_PROGRESS_STORAGE = "ledgerRolloverProgress";
  public static final String GROUPS = "groups";
  public static final String TRANSACTIONS = "transactions";
  public static final String CONFIGURATIONS = "configurations";
  public static final String ORDER_TRANSACTION_SUMMARIES = "orderTransactionSummaries";
  public static final String INVOICE_TRANSACTION_SUMMARIES = "invoiceTransactionSummaries";
  public static final String EXPENSE_CLASSES_STORAGE_URL = "expenseClassStorageUrl";
  public static final String EXPENSE_CLASSES_URL = "expenseClassUrl";
  public static final String BUDGET_EXPENSE_CLASSES = "budgetExpenseClasses";

  private static final Map<String, String> SUB_OBJECT_ITEM_APIS;
  private static final Map<String, String> SUB_OBJECT_COLLECTION_APIS;
  private static final String LANG_PARAM = "?lang=%s";
  static {
    Map<String, String> apis = new HashMap<>();
    apis.put(BUDGETS_STORAGE, "/finance-storage/budgets");
    apis.put(FUNDS_STORAGE, "/finance-storage/funds");
    apis.put(FUND_TYPES, "/finance-storage/fund-types");
    apis.put(GROUP_FUND_FISCAL_YEARS, "/finance-storage/group-fund-fiscal-years");
    apis.put(FISCAL_YEARS_STORAGE, "/finance-storage/fiscal-years");
    apis.put(LEDGERS_STORAGE, "/finance-storage/ledgers");
    apis.put(LEDGER_ROLLOVERS_STORAGE, "/finance-storage/ledger-rollovers");
    apis.put(LEDGER_ROLLOVERS_PROGRESS_STORAGE, "/finance-storage/ledger-rollovers-progress");
    apis.put(GROUPS, "/finance-storage/groups");
    apis.put(TRANSACTIONS, "/finance-storage/transactions");
    apis.put(CONFIGURATIONS, "/configurations/entries");
    apis.put(ORDER_TRANSACTION_SUMMARIES, "/finance-storage/order-transaction-summaries");
    apis.put(INVOICE_TRANSACTION_SUMMARIES, "/finance-storage/invoice-transaction-summaries");
    apis.put(EXPENSE_CLASSES_STORAGE_URL, "/finance-storage/expense-classes");
    apis.put(EXPENSE_CLASSES_URL, "/finance/expense-classes");
    apis.put(BUDGET_EXPENSE_CLASSES, "/finance-storage/budget-expense-classes");

    SUB_OBJECT_COLLECTION_APIS = Collections.unmodifiableMap(apis);
    SUB_OBJECT_ITEM_APIS = Collections.unmodifiableMap(
      apis.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() + "/%s"))
    );
  }

  public static String resourceByIdPath(String field, String id, String lang) {
    return String.format(SUB_OBJECT_ITEM_APIS.get(field).concat(LANG_PARAM), id, lang);
  }

  public static String resourceByIdPath(String field, String id) {
    return String.format(SUB_OBJECT_ITEM_APIS.get(field), id);
  }

  public static String resourcesPath(String field) {
    return SUB_OBJECT_COLLECTION_APIS.get(field);
  }
}
