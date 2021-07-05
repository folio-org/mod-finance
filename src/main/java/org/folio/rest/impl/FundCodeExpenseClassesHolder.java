package org.folio.rest.impl;

import org.folio.rest.acq.model.finance.FundCodeVsExpenseClassesType;
import org.folio.rest.acq.model.finance.FundCodeVsExpenseClassesTypeCollection;
import org.folio.rest.acq.model.finance.Ledger;
import org.folio.rest.jaxrs.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FundCodeExpenseClassesHolder {

  private List<Fund> fundList;
  private List<Ledger> ledgerList;
  private List<BudgetExpenseClass> budgetExpenseClassList;
  private BudgetsCollection budgetsCollection;
  private List<FundCodeVsExpenseClassesType> fundCodeVsExpenseClassesTypeList;
  private List<FiscalYear> fiscalYearList;

  public List<FiscalYear> getFiscalYearList() {
    return fiscalYearList;
  }

  public List<FiscalYear> setFiscalYearList(List<FiscalYear> fiscalYearList) {
    this.fiscalYearList = fiscalYearList;
    return fiscalYearList;
  }

  public List<BudgetExpenseClass> getBudgetExpenseClassList() {
    return budgetExpenseClassList;
  }

  public FundCodeVsExpenseClassesTypeCollection setBudgetExpenseClassList(List<BudgetExpenseClass> budgetExpenseClassList) {
    this.budgetExpenseClassList = budgetExpenseClassList;
    //return budgetsCollection; BudgetsCollection
   return  new FundCodeVsExpenseClassesTypeCollection();
  }

  public void addBudgetExpenseClass(BudgetExpenseClass budgetExpenseClass) {
    budgetExpenseClassList.add(budgetExpenseClass);
  }

  public BudgetsCollection setBudgetCollectionList(BudgetsCollection budgetCollectionList) {
    this.budgetsCollection = budgetsCollection;
    return budgetsCollection;
  }

  public BudgetsCollection getBudgetCollection() {
    return budgetsCollection;
  }

  public List<Fund> getFundList() {
    return fundList;
  }

  public List<Fund> setFundList(List<Fund> fundList) {
    this.fundList = fundList;
    return fundList;
  }

  public List<Ledger> getLedgerList() {
    return ledgerList;
  }

  public List<Ledger> setLedgerList(List<Ledger> ledgerList) {
    this.ledgerList = ledgerList;
    return ledgerList;
  }

  public List<FundCodeVsExpenseClassesType> getFundCodeVsExpenseClassesTypeList() {
    return fundCodeVsExpenseClassesTypeList;
  }

  public void setFundCodeVsExpenseClassesTypeList(List<FundCodeVsExpenseClassesType> fundCodeVsExpenseClassesTypeList) {
    this.fundCodeVsExpenseClassesTypeList = fundCodeVsExpenseClassesTypeList;
  }

  public BudgetsCollection getBudgetsCollection() {
    return budgetsCollection;
  }

  public BudgetsCollection setBudgetsCollection(BudgetsCollection budgetsCollection) {
    this.budgetsCollection = budgetsCollection;

    return budgetsCollection;
  }

  public List<FundCodeVsExpenseClassesType> getFundCodeVsExpenseClassesType() {
    Map<String, Ledger> ledgerIdVsLedgerMap = new HashMap<>();
    FundCodeVsExpenseClassesType fundCodeVsExpenseClassesType = new FundCodeVsExpenseClassesType();
    List<Ledger> ledgerList = getLedgerList();
    for (Ledger ledger : ledgerList) {
      ledgerIdVsLedgerMap.put(ledger.getId(), ledger);
    }
    List<Fund> fundList = getFundList();
    for (Ledger ledger : ledgerList) {
      for (Fund fund : fundList) {
        if (ledger.getId() == fund.getLedgerId()) {
          fundCodeVsExpenseClassesType.setFundCode(fund.getCode());
          fundCodeVsExpenseClassesType.setLedgerCode(ledgerIdVsLedgerMap.get(fund.getLedgerId()).getCode());
          fundCodeVsExpenseClassesType.setActiveFundCodeVsExpClasses(getActiveStatusBudgetExpenseClass(fund));
          fundCodeVsExpenseClassesType.setInactiveFundCodeVsExpClasses(getInActiveStatusBudgetExpenseClass(fund));
          fundCodeVsExpenseClassesTypeList.add(fundCodeVsExpenseClassesType);
        }
      }
    }
    return fundCodeVsExpenseClassesTypeList;
  }

  public List<String> getActiveStatusBudgetExpenseClass(Fund fund) {
    List<BudgetExpenseClass> budgetExpenseClassList = getBudgetExpenseClassList();
    List<Budget> budgetList = getBudgetCollection().getBudgets();
    List<String> activeStatus = new ArrayList<>();
    List<Budget> budgetListByFundId = new ArrayList<>();
    for (Budget budget : budgetList) {
      if (budget.getFundId() == fund.getId()) {
        budgetListByFundId.add(budget);
      }
    }
    for (Budget budget : budgetListByFundId) {
      for (BudgetExpenseClass budgetExpenseClass : budgetExpenseClassList) {
        if (budget.getId() == budgetExpenseClass.getBudgetId()) {
          if (budgetExpenseClass.getStatus().equals(BudgetExpenseClass.Status.fromValue("Active"))) {
            activeStatus.add(budgetExpenseClass.getStatus().toString());
          }
        }
      }
    }
    return activeStatus;
  }

  public List<String> getInActiveStatusBudgetExpenseClass(Fund fund) {
    List<BudgetExpenseClass> budgetExpenseClassList = getBudgetExpenseClassList();
    List<Budget> budgetList = getBudgetCollection().getBudgets();
    List<String> inActiveStatus = new ArrayList<>();
    List<Budget> budgetListByFundId = new ArrayList<>();
    for (Budget budget : budgetList) {
      if (budget.getFundId() == fund.getId()) {
        budgetListByFundId.add(budget);
      }
    }
    for (Budget budget : budgetListByFundId) {
      for (BudgetExpenseClass budgetExpenseClass : budgetExpenseClassList) {
        if (budget.getId() == budgetExpenseClass.getBudgetId()) {
          if (budgetExpenseClass.getStatus().equals(BudgetExpenseClass.Status.fromValue("Inactive"))) {
            inActiveStatus.add(budgetExpenseClass.getStatus().toString());
          }
        }
      }
    }
    return inActiveStatus;
  }

  public FundCodeVsExpenseClassesTypeCollection getFundCodeVsExpenseClassesTypeCollection() {
    FundCodeVsExpenseClassesTypeCollection fundCodeVsExpenseClassesTypeCollection = new FundCodeVsExpenseClassesTypeCollection();
    List<FundCodeVsExpenseClassesType> fundCodeVsExpenseClassesTypeList = getFundCodeVsExpenseClassesType();
    fundCodeVsExpenseClassesTypeCollection.setDelimiter(":");
    fundCodeVsExpenseClassesTypeCollection.setFundCodeVsExpClassesTypes(fundCodeVsExpenseClassesTypeList);
    return fundCodeVsExpenseClassesTypeCollection;
  }
}
