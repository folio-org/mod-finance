package org.folio.config;

import org.folio.rest.core.RestClient;
import org.folio.services.FiscalYearService;
import org.folio.services.FundDetailsService;
import org.folio.services.GroupFundFiscalYearService;
import org.folio.services.BudgetExpenseClassService;
import org.folio.services.BudgetExpenseClassTotalsService;
import org.folio.services.BudgetService;
import org.folio.services.ExpenseClassService;
import org.folio.services.LedgerService;
import org.folio.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan({"org.folio"})
@Import(RestClientConfiguration.class)
public class ApplicationConfig {

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
  public TransactionService transactionService(RestClient transactionRestClient, RestClient fiscalYearStorageRestClient) {
    return new TransactionService(transactionRestClient, fiscalYearStorageRestClient);
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

  @Bean
  public LedgerService ledgerService(RestClient ledgerStorageRestClient){
    return new LedgerService(ledgerStorageRestClient);
  }

  @Bean
  public FiscalYearService fiscalYearService(RestClient fiscalYearStorageRestClient, LedgerService ledgerService){
    return new FiscalYearService(fiscalYearStorageRestClient, ledgerService);
  }
}
