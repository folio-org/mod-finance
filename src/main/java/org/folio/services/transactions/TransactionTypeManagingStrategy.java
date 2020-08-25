package org.folio.services.transactions;

import org.folio.rest.jaxrs.model.Transaction;

public interface TransactionTypeManagingStrategy extends TransactionManagingService {
  Transaction.TransactionType getStrategyName();
}
