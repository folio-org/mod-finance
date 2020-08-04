package org.folio.services;

import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

public class TransactionService {

  private final RestClient transactionRestClient;
  private final RestClient fiscalYearRestClient;

  public TransactionService(RestClient transactionRestClient, RestClient fiscalYearRestClient) {
    this.transactionRestClient = transactionRestClient;
    this.fiscalYearRestClient = fiscalYearRestClient;
  }

  public CompletableFuture<List<Transaction>> getTransactions(Budget budget, RequestContext requestContext) {
    String query = String.format("fromFundId==%s AND fiscalYearId==%s", budget.getFundId(), budget.getFiscalYearId());
    return transactionRestClient.get(query, 0, Integer.MAX_VALUE, requestContext, TransactionCollection.class)
      .thenApply(TransactionCollection::getTransactions);
  }

  public CompletableFuture<List<Transaction>> getTransactions(List<BudgetExpenseClass> budgetExpenseClasses, SharedBudget budget, RequestContext requestContext) {
    List<String> ids = budgetExpenseClasses.stream().map(BudgetExpenseClass::getExpenseClassId).collect(Collectors.toList());
    String query = String.format("fromFundId==%s AND fiscalYearId==%s AND %s", budget.getFundId(), budget.getFiscalYearId(), convertIdsToCqlQuery(ids, "expenseClassId", true));
    return transactionRestClient.get(query, 0, Integer.MAX_VALUE, requestContext, TransactionCollection.class)
      .thenApply(TransactionCollection::getTransactions);
  }

  public CompletableFuture<Transaction> createAllocationTransaction(Budget budget, RequestContext requestContext) {
    Transaction transaction = new Transaction().withAmount(budget.getAllocated())
      .withFiscalYearId(budget.getFiscalYearId()).withToFundId(budget.getFundId())
      .withTransactionType(Transaction.TransactionType.ALLOCATION).withSource(Transaction.Source.USER);
    return fiscalYearRestClient.getById(budget.getFiscalYearId(), requestContext, FiscalYear.class)
      .thenCompose(fiscalYear -> createTransaction(transaction.withCurrency(fiscalYear.getCurrency()), requestContext));
  }

  private CompletableFuture<Transaction> createTransaction(Transaction transaction, RequestContext requestContext) {
    return transactionRestClient.post(transaction, requestContext, Transaction.class);
  }

}
