package org.folio.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.Transaction;

public class LedgerFiscalYearTransactionsHolder {
  private final String fiscalYearId;
  private final Ledger ledger;
  private final List<Budget> ledgerBudgets;
  private final Set<String> ledgerFundIds;
  private List<Transaction> toAllocations;
  private List<Transaction> fromAllocations;
  private List<Transaction> toTransfers;
  private List<Transaction> fromTransfers;

  public LedgerFiscalYearTransactionsHolder( String fiscalYearId, Ledger ledger, List<Budget> ledgerBudgets) {
    this.fiscalYearId = fiscalYearId;
    this.ledger = ledger;
    this.ledgerBudgets = ledgerBudgets;
    this.ledgerFundIds = ledgerBudgets.stream().map(Budget::getFundId).collect(Collectors.toSet());
    this.toAllocations = new ArrayList<>();
    this.fromAllocations = new ArrayList<>();
    this.toTransfers = new ArrayList<>();
    this.fromTransfers = new ArrayList<>();
  }

  public LedgerFiscalYearTransactionsHolder withToAllocations(List<Transaction> allocations) {
    this.toAllocations = allocations;
    return this;
  }

  public LedgerFiscalYearTransactionsHolder withFromAllocations(List<Transaction> allocations) {
    this.fromAllocations = allocations;
    return this;
  }

  public LedgerFiscalYearTransactionsHolder withToTransfers(List<Transaction> transfers) {
    this.toTransfers = transfers;
    return this;
  }

  public LedgerFiscalYearTransactionsHolder withFromTransfers(List<Transaction> transfers) {
    this.fromTransfers = transfers;
    return this;
  }


  public String getFiscalYearId() {
    return fiscalYearId;
  }

  public Ledger getLedger() {
    return this.ledger;
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

    public List<Budget> getLedgerBudgets() {
    return ledgerBudgets;
  }

  public Set<String> getLedgerFundIds() {
    return ledgerFundIds;
  }
}
