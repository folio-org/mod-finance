package org.folio.rest.helper;

import static org.folio.rest.util.ErrorCodes.ALLOCATION_IDS_MISMATCH;
import static org.folio.rest.util.ErrorCodes.ALLOCATION_TRANSFER_FAILED;
import static org.folio.rest.util.ErrorCodes.MISSING_FUND_ID;
import static org.folio.rest.util.HelperUtils.buildQueryParam;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.rest.util.MoneyUtils;

public class TransactionsHelper extends AbstractHelper {

  private static final String GET_TRANSACTIONS_BY_QUERY = resourcesPath(TRANSACTIONS) + SEARCH_PARAMS;

  public TransactionsHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
  }

  public CompletableFuture<Transaction> createTransaction(Transaction transaction) {
    return checkRestrictions(transaction)
      .thenCompose(res -> handleCreateRequest(resourcesPath(TRANSACTIONS), res)
      .thenApply(res::withId));
  }

  public CompletableFuture<TransactionCollection> getTransactions(int limit, int offset, String query) {
    String endpoint = String.format(GET_TRANSACTIONS_BY_QUERY, limit, offset, buildQueryParam(query, logger), lang);
    return handleGetRequest(endpoint)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(ctx, () -> json.mapTo(TransactionCollection.class)));
  }

  public CompletableFuture<Transaction> getTransaction(String id) {
    return handleGetRequest(resourceByIdPath(TRANSACTIONS, id, lang))
      .thenApply(json -> json.mapTo(Transaction.class));
  }

  public CompletableFuture<Void> updateTransaction(Transaction transaction) {
    return handleUpdateRequest(resourceByIdPath(TRANSACTIONS, transaction.getId(), lang), transaction);
  }

  /**
   * Get the {@link Transaction} (encumbrance) from storage and update the encumbered / awaiting payment amounts
   *
   * @param awaitingPayment {@link AwaitingPayment} object
   * @return {@link CompletableFuture<Void>} returns empty result
   */
  public CompletableFuture<Void> moveToAwaitingPayment(AwaitingPayment awaitingPayment) {
    return getTransaction(awaitingPayment.getEncumbranceId())
      .thenApply(tr -> modifyTransaction(tr, awaitingPayment))
      .thenCompose(this::updateTransaction);
  }

  private Transaction modifyTransaction(Transaction transaction, AwaitingPayment awaitingPayment) {
    Double currentAwaitingPaymentAmount = transaction.getEncumbrance().getAmountAwaitingPayment();
    String currency = transaction.getCurrency();

    transaction.getEncumbrance()
      .setAmountAwaitingPayment(MoneyUtils.sumDoubleValues(currentAwaitingPaymentAmount, awaitingPayment.getAmountAwaitingPayment(), currency));

    transaction.getEncumbrance().setStatus(awaitingPayment.getReleaseEncumbrance() ? Encumbrance.Status.RELEASED : Encumbrance.Status.UNRELEASED);
    transaction.setSourceInvoiceId(awaitingPayment.getInvoiceId());
    transaction.setSourceInvoiceLineId(awaitingPayment.getInvoiceLineId());
    return transaction;
  }

  private void validateTransactionType(Transaction transaction, Transaction.TransactionType type) {
    if (transaction.getTransactionType() != type) {
      logger.info("Transaction {} type mismatch. {} expected", transaction.getId(), type) ;
      throw new HttpException(400, String.format("Transaction type mismatch. %s expected", type));
    }
  }

  public CompletableFuture<Void> releaseTransaction(Transaction transaction) {
    logger.info("Start releasing transaction {}", transaction.getId()) ;

    validateTransactionType(transaction, Transaction.TransactionType.ENCUMBRANCE);

    if (transaction.getEncumbrance().getStatus() == Encumbrance.Status.RELEASED) {
      return CompletableFuture.completedFuture(null);
    }

    transaction.getEncumbrance().setStatus(Encumbrance.Status.RELEASED);
    return updateTransaction(transaction);
  }

  private CompletableFuture<Transaction> checkRestrictions(Transaction transaction) {
    CompletableFuture<Transaction> future = new VertxCompletableFuture<>(ctx);
    switch(transaction.getTransactionType()) {
    case ALLOCATION:
      if (Objects.isNull(transaction.getFromFundId()) ^ Objects.isNull(transaction.getToFundId())) {
        future.complete(transaction);
      }
    case TRANSFER:
      if (Objects.nonNull(transaction.getFromFundId()) && Objects.nonNull(transaction.getToFundId())) {
        checkAllocatedIds(transaction)
          .thenApply(isMatch ->
            isMatch ? future.complete(transaction) : future.completeExceptionally(new HttpException(422, ALLOCATION_IDS_MISMATCH)))
          .exceptionally(throwable -> future.completeExceptionally(new HttpException(500, ALLOCATION_TRANSFER_FAILED)));
      } else {
        future.completeExceptionally(new HttpException(422, MISSING_FUND_ID));
      }
      break;
    default:
      future.complete(transaction);
    }
    return future;
  }

  private CompletableFuture<Boolean> checkAllocatedIds(Transaction transaction) {
    FundsHelper fundsHelper = new FundsHelper(okapiHeaders, ctx, lang);
    return fundsHelper.getFund(transaction.getFromFundId())
      .thenCombine(fundsHelper.getFund(transaction.getToFundId()), (from, to) ->
        (from.getFund().getAllocatedToIds().isEmpty() || from.getFund().getAllocatedToIds().contains(transaction.getToFundId())) &&
          (to.getFund().getAllocatedFromIds().isEmpty() || to.getFund().getAllocatedFromIds().contains(transaction.getFromFundId())));
  }
}
