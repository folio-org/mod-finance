package org.folio.models;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundCodeExpenseClassesCollection;
import org.folio.rest.jaxrs.model.FundCodeVsExpClassesType;
import org.folio.rest.jaxrs.model.Ledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


public class FundCodeExpenseClassesHolder {

  private List<Fund> fundList = new ArrayList<>();
  private List<Ledger> ledgerList = new ArrayList<>();
  private List<BudgetExpenseClass> budgetExpenseClassList = new ArrayList<>();
  private List<FundCodeVsExpClassesType> fundCodeVsExpenseClassesTypeList = new ArrayList<>();
  private List<String> budgetIds = new ArrayList<>();
  private BudgetsCollection budgetsCollection;
  private List<ExpenseClass> expenseClassList = new ArrayList<>();
  private FiscalYear fiscalYear;

  public FundCodeExpenseClassesHolder withExpenseClassList(List<ExpenseClass> expenseClassList) {
    this.expenseClassList = expenseClassList;
    return this;
  }

  public List<ExpenseClass> getExpenseClassList() {
    return expenseClassList;
  }

  public FundCodeExpenseClassesHolder withBudgetIds(List<String> budgetIds) {
    this.budgetIds = budgetIds;
    return this;
  }

  public List<String> getBudgetIds() {
    return budgetIds;
  }

  public List<BudgetExpenseClass> getBudgetExpenseClassList() {
    return budgetExpenseClassList;
  }

  public FundCodeExpenseClassesHolder withBudgetExpenseClassList(List<BudgetExpenseClass> budgetExpenseClassList) {
    this.budgetExpenseClassList = budgetExpenseClassList;
    return this;
  }

  public void addBudgetExpenseClass(BudgetExpenseClass budgetExpenseClass) {
    budgetExpenseClassList.add(budgetExpenseClass);
  }

  public FundCodeExpenseClassesHolder withBudgetCollectionList(BudgetsCollection budgetCollection) {
    this.budgetsCollection = budgetCollection;
    return this;
  }

  public BudgetsCollection getBudgetCollection() {
    return budgetsCollection;
  }

  public List<Fund> getFundList() {
    return fundList;
  }

  public FundCodeExpenseClassesHolder withFundList(List<Fund> fundList) {
    this.fundList = fundList;
    return this;
  }

  public List<Ledger> getLedgerList() {
    return ledgerList;
  }

  public FundCodeExpenseClassesHolder withLedgerList(List<Ledger> ledgerList) {
    this.ledgerList = ledgerList;
    return this;
  }

  public FundCodeExpenseClassesHolder setFundCodeVsExpenseClassesTypeList(List<FundCodeVsExpClassesType> fundCodeVsExpenseClassesTypeList) {
    this.fundCodeVsExpenseClassesTypeList = fundCodeVsExpenseClassesTypeList;
    return this;
  }

  public BudgetsCollection getBudgetsCollection() {
    return budgetsCollection;
  }

  public FundCodeExpenseClassesHolder withBudgetsCollection(BudgetsCollection budgetsCollection) {
    this.budgetsCollection = budgetsCollection;
    return this;
  }

  public FundCodeExpenseClassesCollection buildFundCodeVsExpenseClassesTypeCollection() {
    Map<String, Ledger> ledgerIdVsLedgerMap = new HashMap<>();
    List<Ledger> ledgerList = getLedgerList();
    for (Ledger ledger : ledgerList) {
      ledgerIdVsLedgerMap.put(ledger.getId(), ledger);
    }
    List<Fund> fundList = getFundList();
    for (Ledger ledger : ledgerList) {
      for (Fund fund : fundList) {
        if (Objects.equals(ledger.getId(), fund.getLedgerId())) {
          FundCodeVsExpClassesType fundCodeVsExpenseClassesType = new FundCodeVsExpClassesType();
          fundCodeVsExpenseClassesType.setFundCode(fund.getCode());
          fundCodeVsExpenseClassesType.setLedgerCode(ledgerIdVsLedgerMap.get(fund.getLedgerId()).getCode());
          fundCodeVsExpenseClassesType.setActiveFundCodeVsExpClasses(getActiveStatusBudgetExpenseClass(fund));
          fundCodeVsExpenseClassesType.setInactiveFundCodeVsExpClasses(getInActiveStatusBudgetExpenseClass(fund));
          fundCodeVsExpenseClassesTypeList.add(fundCodeVsExpenseClassesType);
        }
      }
    }
    return getFundCodeVsExpenseClassesTypeCollection(fundCodeVsExpenseClassesTypeList);
  }

  private List<String> getActiveStatusBudgetExpenseClass(Fund fund) {
    List<BudgetExpenseClass> budgetExpenseClassList = getBudgetExpenseClassList();
    List<Budget> budgetList = getBudgetCollection().getBudgets();
    List<String> activeStatus = new ArrayList<>();
    List<Budget> budgetListByFundId = new ArrayList<>();
    for (Budget budget : budgetList) {
      if (budget.getFundId().equals(fund.getId())) {
        budgetListByFundId.add(budget);
      }
    }
    for (Budget budget : budgetListByFundId) {
      for (BudgetExpenseClass budgetExpenseClass : budgetExpenseClassList) {
        if (budget.getId().equals(budgetExpenseClass.getBudgetId())) {
          if (budgetExpenseClass.getStatus().equals(BudgetExpenseClass.Status.fromValue("Active"))) {
            activeStatus.add(addFundCodeAndExpanseClassCode(budgetExpenseClass, fund));
          }
        }
      }
    }
    return activeStatus;
  }

  private String addFundCodeAndExpanseClassCode(BudgetExpenseClass budgetExpenseClass, Fund fund) {
    for (ExpenseClass expenseClass : getExpenseClassList()) {
      if (Objects.equals(budgetExpenseClass.getExpenseClassId(), expenseClass.getId())) {
        return fund.getCode() + ":" + expenseClass.getCode();
      }
    }
    return null;
  }

  private List<String> getInActiveStatusBudgetExpenseClass(Fund fund) {
    List<BudgetExpenseClass> budgetExpenseClassList = getBudgetExpenseClassList();
    List<Budget> budgetList = getBudgetCollection().getBudgets();
    List<String> inActiveStatus = new ArrayList<>();
    List<Budget> budgetListByFundId = new ArrayList<>();
    for (Budget budget : budgetList) {
      if (budget.getFundId().equals(fund.getId())) {
        budgetListByFundId.add(budget);
      }
    }
    for (Budget budget : budgetListByFundId) {
      for (BudgetExpenseClass budgetExpenseClass : budgetExpenseClassList) {
        if (budget.getId().equals(budgetExpenseClass.getBudgetId())) {
          if (budgetExpenseClass.getStatus().equals(BudgetExpenseClass.Status.fromValue("Inactive"))) {
            inActiveStatus.add(addFundCodeAndExpanseClassCode(budgetExpenseClass, fund));
          }
        }
      }
    }
    return inActiveStatus;
  }

  private FundCodeExpenseClassesCollection getFundCodeVsExpenseClassesTypeCollection(List<FundCodeVsExpClassesType> fundCodeVsExpenseClassesTypeList) {
    FundCodeExpenseClassesCollection fundCodeVsExpenseClassesTypeCollection = new FundCodeExpenseClassesCollection();
    fundCodeVsExpenseClassesTypeCollection.setDelimiter(":");
    fundCodeVsExpenseClassesTypeCollection.setFundCodeVsExpClassesTypes(fundCodeVsExpenseClassesTypeList);
    return fundCodeVsExpenseClassesTypeCollection;
  }

  public CompletableFuture<FiscalYear> getFiscalYearFuture() {
    CompletableFuture<FiscalYear> f2 = CompletableFuture.supplyAsync(() -> fiscalYear);
    return f2;
  }

  public FiscalYear getFiscalYear() {
    return fiscalYear;
  }

  public FundCodeExpenseClassesHolder setFiscalYear(FiscalYear fiscalYear) {
    this.fiscalYear = fiscalYear;
    return this;
  }
}
