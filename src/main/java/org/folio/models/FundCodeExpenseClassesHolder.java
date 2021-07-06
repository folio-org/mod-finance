package org.folio.models;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundCodeVsExpClassesType;
import org.folio.rest.jaxrs.model.Ledger;

public class FundCodeExpenseClassesHolder {

  private List<Fund> fundList = new ArrayList<>();;
  private List<Ledger> ledgerList = new ArrayList<>();;
  private List<BudgetExpenseClass> budgetExpenseClassList = new ArrayList<>();
  private List<FundCodeVsExpClassesType> fundCodeVsExpenseClassesTypeList = new ArrayList<>();;
  private List<FiscalYear> fiscalYearList = new ArrayList<>();;
  private BudgetsCollection budgetsCollection;

  public List<FiscalYear> getFiscalYearList() {
    return fiscalYearList;
  }

  public FundCodeExpenseClassesHolder withFiscalYearList(List<FiscalYear> fiscalYearList) {
    this.fiscalYearList = fiscalYearList;
    return this;
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

  public FundCodeExpenseClassesHolder withBudgetCollectionList(BudgetsCollection budgetCollectionList) {
    this.budgetsCollection = budgetCollectionList;
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

  public List<Ledger> setLedgerList(List<Ledger> ledgerList) {
    this.ledgerList = ledgerList;
    return ledgerList;
  }

  public List<FundCodeVsExpClassesType> getFundCodeVsExpenseClassesTypeList() {
    return fundCodeVsExpenseClassesTypeList;
  }

  public FundCodeExpenseClassesHolder addFundCodeVsExpenseClassesType(FundCodeVsExpClassesType fundCodeVsExpenseClassesType) {
    this.fundCodeVsExpenseClassesTypeList.add(fundCodeVsExpenseClassesType);
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
}
