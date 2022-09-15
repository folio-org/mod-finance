package org.folio.services.transactions;

import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.util.ErrorCodes.INVALID_TRANSACTION_TYPE;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

public class BaseTransactionService implements TransactionService {
  private static final Logger logger = LogManager.getLogger(BaseTransactionService.class);
  private static final int MAX_FUND_PER_QUERY = 5;
  private static final String ALLOCATION_TYPE_TRANSACTIONS_QUERY = "(fiscalYearId==%s AND transactionType==%s) AND %s";
  private static final String AWAITING_PAYMENT_WITH_ENCUMBRANCE = "awaitingPayment.encumbranceId==%s";

  private final RestClient transactionRestClient;

  public BaseTransactionService(RestClient transactionRestClient) {
    this.transactionRestClient = transactionRestClient;
  }

  @Override
  public CompletableFuture<TransactionCollection> retrieveTransactions(String query, int offset, int limit, RequestContext requestContext) {
    return transactionRestClient.get(query, offset, limit, requestContext, TransactionCollection.class);
  }

  @Override
  public CompletableFuture<Transaction> retrieveTransactionById(String id, RequestContext requestContext) {
    return transactionRestClient.getById(id, requestContext, Transaction.class);
  }

  public CompletableFuture<Transaction> createTransaction(Transaction transaction, RequestContext requestContext) {
    return transactionRestClient.post(transaction, requestContext, Transaction.class);
  }

  public CompletableFuture<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    return transactionRestClient.put(transaction.getId(), transaction, requestContext);
  }

  public CompletableFuture<Void> deleteTransaction(Transaction transaction, RequestContext requestContext) {
    return transactionRestClient.delete(transaction.getId(), requestContext);
  }

  public void validateTransactionType(Transaction transaction, Transaction.TransactionType transactionType) {
    if (transaction.getTransactionType() != transactionType) {
      logger.info("Transaction {} type mismatch. {} expected", transaction.getId(), transactionType) ;
      Parameter parameter = new Parameter().withKey("expected").withValue(transactionType.name());
      throw new HttpException(422, INVALID_TRANSACTION_TYPE.toError().withParameters(Collections.singletonList(parameter)));
    }
  }

  public CompletableFuture<List<Transaction>> retrieveFromTransactions(List<String> fundIds, String fiscalYearId,
                                                                       Transaction.TransactionType trType, RequestContext requestContext) {
    return collectResultsOnSuccess(
            ofSubLists(new ArrayList<>(fundIds), MAX_FUND_PER_QUERY)
                    .map(ids -> retrieveAllocationTransactionsChunk(ids, fiscalYearId, trType, "fromFundId", requestContext)
                      .thenApply(transactions -> filterFundIdsByAllocationDirection(fundIds, transactions, Transaction::getToFundId)))
                    .toList()).thenApply(lists -> lists.stream().flatMap(Collection::stream).collect(Collectors.toList()));
  }

  public CompletableFuture<List<Transaction>> retrieveToTransactions(List<String> fundIds, String fiscalYearId,
                                                                     Transaction.TransactionType trType, RequestContext requestContext) {
    return collectResultsOnSuccess(
            ofSubLists(new ArrayList<>(fundIds), MAX_FUND_PER_QUERY)
                    .map(ids -> retrieveAllocationTransactionsChunk(ids, fiscalYearId, trType, "toFundId", requestContext)
                      .thenApply(transactions -> filterFundIdsByAllocationDirection(fundIds, transactions, Transaction::getFromFundId)))
                    .toList()).thenApply(lists -> lists.stream().flatMap(Collection::stream).collect(Collectors.toList()));
  }

  public CompletableFuture<Boolean> isConnectedToInvoice(String transactionId, RequestContext requestContext) {
    // We want to know if the order with the given encumbrance is connected to an invoice.
    // To avoid adding a dependency to mod-invoice, we check if there is a related awaitingPayment transaction
    String query = String.format(AWAITING_PAYMENT_WITH_ENCUMBRANCE, transactionId);
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(collection -> collection.getTotalRecords() > 0);
  }

  private CompletableFuture<List<Transaction>> retrieveAllocationTransactionsChunk(List<String> fundIds, String fiscalYearId,
                                                                                   Transaction.TransactionType trType, String allocationDirection,
                                                                                   RequestContext requestContext) {
    String fundQuery = convertIdsToCqlQuery(fundIds, allocationDirection, "==", " OR ");
    String query = String.format(ALLOCATION_TYPE_TRANSACTIONS_QUERY, fiscalYearId, trType.value(), fundQuery);
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext).thenApply(TransactionCollection::getTransactions);
  }

  private List<Transaction> filterFundIdsByAllocationDirection(List<String> fundIds, List<Transaction> transactions, Function<Transaction, String> allocationDirection) {
    return transactions.stream()
      .filter(transaction -> !fundIds.contains(allocationDirection.apply(transaction)))
      .collect(Collectors.toList());
  }
}
