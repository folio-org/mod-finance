package org.folio.rest.util;

import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.TestUtils.getMockData;

import java.io.IOException;
import java.util.function.Supplier;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudget;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverError;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverLog;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverProgress;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.resource.Finance;
import org.folio.rest.jaxrs.resource.FinanceBudgets;
import org.folio.rest.jaxrs.resource.FinanceExpenseClasses;
import org.folio.rest.jaxrs.resource.FinanceFiscalYears;
import org.folio.rest.jaxrs.resource.FinanceFundTypes;
import org.folio.rest.jaxrs.resource.FinanceFundUpdateLogs;
import org.folio.rest.jaxrs.resource.FinanceFunds;
import org.folio.rest.jaxrs.resource.FinanceGroupFundFiscalYears;
import org.folio.rest.jaxrs.resource.FinanceGroups;
import org.folio.rest.jaxrs.resource.FinanceLedgerRollovers;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversBudgets;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversErrors;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversLogs;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversProgress;
import org.folio.rest.jaxrs.resource.FinanceLedgers;
import org.folio.rest.tools.parser.JsonPathParser;

import io.vertx.core.json.JsonObject;

public enum TestEntities {
  BUDGET("budgets", getEndpoint(FinanceBudgets.class), Budget.class, "mockdata/budgets/budgets.json", "budgets[0]", "name", "Updated name", 1, "allocated"),
  FUND("funds", getEndpoint(FinanceFunds.class), Fund.class, "mockdata/funds/funds.json", "funds[0]", "name", "History", 1),
  FUND_TYPE("fundTypes", getEndpoint(FinanceFundTypes.class), FundType.class, "mockdata/fund-types/types.json", "fundTypes[0]", "name", "New type name", 1),
  FUND_UPDATE_LOG("fundUpdateLogs", getEndpoint(FinanceFundUpdateLogs.class), FundUpdateLog.class, "mockdata/fund-update-logs/fund-update-logs.json", "fundUpdateLogs[0]", "jobName", "Yearly Update", 1),
  GROUP_FUND_FISCAL_YEAR("groupFundFiscalYears", getEndpoint(FinanceGroupFundFiscalYears.class), GroupFundFiscalYear.class, "mockdata/group-fund-fiscal-years/group_fund_fiscal_years.json", "groupFundFiscalYears[0]", "allocated", 10000, 1),
  FISCAL_YEAR("fiscalYears", getEndpoint(FinanceFiscalYears.class), FiscalYear.class, "mockdata/fiscal-years/fiscal_years.json", "fiscalYears[0]", "code", "FY2020", 1, "currency"),
  LEDGER("ledgers", getEndpoint(FinanceLedgers.class), Ledger.class, "mockdata/ledgers/ledgers.json", "ledgers[0]", "ledgerStatus", "Active", 1),
  GROUP("groups", getEndpoint(FinanceGroups.class), Group.class, "mockdata/groups/groups.json", "groups[0]", "status", "Frozen", 1),
  TRANSACTIONS("transactions", getEndpoint(Finance.class) + "/transactions", Transaction.class, "mockdata/transactions/transactions.json", "transactions[0]", "amount", 25, 3),
  TRANSACTIONS_ALLOCATION("Allocation", getEndpoint(Finance.class) + "/allocations", Transaction.class, "mockdata/transactions/allocations.json", "transactions[0]", "amount", 25, 1),
  TRANSACTIONS_TRANSFER("Transfer", getEndpoint(Finance.class) + "/transfers", Transaction.class, "mockdata/transactions/transfers.json", "transactions[0]", "amount", 25, 1),
  EXPENSE_CLASSES("expenseClasses", getEndpoint(FinanceExpenseClasses.class), ExpenseClass.class, "mockdata/expense-classes/expense-classes.json", "expenseClasses[0]", "externalAccountNumberExt", 1, 1),
  LEDGER_ROLLOVER("ledgerRollover", getEndpoint(FinanceLedgerRollovers.class), LedgerFiscalYearRollover.class, "mockdata/ledger-rollovers/ledger-rollovers.json", "ledgerFiscalYearRollovers[0]", "toFiscalYearId", 1, 1),
  LEDGER_ROLLOVER_LOGS("ledgerRolloverLogs", getEndpoint(FinanceLedgerRolloversLogs.class), LedgerFiscalYearRolloverLog.class, "", "", "", 1, 1),
  LEDGER_ROLLOVER_BUDGETS("ledgerRolloverBudgets", getEndpoint(FinanceLedgerRolloversBudgets.class), LedgerFiscalYearRolloverBudget.class, "", "", "", 1, 1),
  LEDGER_ROLLOVER_PROGRESS("ledgerRolloverProgress", getEndpoint(FinanceLedgerRolloversProgress.class), LedgerFiscalYearRolloverProgress.class, "", "", "", 1, 1),
  LEDGER_ROLLOVER_ERRORS("ledgerRolloverErrors", getEndpoint(FinanceLedgerRolloversErrors.class), LedgerFiscalYearRolloverError.class, "", "", "", 1, 1);

  TestEntities(String name, String endpoint, Class<?> clazz, String pathToSamples, String jsonPathToSample, String updatedFieldName,
               Object updatedFieldValue, int collectionQuantity) {
    this.name = name;
    this.endpoint = endpoint;
    this.clazz = clazz;
    this.jsonPathToObject = jsonPathToSample;
    this.pathToFileWithData = pathToSamples;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
    this.collectionQuantity = collectionQuantity;
  }

  TestEntities(String name, String endpoint, Class<?> clazz, String pathToSamples, String jsonPathToSample, String updatedFieldName,
      Object updatedFieldValue, int collectionQuantity, String ignoreProperties) {
    this.name = name;
    this.endpoint = endpoint;
    this.clazz = clazz;
    this.jsonPathToObject = jsonPathToSample;
    this.pathToFileWithData = pathToSamples;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
    this.collectionQuantity = collectionQuantity;
    this.ignoreProperties = ignoreProperties;
  }

  private final String name;
  private final int collectionQuantity;
  private final String endpoint;
  private final String jsonPathToObject;
  private final String pathToFileWithData;
  private final String updatedFieldName;
  private String ignoreProperties;
  private final Object updatedFieldValue;
  private final Class<?> clazz;

  public String getEndpoint() {
    return endpoint;
  }

  public String getEndpointWithDefaultId() {
    return endpoint + "/" + getMockObject().getString(ID);
  }

  public String getEndpointWithId(String id) {
    return endpoint + "/" + id;
  }

  public String getUpdatedFieldName() {
    return updatedFieldName;
  }

  public Object getUpdatedFieldValue() {
    return updatedFieldValue;
  }

  public int getCollectionQuantity() {
    return collectionQuantity;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public String getPathToFileWithData() {
    return pathToFileWithData;
  }

  public JsonObject getMockObject() {
    return getMockObject(jsonPathToObject);
  }

  public JsonObject getMockObject(String jsonPath) {
    Supplier<JsonObject> jsonFromFile = () -> {
      try {
        return new JsonObject(getMockData(pathToFileWithData));
      } catch (IOException e) {
        return null;
      }
    };
    return (JsonObject) new JsonPathParser(jsonFromFile.get()).getValueAt(jsonPath);
  }

  private static String getEndpoint(Class<?> clazz) {
    return HelperUtils.getEndpoint(clazz);
  }

  public String getName() {
    return name;
  }

  public String getIgnoreProperties() {
    return ignoreProperties;
  }

}
