package org.folio.services.transactions;

import static org.folio.rest.util.ErrorCodes.ALLOCATION_IDS_MISMATCH;
import static org.folio.rest.util.ErrorCodes.MISSING_FUND_ID;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Transaction;

import org.folio.completablefuture.FolioVertxCompletableFuture;
import org.folio.services.fund.FundService;

public class TransactionRestrictService {

  private final FundService fundService;

  public TransactionRestrictService(FundService fundService) {
    this.fundService = fundService;
  }

  public CompletableFuture<Transaction> checkAllocation(Transaction transaction, RequestContext requestContext) {
    if (Objects.isNull(transaction.getFromFundId()) ^ Objects.isNull(transaction.getToFundId())) {
      return CompletableFuture.completedFuture(transaction);
    } else {
      return checkFundsAllocatedIds(transaction, requestContext);
    }
  }

  public CompletableFuture<Transaction> checkTransfer(Transaction transaction, RequestContext requestContext) {
    return checkFundsAllocatedIds(transaction, requestContext);
  }

  private CompletableFuture<Transaction> checkFundsAllocatedIds(Transaction transaction, RequestContext requestContext) {
    CompletableFuture<Transaction> future = new FolioVertxCompletableFuture<>(requestContext.getContext());
    if (Objects.nonNull(transaction.getFromFundId()) && Objects.nonNull(transaction.getToFundId())) {

      return fundService.retrieveFundById(transaction.getFromFundId(), requestContext)
        .thenCombine(fundService.retrieveFundById(transaction.getToFundId(), requestContext), (fromFund, toFund) ->
          (fromFund.getAllocatedToIds().isEmpty() || fromFund.getAllocatedToIds().contains(transaction.getToFundId())) &&
            (toFund.getAllocatedFromIds().isEmpty() || toFund.getAllocatedFromIds().contains(transaction.getFromFundId())))
        .thenApply(isMatch -> {
          if (Boolean.TRUE.equals(isMatch)) {
            return transaction;
          } else {
            throw new HttpException(422, ALLOCATION_IDS_MISMATCH);
          }
        });
    } else {
      future.completeExceptionally(new HttpException(422, MISSING_FUND_ID));
    }
    return future;
  }
}
