package org.folio.config;

import org.folio.rest.core.RestClient;
import org.folio.services.BudgetExpenseClassService;
import org.folio.services.BudgetExpenseClassTotalsService;
import org.folio.services.BudgetService;
import org.folio.services.ExpenseClassService;
import org.folio.services.FiscalYearService;
import org.folio.services.FundDetailsService;
import org.folio.services.FundService;
import org.folio.services.GroupFundFiscalYearService;
import org.folio.services.LedgerService;
import org.folio.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class ServicesConfiguration {
  @Bean
  @Autowired
  ExpenseClassService expenseClassService(RestClient expenseClassRestClient) {
    return new ExpenseClassService(expenseClassRestClient);
  }

  @Bean
  BudgetExpenseClassService budgetExpenseClassService(RestClient budgetExpenseClassRestClient, TransactionService transactionService) {
    return new BudgetExpenseClassService(budgetExpenseClassRestClient, transactionService);
  }

  @Bean
  TransactionService transactionService(RestClient transactionRestClient, RestClient fiscalYearRestClient) {
    return new TransactionService(transactionRestClient, fiscalYearRestClient);
  }

  @Bean
  BudgetExpenseClassTotalsService budgetExpenseClassTotalsService(RestClient budgetRestClient,
                                                                  ExpenseClassService expenseClassService,
                                                                  TransactionService transactionService,
                                                                  BudgetExpenseClassService budgetExpenseClassService)  {
    return new BudgetExpenseClassTotalsService(budgetRestClient, expenseClassService, transactionService, budgetExpenseClassService);
  }

  @Bean
  GroupFundFiscalYearService groupFundFiscalYearService(RestClient groupFundFiscalYearRestClient) {
    return new GroupFundFiscalYearService(groupFundFiscalYearRestClient);
  }

  @Bean
  BudgetService budgetService(RestClient budgetRestClient,
                              TransactionService transactionService,
                              BudgetExpenseClassService budgetExpenseClassService,
                              GroupFundFiscalYearService groupFundFiscalYearService) {
    return new BudgetService(budgetRestClient, transactionService, budgetExpenseClassService, groupFundFiscalYearService);
  }

  @Bean
  LedgerService ledgerService(RestClient ledgerStorageRestClient, RestClient ledgerFYStorageRestClient) {
    return new LedgerService(ledgerStorageRestClient, ledgerFYStorageRestClient);
  }

  @Bean
  FiscalYearService fiscalYearService(LedgerService ledgerService, RestClient fiscalYearRestClient){
    return new FiscalYearService(ledgerService, fiscalYearRestClient);
  }

  @Bean
  FundDetailsService fundDetailsService(FiscalYearService fiscalYearService, FundService fundService
    , BudgetService budgetService, ExpenseClassService expenseClassService, BudgetExpenseClassService budgetExpenseClassService){
    return new FundDetailsService(fiscalYearService, fundService, budgetService, expenseClassService, budgetExpenseClassService);
  }

  @Bean
  FundService fundService(RestClient fundStorageRestClient) {
    return new FundService(fundStorageRestClient);
  }

}
