package org.folio.services.transactions;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.ALLOCATION;
import static org.folio.rest.jaxrs.model.Transaction.TransactionType.TRANSFER;
import static org.folio.rest.util.ErrorCodes.ALLOCATION_IDS_MISMATCH;
import static org.folio.rest.util.ErrorCodes.DELETE_CONNECTED_TO_INVOICE;
import static org.folio.rest.util.ErrorCodes.INVALID_TRANSACTION_TYPE;
import static org.folio.rest.util.ErrorCodes.MISSING_FUND_ID;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.TransactionType;
import org.folio.rest.jaxrs.model.TransactionCollection;

import io.vertx.core.Future;
import org.folio.services.fund.FundService;

/**
 * This class is only used by the API handlers (TransactionsApi and EncumbranceApi).
 * It does not directly use storage (using TransactionService for storage calls).
 */
public class TransactionApiService {
  private static final Logger log = LogManager.getLogger();
  private static final int MAX_TRANSACTIONS_PER_QUERY = 15;

  private final TransactionService transactionService;
  private final FundService fundService;

  public TransactionApiService(TransactionService transactionService, FundService fundService) {
    this.transactionService = transactionService;
    this.fundService = fundService;
  }

  public Future<TransactionCollection> getTransactionCollectionByQuery(String query, int offset, int limit,
      RequestContext requestContext) {
    return transactionService.getTransactionCollectionByQuery(query, offset, limit, requestContext);
  }

  public Future<Transaction> getTransactionById(String id, RequestContext requestContext) {
    return transactionService.getTransactionById(id, requestContext);
  }

  public Future<Void> processBatch(Batch batch, RequestContext requestContext) {
    return checkDeletions(batch, requestContext)
      .compose(v -> checkFundAllocations(batch, requestContext))
      .compose(v -> transactionService.processBatch(batch, requestContext));
  }

  public Future<Transaction> createAllocation(Transaction allocation, RequestContext requestContext) {
    try {
      validateTransactionType(allocation, ALLOCATION);
    } catch (Exception ex) {
      return failedFuture(ex);
    }
    if (allocation.getId() == null) {
      allocation.setId(UUID.randomUUID().toString());
    }
    Batch batch = new Batch().withTransactionsToCreate(singletonList(allocation));
    return processBatch(batch, requestContext)
      .map(v -> allocation)
      .onSuccess(v -> log.info("Success creating allocation, id={}", allocation.getId()))
      .onFailure(t -> log.error("Error creating allocation, id={}", allocation.getId(), t));
  }

  public Future<Transaction> createTransfer(Transaction transfer, RequestContext requestContext) {
    try {
      validateTransactionType(transfer, TRANSFER);
    } catch (Exception ex) {
      return failedFuture(ex);
    }
    if (transfer.getId() == null) {
      transfer.setId(UUID.randomUUID().toString());
    }
    Batch batch = new Batch().withTransactionsToCreate(singletonList(transfer));
    return processBatch(batch, requestContext)
      .map(v -> transfer)
      .onSuccess(v -> log.info("Success creating transfer, id={}", transfer.getId()))
      .onFailure(t -> log.error("Error creating transfer, id={}", transfer.getId(), t));
  }

  public Future<Void> releaseEncumbrance(String id, RequestContext requestContext) {
    return getTransactionById(id, requestContext)
      .compose(transaction -> releaseEncumbrance(transaction, requestContext))
      .onSuccess(v -> log.info("Success releasing encumbrance, id={}", id))
      .onFailure(t -> log.error("Error releasing encumbrance, id={}", id, t));
  }

  public Future<Void> unreleaseEncumbrance(String id, RequestContext requestContext) {
    return getTransactionById(id, requestContext)
      .compose(transaction -> unreleaseEncumbrance(transaction, requestContext))
      .onSuccess(v -> log.info("Success unreleasing encumbrance, id={}", id))
      .onFailure(t -> log.error("Error unreleasing encumbrance, id={}", id, t));
  }


  private void validateTransactionType(Transaction transaction, TransactionType transactionType) {
    if (transaction.getTransactionType() != transactionType) {
      log.warn("validateTransactionType:: Transaction '{}' type mismatch. '{}' expected", transaction.getId(), transactionType);
      Parameter parameter = new Parameter().withKey("expected").withValue(transactionType.name());
      throw new HttpException(422, INVALID_TRANSACTION_TYPE.toError().withParameters(Collections.singletonList(parameter)));
    }
  }

  /**
   * Check transaction deletions are allowed.
   * Usually it is not allowed to delete an encumbrance connected to an approved invoice.
   * To avoid adding a dependency to mod-invoice, we check if there is a related awaitingPayment transaction.
   * It is OK to delete an encumbrance connected to a *cancelled* invoice *if* the batch includes a change to the
   * matching pending payment to remove the link to the encumbrance.
   */
  private Future<Void> checkDeletions(Batch batch, RequestContext requestContext) {
    List<String> ids = batch.getIdsOfTransactionsToDelete();
    if (ids.isEmpty()) {
      return succeededFuture();
    }
    return getPendingPaymentsByEncumbranceIds(ids, requestContext)
      .map(pendingPayments -> {
        if (pendingPayments.isEmpty()) {
          return null;
        }
        ids.forEach(id -> {
          Optional<Transaction> existingPP = pendingPayments.stream()
            .filter(pp -> id.equals(pp.getAwaitingPayment().getEncumbranceId())).findFirst();
          if (existingPP.isEmpty()) {
            return;
          }
          if (TRUE.equals(existingPP.get().getInvoiceCancelled())) {
            Optional<Transaction> matchingPPInBatch = batch.getTransactionsToUpdate().stream()
              .filter(pp -> existingPP.get().getId().equals(pp.getId())).findFirst();
            if (matchingPPInBatch.isPresent() && matchingPPInBatch.get().getAwaitingPayment().getEncumbranceId() == null) {
              return;
            }
          }
          log.warn("validateDeletion:: Tried to delete transactions but one is connected to an invoice, id={}", id);
          throw new HttpException(422, DELETE_CONNECTED_TO_INVOICE.toError());
        });
        return null;
      });
  }

  private Future<List<Transaction>> getPendingPaymentsByEncumbranceIds(List<String> encumbranceIds,
    RequestContext requestContext) {
    return collectResultsOnSuccess(ofSubLists(new ArrayList<>(encumbranceIds), MAX_TRANSACTIONS_PER_QUERY)
      .map(ids -> getPendingPaymentsByEncumbranceIdsChunk(ids, requestContext))
      .toList())
      .map(lists -> lists.stream().flatMap(Collection::stream).toList());
  }

  private Future<List<Transaction>> getPendingPaymentsByEncumbranceIdsChunk(List<String> encumbranceIds,
    RequestContext requestContext) {
    String query = convertIdsToCqlQuery(encumbranceIds, "awaitingPayment.encumbranceId", "==", " OR ");
    return transactionService.getAllTransactionsByQuery(query, requestContext);
  }

  private Future<Void> checkFundAllocations(Batch batch, RequestContext requestContext) {
    List<Transaction> transactionsToCheck = batch.getTransactionsToCreate().stream()
      .filter(t -> (t.getTransactionType() == ALLOCATION && Objects.isNull(t.getFromFundId()) == Objects.isNull(t.getToFundId())) ||
        t.getTransactionType() == TRANSFER)
      .toList();
    if (transactionsToCheck.isEmpty()) {
      return succeededFuture();
    }
    for (Transaction t : transactionsToCheck) {
      if (!(Objects.nonNull(t.getFromFundId()) && Objects.nonNull(t.getToFundId()))) {
        log.warn("checkFundsAllocatedIds:: fundId is missing for transaction: {}", t.getId());
        return failedFuture(new HttpException(422, MISSING_FUND_ID));
      }
    }
    List<String> fundIds = transactionsToCheck.stream()
      .map(t -> List.of(t.getFromFundId(), t.getToFundId()))
      .flatMap(Collection::stream)
      .filter(Objects::nonNull)
      .toList();
    return fundService.getFundsByIds(fundIds, requestContext)
      .map(funds -> {
        Map<String, Fund> fundMap = funds.stream().collect(toMap(Fund::getId, Function.identity()));
        for (Transaction t : transactionsToCheck) {
          if (!isAllocationAllowed(fundMap.get(t.getFromFundId()), fundMap.get(t.getToFundId()), t)) {
            log.warn("checkFundsAllocatedIds:: Allocation ids mismatch for transaction: {}", t.getId());
            throw new HttpException(422, ALLOCATION_IDS_MISMATCH);
          }
        }
        return null;
      })
      .onSuccess(v -> log.debug("checkFundAllocations:: Funds checked successfully for transactions"))
      .mapEmpty();
  }

  private boolean isAllocationAllowed(Fund fromFund, Fund toFund, Transaction transaction) {
    return (fromFund.getAllocatedToIds().isEmpty() || fromFund.getAllocatedToIds().contains(transaction.getToFundId())) &&
      (toFund.getAllocatedFromIds().isEmpty() || toFund.getAllocatedFromIds().contains(transaction.getFromFundId()));
  }

  private Future<Void> releaseEncumbrance(Transaction transaction, RequestContext requestContext) {
    try {
      validateTransactionType(transaction, TransactionType.ENCUMBRANCE);
    } catch (Exception ex) {
      return failedFuture(ex);
    }
    if (transaction.getEncumbrance().getStatus() == Encumbrance.Status.RELEASED) {
      return succeededFuture(null);
    }
    transaction.getEncumbrance().setStatus(Encumbrance.Status.RELEASED);
    return transactionService.updateTransaction(transaction, requestContext);
  }

  private Future<Void> unreleaseEncumbrance(Transaction transaction, RequestContext requestContext) {
    try {
      validateTransactionType(transaction, TransactionType.ENCUMBRANCE);
    } catch (Exception ex) {
      return failedFuture(ex);
    }
    if (transaction.getEncumbrance().getStatus() != Encumbrance.Status.RELEASED) {
      return succeededFuture(null);
    }
    transaction.getEncumbrance().setStatus(Encumbrance.Status.UNRELEASED);
    return transactionService.updateTransaction(transaction, requestContext);
  }

}
