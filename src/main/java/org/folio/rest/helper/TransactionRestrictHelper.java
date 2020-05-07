package org.folio.rest.helper;

import static org.folio.rest.util.ErrorCodes.ALLOCATION_IDS_MISMATCH;
import static org.folio.rest.util.ErrorCodes.MISSING_FUND_ID;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Transaction;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class TransactionRestrictHelper extends AbstractHelper {

  public TransactionRestrictHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Transaction> checkRestrictions(Transaction transaction) {
    switch (transaction.getTransactionType()) {
      case ALLOCATION:
        return checkAllocation(transaction);
      case TRANSFER:
        return checkTransfer(transaction);
      default:
        return CompletableFuture.completedFuture(transaction);
    }
  }

  private CompletableFuture<Transaction> checkAllocation(Transaction transaction) {
    if (Objects.isNull(transaction.getFromFundId()) ^ Objects.isNull(transaction.getToFundId())) {
      return CompletableFuture.completedFuture(transaction);
    } else {
      return checkFundsAllocatedIds(transaction);
    }
  }

  private CompletableFuture<Transaction> checkTransfer(Transaction transaction) {
    return checkFundsAllocatedIds(transaction);
  }

  private CompletableFuture<Transaction> checkFundsAllocatedIds(Transaction transaction) {
    CompletableFuture<Transaction> future = new VertxCompletableFuture<>(ctx);
    if (Objects.nonNull(transaction.getFromFundId()) && Objects.nonNull(transaction.getToFundId())) {
      FundsHelper fundsHelper = new FundsHelper(okapiHeaders, ctx, lang);
      return fundsHelper.getFund(transaction.getFromFundId())
        .thenCombine(fundsHelper.getFund(transaction.getToFundId()), (fromFund, toFund) ->
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
