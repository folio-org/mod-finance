package org.folio.dao;

import static org.folio.rest.util.ResourcePathResolver.BUDGETS;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;

public class BudgetHttpDAO extends AbstractHttpDAO<Budget, BudgetsCollection> implements BudgetDAO {
  @Override
  protected String getByIdEndpoint(String id) {
    return resourceByIdPath(BUDGETS, id);
  }

  @Override
  protected String getEndpoint() {
    return resourcesPath(BUDGETS);
  }

  @Override
  protected Class<Budget> getClazz() {
    return Budget.class;
  }

  @Override
  protected Class<BudgetsCollection> getCollectionClazz() {
    return BudgetsCollection.class;
  }
}
