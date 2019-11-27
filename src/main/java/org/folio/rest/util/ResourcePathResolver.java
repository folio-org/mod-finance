package org.folio.rest.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourcePathResolver {

  private ResourcePathResolver() {
  }

  public static final String BUDGETS = "budgets";
  public static final String FUNDS = "funds";
  public static final String FUND_TYPES = "fundTypes";
  public static final String GROUP_FUND_FISCAL_YEARS = "groupFundFiscalYears";
  public static final String FISCAL_YEARS = "fiscalYears";
  public static final String LEDGERS = "ledgers";
  public static final String GROUPS = "groups";
  public static final String TRANSACTIONS = "transactions";
  public static final String CONFIGURATIONS = "configurations";
  public static final String ORDER_TRANSACTION_SUMMARIES = "orderTransactionSummaries";

  private static final Map<String, String> SUB_OBJECT_ITEM_APIS;
  private static final Map<String, String> SUB_OBJECT_COLLECTION_APIS;

  static {
    Map<String, String> apis = new HashMap<>();
    apis.put(BUDGETS, "/finance-storage/budgets");
    apis.put(FUNDS, "/finance-storage/funds");
    apis.put(FUND_TYPES, "/finance-storage/fund-types");
    apis.put(GROUP_FUND_FISCAL_YEARS, "/finance-storage/group-fund-fiscal-years");
    apis.put(FISCAL_YEARS, "/finance-storage/fiscal-years");
    apis.put(LEDGERS, "/finance-storage/ledgers");
    apis.put(GROUPS, "/finance-storage/groups");
    apis.put(TRANSACTIONS, "/finance-storage/transactions");
    apis.put(CONFIGURATIONS, "/configurations/entries");
    apis.put(ORDER_TRANSACTION_SUMMARIES, "/finance-storage/order-transaction-summaries");

    SUB_OBJECT_COLLECTION_APIS = Collections.unmodifiableMap(apis);
    SUB_OBJECT_ITEM_APIS = Collections.unmodifiableMap(
      apis.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() + "/%s?lang=%s"))
    );
  }

  public static String resourceByIdPath(String field, String id, String lang) {
    return String.format(SUB_OBJECT_ITEM_APIS.get(field), id, lang);
  }

  public static String resourcesPath(String field) {
    return SUB_OBJECT_COLLECTION_APIS.get(field);
  }
}
