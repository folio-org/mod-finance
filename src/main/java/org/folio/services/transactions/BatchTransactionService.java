package org.folio.services.transactions;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.Encumbrance;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import java.util.List;
import java.util.Optional;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static org.folio.rest.util.ErrorCodes.DELETE_CONNECTED_TO_INVOICE;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;
import static org.folio.rest.util.ResourcePathResolver.BATCH_TRANSACTIONS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

public class BatchTransactionService {
  private static final Logger logger = LogManager.getLogger();

  private final RestClient restClient;
  private final BaseTransactionService baseTransactionService;

  public BatchTransactionService(RestClient restClient, BaseTransactionService baseTransactionService) {
    this.restClient = restClient;
    this.baseTransactionService = baseTransactionService;
  }

  public Future<Void> processBatch(Batch batch, RequestContext requestContext) {
    return checkDeletions(batch, requestContext)
      .compose(v -> restClient.postEmptyResponse(resourcesPath(BATCH_TRANSACTIONS_STORAGE), batch, requestContext))
      .onSuccess(v -> logger.info("Batch transaction successful"))
      .onFailure(t -> logger.error("Batch transaction failed, batch={}", JsonObject.mapFrom(batch).encodePrettily(), t));
  }

  public Future<Void> releaseEncumbrance(String id, RequestContext requestContext) {
    return baseTransactionService.retrieveTransactionById(id, requestContext)
      .compose(transaction -> releaseEncumbrance(transaction, requestContext))
      .onSuccess(v -> logger.info("Release encumbrance successful, id={}", id))
      .onFailure(t -> logger.error("Release encumbrance failed, id={}", id, t));
  }

  public Future<Void> unreleaseEncumbrance(String id, RequestContext requestContext) {
    return baseTransactionService.retrieveTransactionById(id, requestContext)
      .compose(transaction -> unreleaseEncumbrance(transaction, requestContext))
      .onSuccess(v -> logger.info("Unrelease encumbrance successful, id={}", id))
      .onFailure(t -> logger.error("Unrelease encumbrance failed, id={}", id, t));
  }

  private Future<Void> releaseEncumbrance(Transaction transaction, RequestContext requestContext) {
    baseTransactionService.validateTransactionType(transaction, Transaction.TransactionType.ENCUMBRANCE);
    if (transaction.getEncumbrance().getStatus() == Encumbrance.Status.RELEASED) {
      return succeededFuture(null);
    }
    transaction.getEncumbrance().setStatus(Encumbrance.Status.RELEASED);
    return updateTransaction(transaction, requestContext);
  }

  private Future<Void> unreleaseEncumbrance(Transaction transaction, RequestContext requestContext) {
    baseTransactionService.validateTransactionType(transaction, Transaction.TransactionType.ENCUMBRANCE);
    if (transaction.getEncumbrance().getStatus() != Encumbrance.Status.RELEASED) {
      return succeededFuture(null);
    }
    transaction.getEncumbrance().setStatus(Encumbrance.Status.UNRELEASED);
    return updateTransaction(transaction, requestContext);
  }

  private Future<Void> updateTransaction(Transaction transaction, RequestContext requestContext) {
    // This will need to change to use patch when it is available - see MODFIN-351
    Batch batch = new Batch()
      .withTransactionsToUpdate(singletonList(transaction));
    return processBatch(batch, requestContext);
  }

  private Future<Void> checkDeletions(Batch batch, RequestContext requestContext) {
    List<String> ids = batch.getIdsOfTransactionsToDelete();
    if (ids.isEmpty()) {
      return succeededFuture();
    }
    // Usually it is not allowed to delete an encumbrance connected to an approved invoice.
    // To avoid adding a dependency to mod-invoice, we check if there is a related awaitingPayment transaction.
    // It is OK to delete an encumbrance connected to a *cancelled* invoice *if* the batch includes a change to the
    // matching pending payment to remove the link to the encumbrance.
    String query = convertIdsToCqlQuery(ids, "awaitingPayment.encumbranceId", "==", " OR ");
    return getTransactions(query, requestContext)
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
          logger.warn("validateDeletion:: Tried to delete transactions but one is connected to an invoice, id={}", id);
          throw new HttpException(422, DELETE_CONNECTED_TO_INVOICE.toError());
        });
        return null;
      });
  }

  private Future<List<Transaction>> getTransactions(String query, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(TRANSACTIONS))
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), TransactionCollection.class, requestContext)
      .map(TransactionCollection::getTransactions);
  }
}
