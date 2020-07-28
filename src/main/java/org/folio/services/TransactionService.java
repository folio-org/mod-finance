package org.folio.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

public class TransactionService {

  private final RestClient restClient;

  public TransactionService(RestClient restClient) {
    this.restClient = restClient;
  }

  public CompletableFuture<List<Transaction>> getTransactions(Budget budget, RequestContext requestContext) {
    String query = String.format("fromFundId==%s AND fiscalYearId==%s", budget.getFundId(), budget.getFiscalYearId());
    return restClient.get(query, 0, Integer.MAX_VALUE, requestContext, TransactionCollection.class)
      .thenApply(TransactionCollection::getTransactions);
  }
}
