package org.folio.config;

import java.util.Set;

import org.folio.rest.core.RestClient;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassTotalsService;
import org.folio.services.budget.BudgetService;
import org.folio.services.budget.RecalculateBudgetService;
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
import org.folio.services.ledger.LedgerRolloverBudgetsService;
import org.folio.services.ledger.LedgerRolloverErrorsService;
import org.folio.services.ledger.LedgerRolloverLogsService;
import org.folio.services.ledger.LedgerRolloverProgressService;
import org.folio.services.ledger.LedgerRolloverService;
import org.folio.services.ledger.LedgerService;
import org.folio.services.ledger.LedgerTotalsService;
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

public class ServicesConfiguration {

  @Bean
  RestClient restClient() {
    return new RestClient();
  }

  @Bean
  @Autowired
  ExpenseClassService expenseClassService(RestClient restClient) {
    return new ExpenseClassService(restClient);
  }

  @Bean
  BudgetExpenseClassService budgetExpenseClassService(RestClient restClient, CommonTransactionService transactionService) {
    return new BudgetExpenseClassService(restClient, transactionService);
  }

  @Bean
  CommonTransactionService transactionService(RestClient restClient) {
    return new CommonTransactionService(restClient);
  }

  @Bean
  BudgetExpenseClassTotalsService budgetExpenseClassTotalsService(RestClient restClient,
                                                                  ExpenseClassService expenseClassService,
                                                                  CommonTransactionService transactionService,
                                                                  BudgetExpenseClassService budgetExpenseClassService)  {
    return new BudgetExpenseClassTotalsService(restClient, expenseClassService, transactionService, budgetExpenseClassService);
  }

  @Bean
  GroupFundFiscalYearService groupFundFiscalYearService(RestClient restClient) {
    return new GroupFundFiscalYearService(restClient);
  }

  @Bean
  FundDetailsService fundDetailsService(BudgetService budgetService, ExpenseClassService expenseClassService,
                                        BudgetExpenseClassService budgetExpenseClassService,
                                        FundFiscalYearService fundFiscalYearService, FiscalYearService fiscalYearService) {
    return new FundDetailsService(budgetService, expenseClassService, budgetExpenseClassService, fundFiscalYearService, fiscalYearService);
  }

  @Bean
  BudgetService budgetService(RestClient restClient,
                              BudgetExpenseClassService budgetExpenseClassService) {
    return new BudgetService(restClient, budgetExpenseClassService);
  }

  @Bean
  FiscalYearService fiscalYearService(RestClient restClient, ConfigurationEntriesService configurationEntriesService, BudgetService budgetService, AcqUnitsService acqUnitsService){
    return new FiscalYearService(restClient, configurationEntriesService, budgetService, acqUnitsService);
  }

  @Bean
  LedgerService ledgerService(RestClient restClient, LedgerTotalsService ledgerTotalsService, AcqUnitsService acqUnitsService) {
    return new LedgerService(restClient, ledgerTotalsService, acqUnitsService);
  }

  @Bean
  LedgerRolloverService ledgerRolloverService(RestClient restClient) {
    return new LedgerRolloverService(restClient);
  }

  @Bean
  LedgerRolloverErrorsService ledgerRolloverErrorsService(RestClient restClient) {
    return new LedgerRolloverErrorsService(restClient);
  }

  @Bean
  LedgerRolloverProgressService ledgerRolloverProgressService(RestClient restClient) {
    return new LedgerRolloverProgressService(restClient);
  }

  @Bean
  LedgerRolloverLogsService ledgerRolloverLogsService(RestClient restClient) {
    return new LedgerRolloverLogsService(restClient);
  }

  @Bean
  LedgerRolloverBudgetsService ledgerRolloverBudgetsService(RestClient restClient) {
    return new LedgerRolloverBudgetsService(restClient);
  }

  @Bean
  LedgerTotalsService ledgerTotalsService(FiscalYearService fiscalYearService, BudgetService budgetService,
                                          BaseTransactionService baseTransactionService) {
    return new LedgerTotalsService(fiscalYearService, budgetService, baseTransactionService);
  }

  @Bean
  FundService fundService(RestClient restClient, AcqUnitsService acquisitionUnitsService) {
    return new FundService(restClient, acquisitionUnitsService);
  }

  @Bean
  GroupService groupService(RestClient restClient, AcqUnitsService acquisitionUnitsService) {
    return new GroupService(restClient, acquisitionUnitsService);
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
  TransactionService baseTransactionService(RestClient restClient) {
    return new BaseTransactionService(restClient);
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
  TransactionService commonTransactionService(RestClient restClient) {
    return new CommonTransactionService(restClient);
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
  CreateBudgetService createBudgetService(RestClient restClient,
                                                             GroupFundFiscalYearService groupFundFiscalYearService,
                                                             FundFiscalYearService fundFiscalYearService,
                                                             BudgetExpenseClassService budgetExpenseClassService,
                                                             CommonTransactionService transactionService,
                                                             FundDetailsService fundDetailsService) {
    return new CreateBudgetService(restClient, groupFundFiscalYearService, fundFiscalYearService,
                                          budgetExpenseClassService, transactionService,  fundDetailsService);
  }

  @Bean
  GroupFiscalYearTotalsService groupFiscalYearTotalsService(RestClient restClient, GroupFundFiscalYearService groupFundFiscalYearService,
                                                            BaseTransactionService baseTransactionService) {
    return new GroupFiscalYearTotalsService(restClient, groupFundFiscalYearService, baseTransactionService);
  }

  @Bean
  ConfigurationEntriesService configurationService(RestClient restClient) {
    return new ConfigurationEntriesService(restClient);
  }

  @Bean
  public AcqUnitMembershipsService acqUnitMembershipsService(RestClient restClient) {
    return new AcqUnitMembershipsService(restClient);
  }

  @Bean
  public AcqUnitsService acqUnitsService(RestClient restClient, AcqUnitMembershipsService acqUnitMembershipsService) {
    return new AcqUnitsService(restClient, acqUnitMembershipsService);
  }

  @Bean
  public ProtectionService protectionService(AcqUnitsService acqUnitsService, AcqUnitMembershipsService acqUnitMembershipsService) {
    return new ProtectionService(acqUnitsService, acqUnitMembershipsService);
  }

  @Bean
  public RecalculateBudgetService recalculateBudgetService(BudgetService budgetService, CommonTransactionService commonTransactionService) {
    return new RecalculateBudgetService(budgetService, commonTransactionService);
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
