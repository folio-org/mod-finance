package org.folio.services.transactions;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionTotal;
import org.folio.rest.jaxrs.model.TransactionTotalCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTION_TOTALS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

@Log4j2
public class TransactionTotalService {

  private static final int MAX_FUND_PER_QUERY = 5;

  private final RestClient restClient;

  public TransactionTotalService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<List<TransactionTotal>> getTransactionsFromFunds(List<String> fundIds, String fiscalYearId,
                                                                 List<Transaction.TransactionType> trTypes, RequestContext requestContext) {
    return getTransactionsFromOrToFunds(fundIds, fiscalYearId, trTypes, "from", requestContext);
  }

  public Future<List<TransactionTotal>> getTransactionsToFunds(List<String> fundIds, String fiscalYearId,
                                                               List<Transaction.TransactionType> trTypes, RequestContext requestContext) {
    return getTransactionsFromOrToFunds(fundIds, fiscalYearId, trTypes, "to", requestContext);
  }

  private Future<List<TransactionTotal>> getTransactionsFromOrToFunds(List<String> fundIds, String fiscalYearId,
                                                                      List<Transaction.TransactionType> trTypes, String direction, RequestContext requestContext) {
    return collectResultsOnSuccess(ofSubLists(new ArrayList<>(fundIds), MAX_FUND_PER_QUERY)
      .map(ids -> getTransactionsByFundChunk(ids, fiscalYearId, trTypes, direction, requestContext)
        .map(transactions -> filterFundIdsByAllocationDirection(fundIds, transactions, direction)))
      .toList())
      .map(lists -> lists.stream().flatMap(Collection::stream).toList());
  }

  private List<TransactionTotal> filterFundIdsByAllocationDirection(List<String> fundIds, List<TransactionTotal> transactions,
                                                                    String direction) {
    // Note that here getToFundId() is used when direction is from (a negation is used afterward)
    Function<TransactionTotal, String> getFundId = "from".equals(direction) ? TransactionTotal::getToFundId : TransactionTotal::getFromFundId;
    return transactions.stream()
      .filter(transaction -> !fundIds.contains(getFundId.apply(transaction)))
      .toList();
  }

  private Future<List<TransactionTotal>> getTransactionsByFundChunk(List<String> fundIds, String fiscalYearId,
                                                                    List<Transaction.TransactionType> trTypes, String direction, RequestContext requestContext) {
    String fundIdField = "from".equals(direction) ? "fromFundId" : "toFundId";
    String fundQuery = convertIdsToCqlQuery(fundIds, fundIdField, "==", " OR ");
    List<String> trTypeValues = trTypes.stream().map(Transaction.TransactionType::value).toList();
    String trTypeQuery = convertIdsToCqlQuery(trTypeValues, "transactionType", "==", " OR ");
    String query = String.format("(fiscalYearId==%s AND %s) AND %s", fiscalYearId, trTypeQuery, fundQuery);
    return getAllTransactionsByQuery(query, requestContext);
  }

  public Future<List<TransactionTotal>> getAllTransactionsByQuery(String query, RequestContext requestContext) {
    log.info("getAllTransactionsByQuery:: Query: {}", query);
    return getTransactionCollectionByQuery(query, 0, Integer.MAX_VALUE, requestContext)
      .map(TransactionTotalCollection::getTransactionTotals);
  }

  public Future<TransactionTotalCollection> getTransactionCollectionByQuery(String query, int offset, int limit,
                                                                            RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(TRANSACTION_TOTALS))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), TransactionTotalCollection.class, requestContext);
  }
}
