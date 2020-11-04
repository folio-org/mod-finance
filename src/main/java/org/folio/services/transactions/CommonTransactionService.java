package org.folio.services.transactions;

import static java.util.stream.Collectors.toList;
import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import one.util.streamex.StreamEx;

public class CommonTransactionService extends BaseTransactionService {

  private static final Logger logger = LoggerFactory.getLogger(CommonTransactionService.class);

  private final RestClient fiscalYearRestClient;

  public CommonTransactionService(RestClient transactionRestClient, RestClient fiscalYearRestClient) {
    super(transactionRestClient);
    this.fiscalYearRestClient = fiscalYearRestClient;
  }

  public CompletableFuture<List<Transaction>> retrieveTransactions(Budget budget, RequestContext requestContext) {
    String query = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s", budget.getFundId(), budget.getFundId(), budget.getFiscalYearId());
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(TransactionCollection::getTransactions);
  }

  public CompletableFuture<List<Transaction>> retrieveTransactions(List<BudgetExpenseClass> budgetExpenseClasses, SharedBudget budget, RequestContext requestContext) {
    List<String> ids = budgetExpenseClasses.stream().map(BudgetExpenseClass::getExpenseClassId).collect(Collectors.toList());
    String query = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s AND %s", budget.getFundId(), budget.getFundId(), budget.getFiscalYearId(), convertIdsToCqlQuery(ids, "expenseClassId", true));
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(TransactionCollection::getTransactions);
  }

  public CompletableFuture<List<Transaction>> retrieveTransactionsByFundIds(List<String> fundIds, String fiscalYearId, RequestContext requestContext) {
    List<CompletableFuture<List<Transaction>>> futures = StreamEx
      .ofSubLists(fundIds, MAX_IDS_FOR_GET_RQ)
      .map(ids ->  retrieveTransactionsChunk(buildGetTransactionsQuery(fiscalYearId, ids), requestContext))
      .collect(toList());

    return collectResultsOnSuccess(futures)
      .thenApply(listList -> listList.stream().flatMap(Collection::stream).collect(toList()));
  }

  private CompletableFuture<List<Transaction>> retrieveTransactionsChunk(String query, RequestContext requestContext) {
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(TransactionCollection::getTransactions);
  }

  private String buildGetTransactionsQuery(String fiscalYearId, List<String> fundIds) {
    return String.format("fiscalYearId==%s AND (%s OR %s)",
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

  public CompletableFuture<Void> releaseTransaction(String id, RequestContext requestContext) {
    return retrieveTransactionById(id, requestContext)
      .thenCompose(transaction -> releaseTransaction(transaction, requestContext));
  }

  private CompletableFuture<Void> releaseTransaction(Transaction transaction, RequestContext requestContext) {
    logger.info("Start releasing transaction {}", transaction.getId()) ;

    validateTransactionType(transaction, Transaction.TransactionType.ENCUMBRANCE);

    if (transaction.getEncumbrance().getStatus() == Encumbrance.Status.RELEASED) {
      return CompletableFuture.completedFuture(null);
    }

    transaction.getEncumbrance().setStatus(Encumbrance.Status.RELEASED);
    return updateTransaction(transaction, requestContext);
  }

}
