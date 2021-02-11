package org.folio.models;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.Transaction;

public class GroupFiscalYearTransactionsHolder {
  private final GroupFiscalYearSummary groupFiscalYearSummary;
  private List<String> groupFundIds;
  private List<Transaction> toAllocations;
  private List<Transaction> fromAllocations;
  private List<Transaction> toTransfers;
  private List<Transaction> fromTransfers;

  public GroupFiscalYearTransactionsHolder(GroupFiscalYearSummary groupFiscalYearSummary) {
    this.groupFiscalYearSummary = groupFiscalYearSummary;
    this.toAllocations = new ArrayList<>();
    this.fromAllocations = new ArrayList<>();
    this.toTransfers = new ArrayList<>();
    this.fromTransfers = new ArrayList<>();
  }

  public GroupFiscalYearTransactionsHolder withToAllocations(List<Transaction> allocations) {
    this.toAllocations = allocations;
    return this;
  }

  public GroupFiscalYearTransactionsHolder withFromAllocations(List<Transaction> allocations) {
    this.fromAllocations = allocations;
    return this;
  }

  public GroupFiscalYearTransactionsHolder withToTransfers(List<Transaction> transfers) {
    this.toTransfers = transfers;
    return this;
  }

  public GroupFiscalYearTransactionsHolder withFromTransfers(List<Transaction> transfers) {
    this.fromTransfers = transfers;
    return this;
  }

  public GroupFiscalYearTransactionsHolder withGroupFundIds(List<String> groupFundIds) {
    this.groupFundIds = groupFundIds;
    return this;
  }

  public GroupFiscalYearSummary getGroupFiscalYearSummary() {
    return groupFiscalYearSummary;
  }

  public List<Transaction> getFromAllocations() {
    return fromAllocations;
  }

  public List<Transaction> getToAllocations() {
    return toAllocations;
  }

  public List<Transaction> getToTransfers() {
    return toTransfers;
  }

  public List<Transaction> getFromTransfers() {
    return fromTransfers;
  }

  public List<String> getGroupFundIds() {
    return groupFundIds;
  }
}
