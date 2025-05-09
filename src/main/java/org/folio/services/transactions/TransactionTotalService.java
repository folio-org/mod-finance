package org.folio.services.transactions;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;
import org.folio.models.TransactionTotalSetting;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
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
                                                                 List<TransactionTotal.TransactionType> trTypes, RequestContext requestContext) {
    return getTransactionsFromOrToFunds(fundIds, fiscalYearId, trTypes, TransactionTotalSetting.FROM_FUND_ID, requestContext);
  }

  public Future<List<TransactionTotal>> getTransactionsToFunds(List<String> fundIds, String fiscalYearId,
                                                               List<TransactionTotal.TransactionType> trTypes, RequestContext requestContext) {
    return getTransactionsFromOrToFunds(fundIds, fiscalYearId, trTypes, TransactionTotalSetting.TO_FUND_ID, requestContext);
  }

  private Future<List<TransactionTotal>> getTransactionsFromOrToFunds(List<String> fundIds, String fiscalYearId, List<TransactionTotal.TransactionType> trTypes,
                                                                      TransactionTotalSetting setting, RequestContext requestContext) {
    return collectResultsOnSuccess(ofSubLists(new ArrayList<>(fundIds), MAX_FUND_PER_QUERY)
      .map(ids -> getTransactionsByFundChunk(ids, fiscalYearId, trTypes, setting, requestContext)
        .map(transactions -> filterFundIdsByAllocationDirection(fundIds, transactions, setting)))
      .toList())
      .map(lists -> lists.stream().flatMap(Collection::stream).toList());
  }

  private Future<List<TransactionTotal>> getTransactionsByFundChunk(List<String> fundIds, String fiscalYearId, List<TransactionTotal.TransactionType> trTypes,
                                                                    TransactionTotalSetting setting, RequestContext requestContext) {
    var fundQuery = convertIdsToCqlQuery(fundIds, setting.getValue(), "==", " OR ");
    var trTypeValues = trTypes.stream().map(TransactionTotal.TransactionType::value).toList();
    var trTypeQuery = convertIdsToCqlQuery(trTypeValues, "transactionType", "==", " OR ");
    var query = String.format("(fiscalYearId==%s AND %s) AND %s", fiscalYearId, trTypeQuery, fundQuery);
    return getAllTransactionsByQuery(query, requestContext);
  }

  private List<TransactionTotal> filterFundIdsByAllocationDirection(List<String> fundIds, List<TransactionTotal> transactions,
                                                                    TransactionTotalSetting setting) {
    // Note that here getToFundId() is used when direction is from (a negation is used afterward)
    Function<TransactionTotal, String> getFundId = setting == TransactionTotalSetting.FROM_FUND_ID
      ? TransactionTotal::getToFundId : TransactionTotal::getFromFundId;
    return transactions.stream()
      .filter(transaction -> !fundIds.contains(getFundId.apply(transaction)))
      .toList();
  }

  private Future<List<TransactionTotal>> getAllTransactionsByQuery(String query, RequestContext requestContext) {
    return getTransactionCollectionByQuery(query, requestContext)
      .map(TransactionTotalCollection::getTransactionTotals);
  }

  private Future<TransactionTotalCollection> getTransactionCollectionByQuery(String query, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(TRANSACTION_TOTALS))
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), TransactionTotalCollection.class, requestContext);
  }
}
