package org.folio.config;

import org.folio.rest.core.RestClient;
import org.folio.services.BudgetExpenseClassService;
import org.folio.services.BudgetExpenseClassTotalsService;
import org.folio.services.BudgetService;
import org.folio.services.CurrentFiscalYearService;
import org.folio.services.ExpenseClassService;
import org.folio.services.FiscalYearService;
import org.folio.services.FundDetailsService;
import org.folio.services.FundService;
import org.folio.services.GroupExpenseClassTotalsService;
import org.folio.services.GroupFundFiscalYearService;
import org.folio.services.LedgerService;
import org.folio.services.LedgerTotalsService;
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
  FiscalYearService fiscalYearService(RestClient fiscalYearRestClient){
    return new FiscalYearService(fiscalYearRestClient);
  }

  @Bean
  LedgerService ledgerService(RestClient ledgerStorageRestClient, LedgerTotalsService ledgerTotalsService) {
    return new LedgerService(ledgerStorageRestClient, ledgerTotalsService);
  }

  @Bean
  LedgerTotalsService ledgerTotalsService(FiscalYearService fiscalYearService, BudgetService budgetService) {
    return new LedgerTotalsService(fiscalYearService, budgetService);
  }

  @Bean
  FundDetailsService fundDetailsService(CurrentFiscalYearService fiscalYearService, FundService fundService
    , BudgetService budgetService, ExpenseClassService expenseClassService, BudgetExpenseClassService budgetExpenseClassService){
    return new FundDetailsService(fiscalYearService, fundService, budgetService, expenseClassService, budgetExpenseClassService);
  }

  @Bean
  FundService fundService(RestClient fundStorageRestClient) {
    return new FundService(fundStorageRestClient);
  }

  @Bean
  GroupExpenseClassTotalsService groupExpenseClassTotalsService(GroupFundFiscalYearService groupFundFiscalYearService, TransactionService transactionService, ExpenseClassService expenseClassService) {
    return new GroupExpenseClassTotalsService(groupFundFiscalYearService, transactionService, expenseClassService);
  }

  @Bean
  CurrentFiscalYearService currentFiscalYearService(FiscalYearService fiscalYearService, LedgerService ledgerService) {
    return new CurrentFiscalYearService(fiscalYearService, ledgerService);
  }

}
