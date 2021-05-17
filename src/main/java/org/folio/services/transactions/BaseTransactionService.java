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
  private static final String TRANSACTION_TO_QUERY = "(fiscalYearId==%s AND transactionType==%s) AND %s AND ((cql.allRecords=1 NOT fromFundId==\"\") OR %s)";
  private static final String TRANSACTION_FROM_QUERY = "(fiscalYearId==%s AND transactionType==%s) AND %s AND ((cql.allRecords=1 NOT toFundId==\"\") OR %s)";

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
                    .map(ids -> retrieveFromTransactionsChunk(ids, fiscalYearId, trType, requestContext))
                    .toList()).thenApply(lists -> lists.stream().flatMap(Collection::stream).collect(Collectors.toList()));
  }

  public CompletableFuture<List<Transaction>> retrieveToTransactions(List<String> fundIds, String fiscalYearId,
                                                                     Transaction.TransactionType trType, RequestContext requestContext) {
    return collectResultsOnSuccess(
            ofSubLists(new ArrayList<>(fundIds), MAX_FUND_PER_QUERY)
                    .map(ids -> retrieveToTransactionsChunk(ids, fiscalYearId, trType, requestContext))
                    .toList()).thenApply(lists -> lists.stream().flatMap(Collection::stream).collect(Collectors.toList()));
  }

  private CompletableFuture<List<Transaction>> retrieveToTransactionsChunk(List<String> fundIds, String fiscalYearId,
                                                                      Transaction.TransactionType trType, RequestContext requestContext) {
    String toFundQuery = convertIdsToCqlQuery(fundIds, "toFundId", "==", " OR ");
    String fromFundQuery = convertIdsToCqlQuery(fundIds, "fromFundId", "<>", " AND ");
    String query = String.format(TRANSACTION_TO_QUERY, fiscalYearId, trType.value(), toFundQuery, fromFundQuery);
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext).thenApply(TransactionCollection::getTransactions);
  }

  private CompletableFuture<List<Transaction>> retrieveFromTransactionsChunk(List<String> fundIds, String fiscalYearId,
                                                                        Transaction.TransactionType trType, RequestContext requestContext) {
    String fromFundQuery = convertIdsToCqlQuery(fundIds, "fromFundId", "==", " OR ");
    String toFundQuery = convertIdsToCqlQuery(fundIds, "toFundId", "<>", " AND ");
    String query = String.format(TRANSACTION_FROM_QUERY, fiscalYearId, trType.value(), fromFundQuery, toFundQuery);
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext).thenApply(TransactionCollection::getTransactions);
  }
}
