package org.folio.rest.impl;

import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.Ledger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FundCodeExpenseClassesHolder {

  private BudgetExpenseClassCollection budgetExpenseClassCollection;
  private List<BudgetExpenseClass> budgetExpenseClassList;
  private List<String> fundIdList;
  private List<CompletableFuture<Fund>> fundListFuture;
  private List<CompletableFuture<Ledger>> ledgerListFuture;

  public BudgetExpenseClassCollection getBudgetExpenseClassCollection() {
    return budgetExpenseClassCollection;
  }

  public void setBudgetExpenseClassCollection(BudgetExpenseClassCollection budgetExpenseClassCollection) {
    this.budgetExpenseClassCollection = budgetExpenseClassCollection;
  }

  public List<BudgetExpenseClass> getBudgetExpenseClassList() {
    return budgetExpenseClassList;
  }

  public void setBudgetExpenseClassList(List<BudgetExpenseClass> budgetExpenseClassList) {
    this.budgetExpenseClassList = budgetExpenseClassList;
  }

  public List<String> getFundIdList() {
    return fundIdList;
  }

  public void setFundIdList(List<String> fundIdList) {
    this.fundIdList = fundIdList;
  }

  public List<CompletableFuture<Fund>> getFundListFuture() {
    return fundListFuture;
  }

  public void setFundListFuture(List<CompletableFuture<Fund>> fundListFuture) {
    this.fundListFuture = fundListFuture;
  }

  public List<CompletableFuture<Ledger>> getLedgerListFuture() {
    return ledgerListFuture;
  }

  public void setLedgerListFuture(List<CompletableFuture<Ledger>> ledgerListFuture) {
    this.ledgerListFuture = ledgerListFuture;
  }
  /*
  public List<BudgetExpenseClass> getBudgetExpenseClassCollection() {
    return budgetExpenseClassCollection;
  }

  public void setBudgetExpenseClassCollection(BudgetExpenseClassCollection budgetExpenseClassCollection) {
    this.budgetExpenseClassCollection = budgetExpenseClassCollection;
  } */
}
