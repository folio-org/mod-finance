package org.folio.services.transactions;

import static org.folio.rest.util.ResourcePathResolver.TRANSACTION_TOTALS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.List;
import java.util.function.Function;

import org.folio.models.TransactionTotalSetting;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.TransactionTotal;
import org.folio.rest.jaxrs.model.TransactionTotalBatch;
import org.folio.rest.jaxrs.model.TransactionTotalCollection;
import org.folio.rest.jaxrs.model.TransactionType;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TransactionTotalService {

  private final RestClient restClient;

  public TransactionTotalService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<List<TransactionTotal>> getTransactionsFromFunds(List<String> fundIds, String fiscalYearId,
                                                                 List<TransactionType> trTypes, RequestContext requestContext) {
    var batchRequest = createBatchRequest(fiscalYearId, trTypes).withFromFundIds(fundIds);
    return getTransactionsFromOrToFunds(fundIds, TransactionTotalSetting.FROM_FUND_ID, batchRequest, requestContext);
  }

  public Future<List<TransactionTotal>> getTransactionsToFunds(List<String> fundIds, String fiscalYearId,
                                                               List<TransactionType> trTypes, RequestContext requestContext) {
    var batchRequest = createBatchRequest(fiscalYearId, trTypes).withToFundIds(fundIds);
    return getTransactionsFromOrToFunds(fundIds, TransactionTotalSetting.TO_FUND_ID, batchRequest, requestContext);
  }

  private Future<List<TransactionTotal>> getTransactionsFromOrToFunds(List<String> fundIds, TransactionTotalSetting setting,
                                                                      TransactionTotalBatch batchRequest, RequestContext requestContext) {
    return restClient.postBatch(resourcesPath(TRANSACTION_TOTALS), batchRequest, TransactionTotalCollection.class, requestContext)
      .map(TransactionTotalCollection::getTransactionTotals)
      .map(transactions -> filterFundIdsByAllocationDirection(fundIds, transactions, setting));
  }

  private List<TransactionTotal> filterFundIdsByAllocationDirection(List<String> fundIds, List<TransactionTotal> transactions, TransactionTotalSetting setting) {
    // Note that here getToFundId() is used when direction is from (a negation is used afterward)
    Function<TransactionTotal, String> fundIdExtractor = setting == TransactionTotalSetting.FROM_FUND_ID
      ? TransactionTotal::getToFundId
      : TransactionTotal::getFromFundId;
    return transactions.stream()
      .filter(transaction -> !fundIds.contains(fundIdExtractor.apply(transaction)))
      .toList();
  }

  private static TransactionTotalBatch createBatchRequest(String fiscalYearId, List<TransactionType> trTypes) {
    return new TransactionTotalBatch()
      .withFiscalYearId(fiscalYearId)
      .withTransactionTypes(trTypes);
  }

}
