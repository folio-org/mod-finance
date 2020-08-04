package org.folio.config;

import static org.folio.rest.util.ResourcePathResolver.BUDGETS;
import static org.folio.rest.util.ResourcePathResolver.BUDGET_EXPENSE_CLASSES;
import static org.folio.rest.util.ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.core.RestClient;
import org.folio.services.FundDetailsService;
import org.folio.services.GroupFundFiscalYearService;
import org.folio.services.BudgetExpenseClassService;
import org.folio.services.BudgetExpenseClassTotalsService;
import org.folio.services.BudgetService;
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
  RestClient fiscalYearRestClient() {
    return new RestClient(resourcesPath(FISCAL_YEARS));
  }

  @Bean
  RestClient groupFundFiscalYearRestClient() {
    return new RestClient(resourcesPath(GROUP_FUND_FISCAL_YEARS));
  }

  @Bean
  @Autowired
  public ExpenseClassService expenseClassService(RestClient expenseClassRestClient) {
    return new ExpenseClassService(expenseClassRestClient);
  }

  @Bean
  public BudgetExpenseClassService budgetExpenseClassService(RestClient budgetExpenseClassRestClient, TransactionService transactionService) {
    return new BudgetExpenseClassService(budgetExpenseClassRestClient, transactionService);
  }

  @Bean
  public TransactionService transactionService(RestClient transactionRestClient, RestClient fiscalYearRestClient) {
    return new TransactionService(transactionRestClient, fiscalYearRestClient);
  }

  @Bean
  public BudgetExpenseClassTotalsService budgetExpenseClassTotalsService(RestClient budgetRestClient,
                                                                         ExpenseClassService expenseClassService,
                                                                         TransactionService transactionService,
                                                                         BudgetExpenseClassService budgetExpenseClassService)  {
    return new BudgetExpenseClassTotalsService(budgetRestClient, expenseClassService, transactionService, budgetExpenseClassService);
  }

  @Bean
  public GroupFundFiscalYearService groupFundFiscalYearService(RestClient groupFundFiscalYearRestClient) {
    return new GroupFundFiscalYearService(groupFundFiscalYearRestClient);
  }

  @Bean
  public BudgetService budgetService(RestClient budgetRestClient,
                                     TransactionService transactionService,
                                     BudgetExpenseClassService budgetExpenseClassService,
                                     GroupFundFiscalYearService groupFundFiscalYearService) {
    return new BudgetService(budgetRestClient, transactionService, budgetExpenseClassService, groupFundFiscalYearService);
  }

  @Bean
  public FundDetailsService fundService(BudgetService budgetService, BudgetExpenseClassService budgetExpenseClassService){
    return new FundDetailsService(budgetService, budgetExpenseClassService);
  }
}
