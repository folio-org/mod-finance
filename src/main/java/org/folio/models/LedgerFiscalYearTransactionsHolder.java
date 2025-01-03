package org.folio.models;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.TransactionTotal;

public class LedgerFiscalYearTransactionsHolder {
  private final String fiscalYearId;
  private final Ledger ledger;
  private final List<Budget> ledgerBudgets;
  private final List<String> ledgerFundIds;
  private List<TransactionTotal> toAllocations;
  private List<TransactionTotal> fromAllocations;
  private List<TransactionTotal> toTransfers;
  private List<TransactionTotal> fromTransfers;

  public LedgerFiscalYearTransactionsHolder( String fiscalYearId, Ledger ledger, List<Budget> ledgerBudgets) {
    this.fiscalYearId = fiscalYearId;
    this.ledger = ledger;
    this.ledgerBudgets = ledgerBudgets;
    this.ledgerFundIds = ledgerBudgets.stream().map(Budget::getFundId).collect(Collectors.toList());
    this.toAllocations = new ArrayList<>();
    this.fromAllocations = new ArrayList<>();
    this.toTransfers = new ArrayList<>();
    this.fromTransfers = new ArrayList<>();
  }

  public LedgerFiscalYearTransactionsHolder withToAllocations(List<TransactionTotal> allocations) {
    this.toAllocations = allocations;
    return this;
  }

  public LedgerFiscalYearTransactionsHolder withFromAllocations(List<TransactionTotal> allocations) {
    this.fromAllocations = allocations;
    return this;
  }

  public LedgerFiscalYearTransactionsHolder withToTransfers(List<TransactionTotal> transfers) {
    this.toTransfers = transfers;
    return this;
  }

  public LedgerFiscalYearTransactionsHolder withFromTransfers(List<TransactionTotal> transfers) {
    this.fromTransfers = transfers;
    return this;
  }

  public String getFiscalYearId() {
    return fiscalYearId;
  }

  public Ledger getLedger() {
    return this.ledger;
  }

  public List<TransactionTotal> getFromAllocations() {
    return fromAllocations;
  }

  public List<TransactionTotal> getToAllocations() {
    return toAllocations;
  }

  public List<TransactionTotal> getToTransfers() {
    return toTransfers;
  }

  public List<TransactionTotal> getFromTransfers() {
    return fromTransfers;
  }

  public List<Budget> getLedgerBudgets() {
    return ledgerBudgets;
  }

  public List<String> getLedgerFundIds() {
    return ledgerFundIds;
  }
}
