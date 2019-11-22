package org.folio.rest.util;

import static org.folio.rest.impl.ApiTestBase.getMockData;
import static org.folio.rest.util.HelperUtils.ID;

import java.io.IOException;
import java.util.function.Supplier;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.OrderTransactionSummary;
import org.folio.rest.jaxrs.resource.FinanceBudgets;
import org.folio.rest.jaxrs.resource.FinanceFiscalYears;
import org.folio.rest.jaxrs.resource.FinanceFundTypes;
import org.folio.rest.jaxrs.resource.FinanceFunds;
import org.folio.rest.jaxrs.resource.FinanceGroupFundFiscalYears;
import org.folio.rest.jaxrs.resource.FinanceGroups;
import org.folio.rest.jaxrs.resource.FinanceLedgers;
import org.folio.rest.jaxrs.resource.FinanceOrderTransactionSummaries;
import org.folio.rest.jaxrs.resource.Finance;
import org.folio.rest.tools.parser.JsonPathParser;

import io.vertx.core.json.JsonObject;

public enum TestEntities {
  BUDGET("budgets", getEndpoint(FinanceBudgets.class), Budget.class, "mockdata/budgets/budgets.json", "budgets[0]", "name", "Updated name", 1),
  FUND("funds", getEndpoint(FinanceFunds.class), Fund.class, "mockdata/funds/funds.json", "funds[0]", "name", "History", 1),
  FUND_TYPE("fundTypes", getEndpoint(FinanceFundTypes.class), FundType.class, "mockdata/fund-types/types.json", "fundTypes[0]", "name", "New type name", 1),
  GROUP_FUND_FISCAL_YEAR("groupFundFiscalYears", getEndpoint(FinanceGroupFundFiscalYears.class), GroupFundFiscalYear.class, "mockdata/group-fund-fiscal-years/group_fund_fiscal_years.json", "groupFundFiscalYears[0]", "allocated", 10000, 1),
  FISCAL_YEAR("fiscalYears", getEndpoint(FinanceFiscalYears.class), FiscalYear.class, "mockdata/fiscal-years/fiscal_years.json", "fiscalYears[0]", "code", "FY2020", 1),
  LEDGER("ledgers", getEndpoint(FinanceLedgers.class), Ledger.class, "mockdata/ledgers/ledgers.json", "ledgers[0]", "ledgerStatus", "Active", 1),
  GROUP("groups", getEndpoint(FinanceGroups.class), Group.class, "mockdata/groups/groups.json", "groups[0]", "status", "Frozen", 1),
  TRANSACTIONS("Transaction", getEndpoint(Finance.class) + "/transactions", Transaction.class, "mockdata/transactions/transactions.json", "transactions[0]", "amount", 25, 1),
  TRANSACTIONS_ALLOCATION("Allocation", getEndpoint(Finance.class) + "/allocations", Transaction.class, "mockdata/transactions/allocations.json", "transactions[0]", "amount", 25, 1),
  TRANSACTIONS_TRANSFER("Transfer", getEndpoint(Finance.class) + "/transfers", Transaction.class, "mockdata/transactions/transfers.json", "transactions[0]", "amount", 25, 1),
  TRANSACTIONS_ENCUMBRANCE("Encumbrance", getEndpoint(Finance.class) + "/encumbrances", Transaction.class, "mockdata/transactions/encumbrances.json", "transactions[0]", "amount", 25, 1),
  ORDER_TRANSACTION_SUMMARY("orderTransactionSummary", getEndpoint(FinanceOrderTransactionSummaries.class), OrderTransactionSummary.class, "mockdata/transaction-summaries/order_transaction_summary.json", "", null, null, 1);

  TestEntities(String name, String endpoint, Class clazz, String pathToSamples, String jsonPathToSample, String updatedFieldName,
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

  private final String name;
  private int collectionQuantity;
  private String endpoint;
  private String jsonPathToObject;
  private String pathToFileWithData;
  private String updatedFieldName;
  private Object updatedFieldValue;
  private Class clazz;

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
}
