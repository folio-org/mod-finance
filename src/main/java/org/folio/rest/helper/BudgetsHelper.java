package org.folio.rest.helper;

import static org.folio.rest.util.ErrorCodes.ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ErrorCodes.ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.Source;
import org.folio.rest.util.ErrorCodes;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class BudgetsHelper extends AbstractHelper {

  @Autowired
  private RestClient budgetRestClient;
  @Autowired
  private RestClient transactionRestClient;

  public BudgetsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  public CompletableFuture<Budget> createBudget(Budget budget) {
    double allocatedValue = budget.getAllocated();
    budget.setAllocated(0d);
    return budgetRestClient.post(budget, new RequestContext(ctx, okapiHeaders), Budget.class).thenCompose(createdBudget -> {
      if (allocatedValue > 0d) {
        return createAllocationTransaction(createdBudget.withAllocated(allocatedValue))
          .exceptionally(e -> {
            throw new HttpException(500, ErrorCodes.ALLOCATION_TRANSFER_FAILED);
          });
      }
      return CompletableFuture.completedFuture(createdBudget);
    });
  }

  private CompletableFuture<Budget> createAllocationTransaction(Budget budget) {
    Transaction transaction = new Transaction().withAmount(budget.getAllocated())
      .withFiscalYearId(budget.getFiscalYearId()).withToFundId(budget.getFundId())
      .withTransactionType(Transaction.TransactionType.ALLOCATION).withSource(Source.USER);

    return handleGetRequest(resourceByIdPath(FISCAL_YEARS, budget.getFiscalYearId(), lang)).
      thenApply(json -> json.mapTo(FiscalYear.class)).thenAccept(fy ->
      transactionRestClient.post(transaction.withCurrency(fy.getCurrency()), new RequestContext(ctx, okapiHeaders), Transaction.class))
      .thenApply(aVoid -> budget);

  }

  public CompletableFuture<BudgetsCollection> getBudgets(String query, int offset, int limit) {
    return budgetRestClient.get(query, offset, limit, new RequestContext(ctx, okapiHeaders), BudgetsCollection.class);
  }

  public CompletableFuture<Budget> getBudget(String id) {
    return budgetRestClient.getById(id, new RequestContext(ctx, okapiHeaders), Budget.class);
  }

  public CompletableFuture<Void> updateBudget(Budget budget) {
    return budgetRestClient.put(budget.getId(), budget, new RequestContext(ctx, okapiHeaders));
  }

  public CompletableFuture<Void> deleteBudget(String id) {
    return budgetRestClient.delete(id, new RequestContext(ctx, okapiHeaders));
  }

  public boolean newAllowableAmountsExceeded(Budget budget) {
    BigDecimal allocated = BigDecimal.valueOf(budget.getAllocated());
    BigDecimal encumbered = BigDecimal.valueOf(budget.getEncumbered());
    BigDecimal expenditures = BigDecimal.valueOf(budget.getExpenditures());
    BigDecimal awaitingPayment = BigDecimal.valueOf(budget.getAwaitingPayment());
    BigDecimal available = BigDecimal.valueOf(budget.getAvailable());
    BigDecimal unavailable = BigDecimal.valueOf(budget.getUnavailable());

    //[remaining amount we can encumber] = (allocated * allowableEncumbered) - (encumbered + awaitingPayment + expended)
    if (budget.getAllowableEncumbrance() != null) {
      BigDecimal newAllowableEncumbrance = BigDecimal.valueOf(budget.getAllowableEncumbrance()).movePointLeft(2);
      if (allocated.multiply(newAllowableEncumbrance).compareTo(encumbered.add(awaitingPayment).add(expenditures)) < 0) {
        this.addProcessingError(ALLOWABLE_ENCUMBRANCE_LIMIT_EXCEEDED.toError());
      }
    }
    //[amount we can expend] = (allocated * allowableExpenditure) - (allocated - (unavailable + available)) - (awaitingPayment + expended)
    if (budget.getAllowableExpenditure() != null) {
      BigDecimal newAllowableExpenditure = BigDecimal.valueOf(budget.getAllowableExpenditure())
        .movePointLeft(2);
      if (allocated.multiply(newAllowableExpenditure)
        .subtract(allocated.subtract(available.add(unavailable)))
        .subtract(expenditures.add(awaitingPayment))
        .compareTo(BigDecimal.ZERO) < 0) {
        this.addProcessingError(ALLOWABLE_EXPENDITURE_LIMIT_EXCEEDED.toError());
      }
    }
    return !getProcessingErrors().getErrors().isEmpty();
  }
}
