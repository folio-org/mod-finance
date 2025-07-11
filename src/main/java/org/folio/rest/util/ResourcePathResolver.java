package org.folio.rest.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourcePathResolver {

  private ResourcePathResolver() {
  }

  public static final String BUDGETS_STORAGE = "budgets";
  public static final String FINANCE_DATA_STORAGE = "financeData";
  public static final String FUNDS_STORAGE = "funds";
  public static final String FUND_TYPES = "fundTypes";
  public static final String FUND_UPDATE_LOGS = "fundUpdateLogs";
  public static final String JOB_NUMBER = "jobNumber";
  public static final String GROUP_FUND_FISCAL_YEARS = "groupFundFiscalYears";
  public static final String FISCAL_YEARS_STORAGE = "fiscalYears";
  public static final String LEDGERS_STORAGE = "ledgers";
  public static final String LEDGER_ROLLOVERS_STORAGE = "ledgerRollovers";
  public static final String LEDGER_ROLLOVERS_LOGS_STORAGE = "ledgerRolloverLogs";
  public static final String LEDGER_ROLLOVERS_BUDGETS_STORAGE = "ledgerRolloverBudgets";
  public static final String LEDGER_ROLLOVERS_ERRORS_STORAGE = "ledgerRolloverErrors";
  public static final String LEDGER_ROLLOVERS_PROGRESS_STORAGE = "ledgerRolloverProgress";
  public static final String GROUPS = "groups";
  public static final String TRANSACTIONS = "transactions";
  public static final String TRANSACTION_TOTALS = "transactionTotals";
  public static final String BATCH_TRANSACTIONS = "batchTransactions";
  public static final String BATCH_TRANSACTIONS_STORAGE = "batchTransactionsStorage";
  public static final String COMMON_SETTINGS = "settings";
  public static final String EXPENSE_CLASSES_STORAGE_URL = "expenseClassStorageUrl";
  public static final String EXPENSE_CLASSES_URL = "expenseClassUrl";
  public static final String BUDGET_EXPENSE_CLASSES = "budgetExpenseClasses";
  public static final String ACQUISITIONS_UNITS = "acquisitionsUnits";
  public static final String ACQUISITIONS_MEMBERSHIPS = "acquisitionsMemberships";
  public static final String EXCHANGE_RATE_SOURCE = "exchangeRateSource";

  private static final Map<String, String> SUB_OBJECT_ITEM_APIS;
  private static final Map<String, String> SUB_OBJECT_COLLECTION_APIS;
  static {
    Map<String, String> apis = new HashMap<>();
    apis.put(BUDGETS_STORAGE, "/finance-storage/budgets");
    apis.put(FINANCE_DATA_STORAGE, "/finance-storage/finance-data");
    apis.put(FUNDS_STORAGE, "/finance-storage/funds");
    apis.put(FUND_TYPES, "/finance-storage/fund-types");
    apis.put(FUND_UPDATE_LOGS, "/finance-storage/fund-update-logs");
    apis.put(JOB_NUMBER, "/finance-storage/job-number");
    apis.put(GROUP_FUND_FISCAL_YEARS, "/finance-storage/group-fund-fiscal-years");
    apis.put(FISCAL_YEARS_STORAGE, "/finance-storage/fiscal-years");
    apis.put(LEDGERS_STORAGE, "/finance-storage/ledgers");
    apis.put(LEDGER_ROLLOVERS_STORAGE, "/finance-storage/ledger-rollovers");
    apis.put(LEDGER_ROLLOVERS_BUDGETS_STORAGE, "/finance-storage/ledger-rollovers-budgets");
    apis.put(LEDGER_ROLLOVERS_LOGS_STORAGE, "/finance-storage/ledger-rollovers-logs");
    apis.put(LEDGER_ROLLOVERS_ERRORS_STORAGE, "/finance-storage/ledger-rollovers-errors");
    apis.put(LEDGER_ROLLOVERS_PROGRESS_STORAGE, "/finance-storage/ledger-rollovers-progress");
    apis.put(GROUPS, "/finance-storage/groups");
    apis.put(TRANSACTIONS, "/finance-storage/transactions");
    apis.put(TRANSACTION_TOTALS, "/finance-storage/transaction-totals");
    apis.put(BATCH_TRANSACTIONS, "/finance/transactions/batch-all-or-nothing");
    apis.put(BATCH_TRANSACTIONS_STORAGE, "/finance-storage/transactions/batch-all-or-nothing");
    apis.put(COMMON_SETTINGS, "/settings/entries");
    apis.put(EXPENSE_CLASSES_STORAGE_URL, "/finance-storage/expense-classes");
    apis.put(EXPENSE_CLASSES_URL, "/finance/expense-classes");
    apis.put(BUDGET_EXPENSE_CLASSES, "/finance-storage/budget-expense-classes");
    apis.put(ACQUISITIONS_UNITS, "/acquisitions-units-storage/units");
    apis.put(ACQUISITIONS_MEMBERSHIPS, "/acquisitions-units-storage/memberships");
    apis.put(EXCHANGE_RATE_SOURCE, "/finance-storage/exchange-rate-source");

    SUB_OBJECT_COLLECTION_APIS = Collections.unmodifiableMap(apis);
    SUB_OBJECT_ITEM_APIS = Collections.unmodifiableMap(
      apis.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() + "/%s"))
    );
  }

  public static String resourceByIdPath(String field, String id) {
    return String.format(SUB_OBJECT_ITEM_APIS.get(field), id);
  }

  public static String resourcesPath(String field) {
    return SUB_OBJECT_COLLECTION_APIS.get(field);
  }
}
