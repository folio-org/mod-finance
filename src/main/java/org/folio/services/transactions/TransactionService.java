package org.folio.services.transactions;

import static java.util.Collections.singletonList;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.BATCH_TRANSACTIONS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.TransactionType;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Future;
import one.util.streamex.StreamEx;
import org.folio.services.fiscalyear.FiscalYearService;

public class TransactionService {
  private static final Logger log = LogManager.getLogger();
  private static final int MAX_FUND_PER_QUERY = 5;

  private final RestClient restClient;
  private final FiscalYearService fiscalYearService;

  public TransactionService(RestClient restClient, FiscalYearService fiscalYearService) {
    this.restClient = restClient;
    this.fiscalYearService = fiscalYearService;
  }

  public Future<TransactionCollection> getTransactionCollectionByQuery(String query, int offset, int limit,
      RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(TRANSACTIONS))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), TransactionCollection.class, requestContext);
  }

  public Future<List<Transaction>> getAllTransactionsByQuery(String query, RequestContext requestContext) {
    return getTransactionCollectionByQuery(query, 0, Integer.MAX_VALUE, requestContext)
      .map(TransactionCollection::getTransactions);
  }

  public Future<Transaction> getTransactionById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(TRANSACTIONS, id), Transaction.class, requestContext);
  }

  public Future<Void> processBatch(Batch batch, RequestContext requestContext) {
    return restClient.postEmptyResponse(resourcesPath(BATCH_TRANSACTIONS_STORAGE), batch, requestContext)
      .onSuccess(v -> log.info("Batch transaction successful"))
      .onFailure(t -> log.error("Batch transaction failed, batch={}", JsonObject.mapFrom(batch).encodePrettily(), t));
  }

  public Future<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    // This will need to change to use patch when it is available - see MODFIN-351
    Batch batch = new Batch().withTransactionsToUpdate(singletonList(transaction));
    return processBatch(batch, requestContext);
  }

  public Future<List<Transaction>> getTransactionsFromFunds(List<String> fundIds, String fiscalYearId,
      List<TransactionType> trTypes, RequestContext requestContext) {
    return getTransactionsFromOrToFunds(fundIds, fiscalYearId, trTypes, "from", requestContext);
  }

  public Future<List<Transaction>> getTransactionsToFunds(List<String> fundIds, String fiscalYearId,
      List<TransactionType> trTypes, RequestContext requestContext) {
    return getTransactionsFromOrToFunds(fundIds, fiscalYearId, trTypes, "to", requestContext);
  }

  public Future<List<Transaction>> getBudgetTransactions(Budget budget, RequestContext requestContext) {
    String query = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s", budget.getFundId(),
      budget.getFundId(), budget.getFiscalYearId());
    return getAllTransactionsByQuery(query, requestContext);
  }

  public Future<List<Transaction>> getBudgetTransactionsWithExpenseClasses(List<BudgetExpenseClass> budgetExpenseClasses,
      SharedBudget budget, RequestContext requestContext) {
    List<String> ids = budgetExpenseClasses.stream()
      .map(BudgetExpenseClass::getExpenseClassId)
      .toList();
    String query = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s AND %s", budget.getFundId(),
      budget.getFundId(), budget.getFiscalYearId(), convertIdsToCqlQuery(ids, "expenseClassId", true));
    return getAllTransactionsByQuery(query, requestContext);
  }

  public Future<List<Transaction>> getTransactionsByFundIds(List<String> fundIds, String fiscalYearId,
      RequestContext requestContext) {
    List<Future<List<Transaction>>> futures = StreamEx
      .ofSubLists(fundIds, MAX_IDS_FOR_GET_RQ)
      .map(ids ->  getAllTransactionsByQuery(buildGetTransactionsByFundIdsQuery(fiscalYearId, ids), requestContext))
      .toList();

    return collectResultsOnSuccess(futures)
      .map(listList -> listList.stream()
        .flatMap(Collection::stream)
        .toList()
      );
  }

  public Future<Void> createTransaction(Transaction transaction, RequestContext requestContext) {
    Batch batch = new Batch().withTransactionsToCreate(singletonList(transaction));
    return processBatch(batch, requestContext);
  }

  public Future<Void> createAllocationTransaction(Budget budget, RequestContext requestContext) {
    Transaction transaction = new Transaction()
      .withTransactionType(Transaction.TransactionType.ALLOCATION)
      .withId(UUID.randomUUID().toString())
      .withAmount(budget.getAllocated())
      .withFiscalYearId(budget.getFiscalYearId())
      .withToFundId(budget.getFundId())
      .withSource(Transaction.Source.USER);
    return fiscalYearService.getFiscalYearById(budget.getFiscalYearId(), requestContext)
      .compose(fiscalYear -> createTransaction(transaction.withCurrency(fiscalYear.getCurrency()), requestContext));
  }


  private Future<List<Transaction>> getTransactionsFromOrToFunds(List<String> fundIds, String fiscalYearId,
      List<TransactionType> trTypes, String direction, RequestContext requestContext) {
    return collectResultsOnSuccess(ofSubLists(new ArrayList<>(fundIds), MAX_FUND_PER_QUERY)
        .map(ids -> getTransactionsByFundChunk(ids, fiscalYearId, trTypes, direction, requestContext)
          .map(transactions -> filterFundIdsByAllocationDirection(fundIds, transactions, direction)))
        .toList())
      .map(lists -> lists.stream().flatMap(Collection::stream).toList());
  }

  private Future<List<Transaction>> getTransactionsByFundChunk(List<String> fundIds, String fiscalYearId,
      List<TransactionType> trTypes, String direction, RequestContext requestContext) {
    String fundIdField = "from".equals(direction) ? "fromFundId" : "toFundId";
    String fundQuery = convertIdsToCqlQuery(fundIds, fundIdField, "==", " OR ");
    List<String> trTypeValues = trTypes.stream().map(TransactionType::value).toList();
    String trTypeQuery = convertIdsToCqlQuery(trTypeValues, "transactionType", "==", " OR ");
    String query = String.format("(fiscalYearId==%s AND %s) AND %s", fiscalYearId, trTypeQuery, fundQuery);
    return getAllTransactionsByQuery(query, requestContext);
  }

  private List<Transaction> filterFundIdsByAllocationDirection(List<String> fundIds, List<Transaction> transactions,
      String direction) {
    // Note that here getToFundId() is used when direction is from (a negation is used afterward)
    Function<Transaction, String> getFundId = "from".equals(direction) ? Transaction::getToFundId : Transaction::getFromFundId;
    return transactions.stream()
      .filter(transaction -> !fundIds.contains(getFundId.apply(transaction)))
      .toList();
  }

  private String buildGetTransactionsByFundIdsQuery(String fiscalYearId, List<String> fundIds) {
    return String.format("fiscalYearId==%s AND (%s OR %s)",
      fiscalYearId,
      convertIdsToCqlQuery(fundIds, "fromFundId", true),
      convertIdsToCqlQuery(fundIds, "toFundId", true));
  }

}
