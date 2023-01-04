package org.folio.config;

import org.folio.rest.core.RestClient;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassTotalsService;
import org.folio.services.budget.BudgetService;
import org.folio.services.budget.CreateBudgetService;
import org.folio.services.configuration.ConfigurationEntriesService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.fund.FundCodeExpenseClassesService;
import org.folio.services.fund.FundDetailsService;
import org.folio.services.fund.FundFiscalYearService;
import org.folio.services.fund.FundService;
import org.folio.services.group.GroupExpenseClassTotalsService;
import org.folio.services.group.GroupFiscalYearTotalsService;
import org.folio.services.group.GroupFundFiscalYearService;
import org.folio.services.group.GroupService;
import org.folio.services.ledger.LedgerDetailsService;
import org.folio.services.ledger.LedgerRolloverErrorsService;
import org.folio.services.ledger.LedgerRolloverProgressService;
import org.folio.services.ledger.LedgerRolloverService;
import org.folio.services.ledger.LedgerService;
import org.folio.services.ledger.LedgerTotalsService;
import org.folio.services.ledger.LedgerRolloverLogsService;
import org.folio.services.ledger.LedgerRolloverBudgetsService;
import org.folio.services.protection.AcqUnitMembershipsService;
import org.folio.services.protection.AcqUnitsService;
import org.folio.services.protection.ProtectionService;
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

import java.util.Set;

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
  CommonTransactionService transactionService(RestClient transactionRestClient, RestClient fiscalYearRestClient, RestClient orderTransactionSummaryRestClient) {
    return new CommonTransactionService(transactionRestClient, fiscalYearRestClient, orderTransactionSummaryRestClient);
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
  FiscalYearService fiscalYearService(RestClient fiscalYearRestClient,ConfigurationEntriesService configurationEntriesService, BudgetService budgetService, AcqUnitsService acqUnitsService){
    return new FiscalYearService(fiscalYearRestClient, configurationEntriesService, budgetService, acqUnitsService);
  }

  @Bean
  LedgerService ledgerService(RestClient ledgerStorageRestClient, LedgerTotalsService ledgerTotalsService, AcqUnitsService acqUnitsService) {
    return new LedgerService(ledgerStorageRestClient, ledgerTotalsService, acqUnitsService);
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
  LedgerRolloverLogsService ledgerRolloverLogsService(RestClient ledgerRolloverLogsStorageRestClient) {
    return new LedgerRolloverLogsService(ledgerRolloverLogsStorageRestClient);
  }

  @Bean
  LedgerRolloverBudgetsService ledgerRolloverBudgetsService(RestClient ledgerRolloverBudgetsStorageRestClient) {
    return new LedgerRolloverBudgetsService(ledgerRolloverBudgetsStorageRestClient);
  }

  @Bean
  LedgerTotalsService ledgerTotalsService(FiscalYearService fiscalYearService, BudgetService budgetService,
                                          BaseTransactionService baseTransactionService) {
    return new LedgerTotalsService(fiscalYearService, budgetService, baseTransactionService);
  }

  @Bean
  FundService fundService(RestClient fundStorageRestClient, AcqUnitsService acquisitionUnitsService) {
    return new FundService(fundStorageRestClient, acquisitionUnitsService);
  }

  @Bean
  GroupService groupService(RestClient groupStorageRestClient, AcqUnitsService acquisitionUnitsService) {
    return new GroupService(groupStorageRestClient, acquisitionUnitsService);
  }

  @Bean
  GroupExpenseClassTotalsService groupExpenseClassTotalsService(GroupFundFiscalYearService groupFundFiscalYearService, CommonTransactionService transactionService, ExpenseClassService expenseClassService) {
    return new GroupExpenseClassTotalsService(groupFundFiscalYearService, transactionService, expenseClassService);
  }

  @Bean
  LedgerDetailsService ledgerDetailsService(FiscalYearService fiscalYearService, LedgerService ledgerService, ConfigurationEntriesService configurationEntriesService) {
    return new LedgerDetailsService(fiscalYearService, ledgerService, configurationEntriesService);
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
  TransactionManagingService encumbranceService(BaseTransactionService transactionService,
      CommonTransactionService commonTransactionService) {
    return new EncumbranceService(transactionService, commonTransactionService);
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
  TransactionService commonTransactionService(RestClient transactionRestClient, RestClient fiscalYearRestClient, RestClient orderTransactionSummaryRestClient) {
    return new CommonTransactionService(transactionRestClient, fiscalYearRestClient, orderTransactionSummaryRestClient);
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
  GroupFiscalYearTotalsService groupFiscalYearTotalsService(RestClient budgetRestClient, GroupFundFiscalYearService groupFundFiscalYearService,
                                                            BaseTransactionService baseTransactionService) {
    return new GroupFiscalYearTotalsService(budgetRestClient, groupFundFiscalYearService, baseTransactionService);
  }

  @Bean
  ConfigurationEntriesService configurationService(RestClient configEntriesRestClient) {
    return new ConfigurationEntriesService(configEntriesRestClient);
  }

  @Bean
  public AcqUnitMembershipsService acqUnitMembershipsService(RestClient acqUnitMembershipsRestClient) {
    return new AcqUnitMembershipsService(acqUnitMembershipsRestClient);
  }

  @Bean
  public AcqUnitsService acqUnitsService(RestClient acqUnitsStorageRestClient, AcqUnitMembershipsService acqUnitMembershipsService) {
    return new AcqUnitsService(acqUnitsStorageRestClient, acqUnitMembershipsService);
  }

  @Bean
  public ProtectionService protectionService(AcqUnitsService acqUnitsService, AcqUnitMembershipsService acqUnitMembershipsService) {
    return new ProtectionService(acqUnitsService, acqUnitMembershipsService);
  }

  @Bean
  FundCodeExpenseClassesService fundCodeExpenseClassesService(BudgetService budgetService, BudgetExpenseClassService budgetExpenseClassService,
                                                              FundService fundService, LedgerService ledgerService,
                                                              FiscalYearService fiscalYearService, LedgerDetailsService ledgerDetailsService,
                                                              ExpenseClassService expenseClassService) {
    return new FundCodeExpenseClassesService(budgetService, budgetExpenseClassService,
      fundService, ledgerService, fiscalYearService, ledgerDetailsService, expenseClassService);
  }
}
