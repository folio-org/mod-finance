package org.folio.config;

import static org.folio.rest.util.ResourcePathResolver.BUDGETS;
import static org.folio.rest.util.ResourcePathResolver.BUDGET_EXPENSE_CLASSES;
import static org.folio.rest.util.ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.core.RestClient;
import org.folio.services.BudgetExpenseClassService;
import org.folio.services.BudgetExpenseClassTotalsService;
import org.folio.services.ExpenseClassService;
import org.folio.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"org.folio"})
public class ApplicationConfig {

  @Bean
  RestClient budgetRestClient() {
    return new RestClient(resourcesPath(BUDGETS));
  }

  @Bean
  RestClient budgetExpenseClassRestClient() {
    return new RestClient(resourcesPath(BUDGET_EXPENSE_CLASSES));
  }

  @Bean
  RestClient expenseClassRestClient() {
    return new RestClient(resourcesPath(EXPENSE_CLASSES_STORAGE_URL));
  }

  @Bean
  RestClient transactionRestClient() {
    return new RestClient(resourcesPath(TRANSACTIONS));
  }

  @Bean
  @Autowired
  public ExpenseClassService expenseClassService(RestClient expenseClassRestClient) {
    return new ExpenseClassService(expenseClassRestClient);
  }

  @Bean
  public BudgetExpenseClassService budgetExpenseClassService(RestClient budgetExpenseClassRestClient) {
    return new BudgetExpenseClassService(budgetExpenseClassRestClient);
  }

  @Bean
  public TransactionService transactionService(RestClient transactionRestClient) {
    return new TransactionService(transactionRestClient);
  }

  @Bean
  public BudgetExpenseClassTotalsService budgetExpenseClassTotalsService(RestClient budgetRestClient,
                                                                         ExpenseClassService expenseClassService,
                                                                         TransactionService transactionService,
                                                                         BudgetExpenseClassService budgetExpenseClassService)  {
    return new BudgetExpenseClassTotalsService(budgetRestClient, expenseClassService, transactionService, budgetExpenseClassService);
  }
}
