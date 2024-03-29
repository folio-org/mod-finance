package org.folio.services.transactions;

import static org.folio.rest.RestConstants.MAX_IDS_FOR_GET_RQ;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Future;
import one.util.streamex.StreamEx;

public class CommonTransactionService extends BaseTransactionService {

  public CommonTransactionService(RestClient restClient) {
    super(restClient);
  }

  public Future<List<Transaction>> retrieveTransactions(Budget budget, RequestContext requestContext) {
    String query = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s", budget.getFundId(), budget.getFundId(), budget.getFiscalYearId());
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .map(TransactionCollection::getTransactions);
  }

  public Future<List<Transaction>> retrieveTransactions(List<BudgetExpenseClass> budgetExpenseClasses, SharedBudget budget, RequestContext requestContext) {
    List<String> ids = budgetExpenseClasses.stream()
      .map(BudgetExpenseClass::getExpenseClassId)
      .collect(Collectors.toList());
    String query = String.format("(fromFundId==%s OR toFundId==%s) AND fiscalYearId==%s AND %s", budget.getFundId(), budget.getFundId(), budget.getFiscalYearId(), convertIdsToCqlQuery(ids, "expenseClassId", true));
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .map(TransactionCollection::getTransactions);
  }

  public Future<List<Transaction>> retrieveTransactionsByFundIds(List<String> fundIds, String fiscalYearId, RequestContext requestContext) {
    List<Future<List<Transaction>>> futures = StreamEx
      .ofSubLists(fundIds, MAX_IDS_FOR_GET_RQ)
      .map(ids ->  retrieveTransactionsChunk(buildGetTransactionsQuery(fiscalYearId, ids), requestContext))
      .collect(Collectors.toList());

    return collectResultsOnSuccess(futures)
      .map(listList -> listList.stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList())
      );
  }

  private Future<List<Transaction>> retrieveTransactionsChunk(String query, RequestContext requestContext) {
    return retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .map(TransactionCollection::getTransactions);
  }

  private String buildGetTransactionsQuery(String fiscalYearId, List<String> fundIds) {
    return String.format("fiscalYearId==%s AND (%s OR %s)",
      fiscalYearId,
      convertIdsToCqlQuery(fundIds, "fromFundId", true),
      convertIdsToCqlQuery(fundIds, "toFundId", true));
  }

  public Future<Transaction> createAllocationTransaction(Budget budget, RequestContext requestContext) {
    Transaction transaction = new Transaction().withAmount(budget.getAllocated())
      .withFiscalYearId(budget.getFiscalYearId()).withToFundId(budget.getFundId())
      .withTransactionType(Transaction.TransactionType.ALLOCATION).withSource(Transaction.Source.USER);
    return restClient.get(resourceByIdPath(FISCAL_YEARS_STORAGE, budget.getFiscalYearId()), FiscalYear.class, requestContext)
      .compose(fiscalYear -> createTransaction(transaction.withCurrency(fiscalYear.getCurrency()), requestContext));
  }

}
