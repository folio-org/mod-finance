package org.folio.dao;

import static org.folio.rest.util.ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;

public class ExpenseClassHttpDAO extends AbstractHttpDAO<ExpenseClass, ExpenseClassCollection> implements ExpenseClassDAO{
  @Override
  protected String getByIdEndpoint(String id) {
    return resourceByIdPath(EXPENSE_CLASSES_STORAGE_URL, id);
  }

  @Override
  protected String getEndpoint() {
    return resourcesPath(EXPENSE_CLASSES_STORAGE_URL);
  }

  @Override
  protected Class<ExpenseClass> getClazz() {
    return ExpenseClass.class;
  }

  @Override
  protected Class<ExpenseClassCollection> getCollectionClazz() {
    return ExpenseClassCollection.class;
  }
}
