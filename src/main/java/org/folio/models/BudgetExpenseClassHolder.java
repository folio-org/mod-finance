package org.folio.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.folio.rest.jaxrs.model.BudgetExpenseClass;

public class BudgetExpenseClassHolder {

  private final List<BudgetExpenseClass> createList;
  private final List<BudgetExpenseClass> updateList;
  private final List<BudgetExpenseClass> deleteList;


  public BudgetExpenseClassHolder() {
    this.createList = new ArrayList<>();
    this.updateList = new ArrayList<>();
    this.deleteList = new ArrayList<>();
  }


  public List<BudgetExpenseClass> getCreateList() {
    return createList;
  }

  public void addToCreateList(BudgetExpenseClass item) {
    this.createList.add(item);
  }

  public List<BudgetExpenseClass> getUpdateList() {
    return updateList;
  }

  public void addToUpdateList(BudgetExpenseClass item) {
    this.updateList.add(item);
  }

  public List<BudgetExpenseClass> getDeleteList() {
    return deleteList;
  }

  public void addAllToDeleteList(Collection<BudgetExpenseClass> items) {
    this.deleteList.addAll(items);
  }
}
