package org.folio.services.budget;

import static org.folio.rest.RestConstants.BAD_REQUEST;
import static org.folio.rest.RestConstants.NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.FUND_NOT_FOUND_ERROR;

import java.util.Optional;
import io.vertx.core.Future;
import java.util.concurrent.CompletionException;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.SharedBudget;
import org.folio.rest.util.BudgetUtils;
import org.folio.rest.util.ErrorCodes;
import org.folio.rest.util.ExpenseClassConverterUtils;
import org.folio.services.group.GroupFundFiscalYearService;
import org.folio.services.fund.FundDetailsService;
import org.folio.services.fund.FundFiscalYearService;
import org.folio.services.transactions.CommonTransactionService;

public class CreateBudgetService {
  private final RestClient budgetRestClient;
  private final GroupFundFiscalYearService groupFundFiscalYearService;
  private final FundFiscalYearService fundFiscalYearService;
  private final BudgetExpenseClassService budgetExpenseClassService;
  private final CommonTransactionService transactionService;
  private final FundDetailsService fundDetailsService;

  public CreateBudgetService(RestClient budgetRestClient,
                             GroupFundFiscalYearService groupFundFiscalYearService,
                             FundFiscalYearService fundFiscalYearService,
                             BudgetExpenseClassService budgetExpenseClassService,
                             CommonTransactionService transactionService,
                             FundDetailsService fundDetailsService) {
    this.budgetRestClient = budgetRestClient;
    this.groupFundFiscalYearService = groupFundFiscalYearService;
    this.fundFiscalYearService = fundFiscalYearService;
    this.budgetExpenseClassService = budgetExpenseClassService;
    this.transactionService = transactionService;
    this.fundDetailsService = fundDetailsService;
  }

  public Future<SharedBudget> createBudget(SharedBudget sharedBudget, RequestContext requestContext) {
    return fundFiscalYearService.retrievePlannedFiscalYear(BudgetUtils.convertToBudget(sharedBudget).getFundId(), requestContext)
                             .thenCompose(plannedFiscalYear -> {
                               Budget budget = BudgetUtils.convertToBudget(sharedBudget);
                               if (plannedFiscalYear !=null && budget.getFiscalYearId().equals(plannedFiscalYear.getId())) {
                                 return createPlannedBudget(sharedBudget, requestContext);
                               } else {
                                 return createNewBudget(sharedBudget, requestContext);
                               }
                             })
                             .exceptionally(t -> {
                               if (t.getCause() instanceof HttpException ) {
                                 processHttpException((HttpException) t.getCause());
                               }
                               throw new CompletionException(t.getCause());
                             });
  }

  private void processHttpException(HttpException httpException) {
   Error error = Optional.ofNullable(httpException.getErrors())
                        .map(Errors::getErrors)
                        .filter(errors -> !CollectionUtils.isEmpty(errors))
                        .map(errors -> errors.get(0))
                        .orElseThrow(() -> new CompletionException(httpException));
    if (NOT_FOUND == httpException.getCode() && FUND_NOT_FOUND_ERROR.getCode().equals(error.getCode())) {
      throw new CompletionException(new HttpException(BAD_REQUEST, httpException.getErrors()));
    }
  }

  private Future<Budget> allocateToBudget(Budget createdBudget, RequestContext requestContext) {
    if (createdBudget.getAllocated() > 0d) {
      return transactionService.createAllocationTransaction(createdBudget, requestContext)
        .map(transaction -> createdBudget)
        .exceptionally(e -> {
          throw new HttpException(500, ErrorCodes.ALLOCATION_TRANSFER_FAILED);
        });
    }
    return succeededFuture(createdBudget);
  }

  private Future<SharedBudget> createPlannedBudget(SharedBudget sharedBudget, RequestContext requestContext) {
    if (CollectionUtils.isEmpty(sharedBudget.getStatusExpenseClasses())) {
      return fundDetailsService.retrieveCurrentBudget(sharedBudget.getFundId(), null, true, requestContext)
                               .thenCompose(currBudget -> {
                                 if (currBudget != null) {
                                 return  budgetExpenseClassService.getBudgetExpenseClasses(currBudget.getId(),  requestContext)
                                                     .map(ExpenseClassConverterUtils::buildStatusExpenseClassList)
                                                     .map(sharedBudget::withStatusExpenseClasses)
                                                     .thenCompose(updatedSharedBudget -> createNewBudget(updatedSharedBudget, requestContext));
                                 }
                                 return createNewBudget(sharedBudget, requestContext);
                               });
    }
    return createNewBudget(sharedBudget, requestContext);
  }

  public Future<SharedBudget> createNewBudget(SharedBudget sharedBudget, RequestContext requestContext) {
    double allocatedValue = sharedBudget.getAllocated();
    sharedBudget.setAllocated(0d);
    return budgetRestClient.post(BudgetUtils.convertToBudget(sharedBudget), requestContext, Budget.class)
                           .thenCompose(createdBudget -> allocateToBudget(createdBudget.withAllocated(allocatedValue), requestContext))
                           .thenCompose(createdBudget -> groupFundFiscalYearService.updateBudgetIdForGroupFundFiscalYears(createdBudget, requestContext)
                                          .thenCompose(aVoid -> budgetExpenseClassService.createBudgetExpenseClasses(sharedBudget, requestContext))
                                          .map(aVoid -> BudgetUtils.convertToSharedBudget(createdBudget).withStatusExpenseClasses(sharedBudget.getStatusExpenseClasses()))
    );
  }
}
