package org.folio.dao;

import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

public class TransactionHttpDAO extends AbstractHttpDAO<Transaction, TransactionCollection> implements TransactionDAO {
  @Override
  protected String getByIdEndpoint(String id) {
    return resourceByIdPath(TRANSACTIONS, id);
  }

  @Override
  protected String getEndpoint() {
    return resourcesPath(TRANSACTIONS);
  }

  @Override
  protected Class<Transaction> getClazz() {
    return Transaction.class;
  }

  @Override
  protected Class<TransactionCollection> getCollectionClazz() {
    return TransactionCollection.class;
  }
}
