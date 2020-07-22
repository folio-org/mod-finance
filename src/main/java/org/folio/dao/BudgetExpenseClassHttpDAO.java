package org.folio.dao;

import static org.folio.rest.util.ResourcePathResolver.BUDGET_EXPENSE_CLASSES;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.jaxrs.model.BudgetExpenseClass;
import org.folio.rest.jaxrs.model.BudgetExpenseClassCollection;

public class BudgetExpenseClassHttpDAO extends AbstractHttpDAO<BudgetExpenseClass, BudgetExpenseClassCollection> implements BudgetExpenseClassDAO {
  @Override
  protected String getByIdEndpoint(String id) {
    return resourceByIdPath(BUDGET_EXPENSE_CLASSES, id);
  }

  @Override
  protected String getEndpoint() {
    return resourcesPath(BUDGET_EXPENSE_CLASSES);
  }

  @Override
  protected Class<BudgetExpenseClass> getClazz() {
    return BudgetExpenseClass.class;
  }

  @Override
  protected Class<BudgetExpenseClassCollection> getCollectionClazz() {
    return BudgetExpenseClassCollection.class;
  }
}
