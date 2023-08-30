package org.folio.services.transactions;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.ErrorCodes.ALLOCATION_IDS_MISMATCH;
import static org.folio.rest.util.ErrorCodes.MISSING_FUND_ID;

import java.util.Objects;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.Transaction;

import org.folio.services.fund.FundService;

public class TransactionRestrictService {

  private final FundService fundService;

  public TransactionRestrictService(FundService fundService) {
    this.fundService = fundService;
  }

  public Future<Void> checkAllocation(Transaction transaction, RequestContext requestContext) {
    if (Objects.isNull(transaction.getFromFundId()) ^ Objects.isNull(transaction.getToFundId())) {
      return succeededFuture();
    } else {
      return checkFundsAllocatedIds(transaction, requestContext);
    }
  }

  public Future<Void> checkTransfer(Transaction transaction, RequestContext requestContext) {
    return checkFundsAllocatedIds(transaction, requestContext);
  }

  private Future<Void> checkFundsAllocatedIds(Transaction transaction, RequestContext requestContext) {
    if (Objects.nonNull(transaction.getFromFundId()) && Objects.nonNull(transaction.getToFundId())) {
      var fromFund = fundService.retrieveFundById(transaction.getFromFundId(), requestContext);
      var toFund = fundService.retrieveFundById(transaction.getToFundId(), requestContext);
      return CompositeFuture.join(toFund, fromFund)
        .map(cf -> isAllocationAllowed(fromFund.result(), toFund.result(), transaction))
        .compose(isAllocationAllowed -> {
        if (Boolean.TRUE.equals(isAllocationAllowed)) {
          return succeededFuture();
        } else {
          return failedFuture(new HttpException(422, ALLOCATION_IDS_MISMATCH));
        }
      });

  } else {
      return succeededFuture();
    }
}

  private boolean isAllocationAllowed(Fund fromFund, Fund toFund, Transaction transaction) {
    return (fromFund.getAllocatedToIds().isEmpty() || fromFund.getAllocatedToIds().contains(transaction.getToFundId())) &&
      (toFund.getAllocatedFromIds().isEmpty() || toFund.getAllocatedFromIds().contains(transaction.getFromFundId()));
  }

}
