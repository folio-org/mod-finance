package org.folio.services.transactions;

import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.util.ErrorCodes.INVALID_TRANSACTION_TYPE;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Future;

public class BaseTransactionService implements TransactionService {
  private static final Logger log = LogManager.getLogger();
  private static final int MAX_FUND_PER_QUERY = 5;
  private static final String ALLOCATION_TYPE_TRANSACTIONS_QUERY = "(fiscalYearId==%s AND %s) AND %s";
  private static final String AWAITING_PAYMENT_WITH_ENCUMBRANCE = "awaitingPayment.encumbranceId==%s";

  final RestClient restClient;

  public BaseTransactionService(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public Future<TransactionCollection> retrieveTransactions(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(TRANSACTIONS))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), TransactionCollection.class, requestContext);
  }

  @Override
  public Future<Transaction> retrieveTransactionById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(TRANSACTIONS, id), Transaction.class, requestContext);
  }

  public Future<Transaction> createTransaction(Transaction transaction, RequestContext requestContext) {
    return restClient.post(resourcesPath(TRANSACTIONS), transaction, Transaction.class, requestContext);
  }

  public Future<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    return restClient.put(resourceByIdPath(TRANSACTIONS, transaction.getId()), transaction, requestContext);
  }

  public Future<Void> deleteTransaction(Transaction transaction, RequestContext requestContext) {
    return restClient.delete(resourceByIdPath(TRANSACTIONS, transaction.getId()), requestContext);
  }

  public void validateTransactionType(Transaction transaction, Transaction.TransactionType transactionType) {
    if (transaction.getTransactionType() != transactionType) {
      log.info("Transaction {} type mismatch. {} expected", transaction.getId(), transactionType) ;
      Parameter parameter = new Parameter().withKey("expected").withValue(transactionType.name());
      throw new HttpException(422, INVALID_TRANSACTION_TYPE.toError().withParameters(Collections.singletonList(parameter)));
    }
  }

  public Future<List<Transaction>> retrieveFromTransactions(List<String> fundIds, String fiscalYearId,
      List<Transaction.TransactionType> trTypes, RequestContext requestContext) {
    return collectResultsOnSuccess(
            ofSubLists(new ArrayList<>(fundIds), MAX_FUND_PER_QUERY)
                    .map(ids -> retrieveAllocationTransactionsChunk(ids, fiscalYearId, trTypes, "fromFundId", requestContext)
                      .map(transactions -> filterFundIdsByAllocationDirection(fundIds, transactions, Transaction::getToFundId)))
                    .collect(Collectors.toList())).map(lists -> lists.stream().flatMap(Collection::stream).collect(Collectors.toList()));
  }

  public Future<List<Transaction>> retrieveToTransactions(List<String> fundIds, String fiscalYearId,
      List<Transaction.TransactionType> trTypes, RequestContext requestContext) {
    return collectResultsOnSuccess(
            ofSubLists(new ArrayList<>(fundIds), MAX_FUND_PER_QUERY)
                    .map(ids -> retrieveAllocationTransactionsChunk(ids, fiscalYearId, trTypes, "toFundId", requestContext)
                      .map(transactions -> filterFundIdsByAllocationDirection(fundIds, transactions, Transaction::getFromFundId)))
                    .collect(Collectors.toList())).map(lists -> lists.stream().flatMap(Collection::stream).collect(Collectors.toList()));
  }

  public Future<Boolean> isConnectedToInvoice(String transactionId, RequestContext requestContext) {
    // We want to know if the order with the given encumbrance is connected to an invoice.
    // To avoid adding a dependency to mod-invoice, we check if there is a related awaitingPayment transaction
    String query = String.format(AWAITING_PAYMENT_WITH_ENCUMBRANCE, transactionId);
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .map(collection -> collection.getTotalRecords() > 0);
  }

  private Future<List<Transaction>> retrieveAllocationTransactionsChunk(List<String> fundIds, String fiscalYearId,
      List<Transaction.TransactionType> trTypes, String allocationDirection, RequestContext requestContext) {
    String fundQuery = convertIdsToCqlQuery(fundIds, allocationDirection, "==", " OR ");
    List<String> trTypeValues = trTypes.stream().map(Transaction.TransactionType::value).collect(Collectors.toList());
    String trTypeQuery = convertIdsToCqlQuery(trTypeValues, "transactionType", "==", " OR ");
    String query = String.format(ALLOCATION_TYPE_TRANSACTIONS_QUERY, fiscalYearId, trTypeQuery, fundQuery);
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext).map(TransactionCollection::getTransactions);
  }

  private List<Transaction> filterFundIdsByAllocationDirection(List<String> fundIds, List<Transaction> transactions, Function<Transaction, String> allocationDirection) {
    return transactions.stream()
      .filter(transaction -> !fundIds.contains(allocationDirection.apply(transaction)))
      .collect(Collectors.toList());
  }
}
