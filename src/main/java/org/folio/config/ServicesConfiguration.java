package org.folio.config;

import java.util.Set;

import org.folio.rest.core.RestClient;
import org.folio.services.ConfigurationService;
import org.folio.services.ExpenseClassService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.GroupExpenseClassTotalsService;
import org.folio.services.GroupFiscalYearTotalsService;
import org.folio.services.GroupFundFiscalYearService;
import org.folio.services.LedgerDetailsService;
import org.folio.services.LedgerRolloverErrorsService;
import org.folio.services.LedgerRolloverProgressService;
import org.folio.services.LedgerRolloverService;
import org.folio.services.LedgerService;
import org.folio.services.LedgerTotalsService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassTotalsService;
import org.folio.services.budget.BudgetService;
import org.folio.services.budget.CreateBudgetService;
import org.folio.services.fund.FundDetailsService;
import org.folio.services.fund.FundFiscalYearService;
import org.folio.services.fund.FundService;
import org.folio.services.transactions.AllocationService;
import org.folio.services.transactions.BaseTransactionService;
import org.folio.services.transactions.CommonTransactionService;
import org.folio.services.transactions.CreditService;
import org.folio.services.transactions.EncumbranceService;
import org.folio.services.transactions.PaymentService;
import org.folio.services.transactions.PendingPaymentService;
import org.folio.services.transactions.TransactionManagingService;
import org.folio.services.transactions.TransactionRestrictService;
import org.folio.services.transactions.TransactionService;
import org.folio.services.transactions.TransactionStrategyFactory;
import org.folio.services.transactions.TransactionTypeManagingStrategy;
import org.folio.services.transactions.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class ServicesConfiguration {
  @Bean
  @Autowired
  ExpenseClassService expenseClassService(RestClient expenseClassRestClient) {
    return new ExpenseClassService(expenseClassRestClient);
  }

  @Bean
  BudgetExpenseClassService budgetExpenseClassService(RestClient budgetExpenseClassRestClient, CommonTransactionService transactionService) {
    return new BudgetExpenseClassService(budgetExpenseClassRestClient, transactionService);
  }

  @Bean
  CommonTransactionService transactionService(RestClient transactionRestClient, RestClient fiscalYearRestClient) {
    return new CommonTransactionService(transactionRestClient, fiscalYearRestClient);
  }

  @Bean
  BudgetExpenseClassTotalsService budgetExpenseClassTotalsService(RestClient budgetRestClient,
                                                                  ExpenseClassService expenseClassService,
                                                                  CommonTransactionService transactionService,
                                                                  BudgetExpenseClassService budgetExpenseClassService)  {
    return new BudgetExpenseClassTotalsService(budgetRestClient, expenseClassService, transactionService, budgetExpenseClassService);
  }

  @Bean
  GroupFundFiscalYearService groupFundFiscalYearService(RestClient groupFundFiscalYearRestClient) {
    return new GroupFundFiscalYearService(groupFundFiscalYearRestClient);
  }

  @Bean
  FundDetailsService fundDetailsService(BudgetService budgetService, ExpenseClassService expenseClassService,
                                            BudgetExpenseClassService budgetExpenseClassService, FundFiscalYearService fundFiscalYearService){
    return new FundDetailsService(budgetService, expenseClassService, budgetExpenseClassService, fundFiscalYearService);
  }

  @Bean
  BudgetService budgetService(RestClient budgetRestClient,
                              BudgetExpenseClassService budgetExpenseClassService) {
    return new BudgetService(budgetRestClient, budgetExpenseClassService);
  }

  @Bean
  FiscalYearService fiscalYearService(RestClient fiscalYearRestClient, ConfigurationService configurationService, BudgetService budgetService){
    return new FiscalYearService(fiscalYearRestClient, configurationService, budgetService);
  }

  @Bean
  LedgerService ledgerService(RestClient ledgerStorageRestClient, LedgerTotalsService ledgerTotalsService) {
    return new LedgerService(ledgerStorageRestClient, ledgerTotalsService);
  }

  @Bean
  LedgerRolloverService ledgerRolloverService(RestClient ledgerRolloverStorageRestClient) {
    return new LedgerRolloverService(ledgerRolloverStorageRestClient);
  }

  @Bean
  LedgerRolloverErrorsService ledgerRolloverErrorsService(RestClient ledgerRolloverErrorsStorageRestClient) {
    return new LedgerRolloverErrorsService(ledgerRolloverErrorsStorageRestClient);
  }

  @Bean
  LedgerRolloverProgressService ledgerRolloverProgressService(RestClient ledgerRolloverProgressStorageRestClient) {
    return new LedgerRolloverProgressService(ledgerRolloverProgressStorageRestClient);
  }

  @Bean
  LedgerTotalsService ledgerTotalsService(FiscalYearService fiscalYearService, BudgetService budgetService) {
    return new LedgerTotalsService(fiscalYearService, budgetService);
  }

  @Bean
  FundService fundService(RestClient fundStorageRestClient) {
    return new FundService(fundStorageRestClient);
  }

  @Bean
  GroupExpenseClassTotalsService groupExpenseClassTotalsService(GroupFundFiscalYearService groupFundFiscalYearService, CommonTransactionService transactionService, ExpenseClassService expenseClassService) {
    return new GroupExpenseClassTotalsService(groupFundFiscalYearService, transactionService, expenseClassService);
  }

  @Bean
  LedgerDetailsService ledgerDetailsService(FiscalYearService fiscalYearService, LedgerService ledgerService) {
    return new LedgerDetailsService(fiscalYearService, ledgerService);
  }

  @Bean
  TransactionService baseTransactionService(RestClient transactionRestClient) {
    return new BaseTransactionService(transactionRestClient);
  }

  @Bean
  TransactionRestrictService transactionRestrictService(FundService fundService) {
    return new TransactionRestrictService(fundService);
  }

  @Bean
  TransactionManagingService allocationService(BaseTransactionService transactionService, TransactionRestrictService transactionRestrictService) {
    return new AllocationService(transactionService, transactionRestrictService);
  }

  @Bean
  TransactionManagingService creditService(BaseTransactionService transactionService) {
    return new CreditService(transactionService);
  }

  @Bean
  TransactionManagingService paymentService(BaseTransactionService transactionService) {
    return new PaymentService(transactionService);
  }

  @Bean
  TransactionManagingService encumbranceService(BaseTransactionService transactionService) {
    return new EncumbranceService(transactionService);
  }

  @Bean
  TransactionManagingService transferService(BaseTransactionService transactionService, TransactionRestrictService transactionRestrictService) {
    return new TransferService(transactionService, transactionRestrictService);
  }

  @Bean
  TransactionManagingService pendingPaymentService(BaseTransactionService transactionService) {
    return new PendingPaymentService(transactionService);
  }

  @Bean
  TransactionService commonTransactionService(RestClient transactionRestClient, RestClient fiscalYearRestClient) {
    return new CommonTransactionService(transactionRestClient, fiscalYearRestClient);
  }

  @Bean
  TransactionStrategyFactory transactionStrategyFactory(Set<TransactionTypeManagingStrategy> transactionTypeManagingStrategies) {
    return new TransactionStrategyFactory(transactionTypeManagingStrategies);
  }

  @Bean
  FundFiscalYearService fundFiscalYearService(LedgerDetailsService ledgerDetailsService, FundService fundService) {
    return new FundFiscalYearService(ledgerDetailsService, fundService);
  }

  @Bean
  CreateBudgetService createBudgetService(RestClient budgetRestClient,
                                                             GroupFundFiscalYearService groupFundFiscalYearService,
                                                             FundFiscalYearService fundFiscalYearService,
                                                             BudgetExpenseClassService budgetExpenseClassService,
                                                             CommonTransactionService transactionService,
                                                             FundDetailsService fundDetailsService) {
    return new CreateBudgetService( budgetRestClient, groupFundFiscalYearService, fundFiscalYearService,
                                          budgetExpenseClassService, transactionService,  fundDetailsService);
  }

  @Bean
  GroupFiscalYearTotalsService groupFiscalYearTotalsService(RestClient budgetRestClient, GroupFundFiscalYearService groupFundFiscalYearService) {
    return new GroupFiscalYearTotalsService(budgetRestClient, groupFundFiscalYearService);
  }

  @Bean
  ConfigurationService configurationService(RestClient configEntriesRestClient) {
    return new ConfigurationService(configEntriesRestClient);
  }

}
