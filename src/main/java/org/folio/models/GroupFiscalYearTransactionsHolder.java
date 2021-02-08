package org.folio.models;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.Transaction;

public class GroupFiscalYearTransactionsHolder {
  private final GroupFiscalYearSummary groupFiscalYearSummary;
  private List<String> groupFundIds;
  private List<Transaction> toTransactions;
  private List<Transaction> fromTransactions;

  public GroupFiscalYearTransactionsHolder(GroupFiscalYearSummary groupFiscalYearSummary) {
    this.groupFiscalYearSummary = groupFiscalYearSummary;
    this.toTransactions = new ArrayList<>();
    this.fromTransactions = new ArrayList<>();
  }

  public void addToTransaction(Transaction toTransaction) {
    this.toTransactions.add(toTransaction);
  }

  public void addFromTransaction(Transaction fromTransaction) {
    this.fromTransactions.add(fromTransaction);
  }

  public GroupFiscalYearTransactionsHolder withToTransactions(List<Transaction> toTransactions) {
    this.toTransactions = toTransactions;
    return this;
  }

  public GroupFiscalYearTransactionsHolder withFromTransactions(List<Transaction> fromTransactions) {
    this.fromTransactions = fromTransactions;
    return this;
  }

  public GroupFiscalYearTransactionsHolder withGroupFundIds(List<String> groupFundIds) {
    this.groupFundIds = groupFundIds;
    return this;
  }

  public GroupFiscalYearSummary getGroupFiscalYearSummary() {
    return groupFiscalYearSummary;
  }

  public List<Transaction> getFromTransactions() {
    return fromTransactions;
  }

  public List<Transaction> getToTransactions() {
    return toTransactions;
  }

  public List<String> getGroupFundIds() {
    return groupFundIds;
  }
}
