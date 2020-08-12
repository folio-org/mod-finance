package org.folio.services;

import static java.util.stream.Collectors.toList;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
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

  public CompletableFuture<TransactionCollection> getTransactions(String query, int offset, int limit, RequestContext requestContext) {
    return transactionRestClient.get(query, offset, limit, requestContext, TransactionCollection.class);
  }

  public CompletableFuture<List<Transaction>> getTransactions(Budget budget, RequestContext requestContext) {
    String query = String.format("fromFundId==%s AND fiscalYearId==%s", budget.getFundId(), budget.getFiscalYearId());
    return getTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(TransactionCollection::getTransactions);
  }

  public CompletableFuture<List<Transaction>> getTransactions(List<BudgetExpenseClass> budgetExpenseClasses, SharedBudget budget, RequestContext requestContext) {
    List<String> ids = budgetExpenseClasses.stream().map(BudgetExpenseClass::getExpenseClassId).collect(Collectors.toList());
    String query = String.format("fromFundId==%s AND fiscalYearId==%s AND %s", budget.getFundId(), budget.getFiscalYearId(), convertIdsToCqlQuery(ids, "expenseClassId", true));
    return getTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(TransactionCollection::getTransactions);
  }

  public CompletableFuture<List<Transaction>> getTransactionsByFundIds(List<String> fundIds, String fiscalYearId, RequestContext requestContext) {
    List<CompletableFuture<List<Transaction>>> futures = StreamEx
      .ofSubLists(fundIds, MAX_IDS_FOR_GET_RQ)
      .map(ids ->  getTransactionsChunk(buildGetTransactionsQuery(fiscalYearId, ids), requestContext))
      .collect(toList());

    return collectResultsOnSuccess(futures)
      .thenApply(listList -> listList.stream().flatMap(Collection::stream).collect(toList()));
  }

  private CompletableFuture<List<Transaction>> getTransactionsChunk(String query, RequestContext requestContext) {
    return getTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(TransactionCollection::getTransactions);
  }

  private String buildGetTransactionsQuery(String fiscalYearId, List<String> fundIds) {
    return String.format("fiscalYearId==%s AND ((transactionType==Payment AND %s) OR (transactionType==Credit AND %s))",
      fiscalYearId,
      convertIdsToCqlQuery(fundIds, "fromFundId", true),
      convertIdsToCqlQuery(fundIds, "toFundId", true));
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
