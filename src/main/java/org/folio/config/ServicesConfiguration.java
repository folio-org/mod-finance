package org.folio.config;

import org.folio.rest.core.RestClient;
import org.folio.services.ExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassService;
import org.folio.services.budget.BudgetExpenseClassTotalsService;
import org.folio.services.budget.BudgetService;
import org.folio.services.budget.RecalculateBudgetService;
import org.folio.services.budget.CreateBudgetService;
import org.folio.services.configuration.ConfigurationEntriesService;
import org.folio.services.financedata.FinanceDataService;
import org.folio.services.financedata.FinanceDataValidator;
import org.folio.services.fiscalyear.FiscalYearApiService;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.fund.FundCodeExpenseClassesService;
import org.folio.services.fund.FundDetailsService;
import org.folio.services.fund.FundFiscalYearService;
import org.folio.services.fund.FundService;
import org.folio.services.fund.FundUpdateLogService;
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
import org.folio.services.transactions.TransactionApiService;
import org.folio.services.transactions.TransactionService;
import org.folio.services.transactions.TransactionTotalService;
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
  BudgetExpenseClassService budgetExpenseClassService(RestClient restClient, TransactionService transactionService) {
    return new BudgetExpenseClassService(restClient, transactionService);
  }

  @Bean
  TransactionApiService transactionApiService(TransactionService transactionService, FundService fundService) {
    return new TransactionApiService(transactionService, fundService);
  }

  @Bean
  TransactionService transactionService(RestClient restClient, FiscalYearService fiscalYearService) {
    return new TransactionService(restClient, fiscalYearService);
  }

  @Bean
  TransactionTotalService transactionTotalService(RestClient restClient) {
    return new TransactionTotalService(restClient);
  }

  @Bean
  BudgetExpenseClassTotalsService budgetExpenseClassTotalsService(RestClient restClient,
                                                                  ExpenseClassService expenseClassService,
                                                                  TransactionService transactionService,
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
  FiscalYearApiService fiscalYearApiService(FiscalYearService fiscalYearService, ConfigurationEntriesService configurationEntriesService,
      BudgetService budgetService, AcqUnitsService acqUnitsService){
    return new FiscalYearApiService(fiscalYearService, configurationEntriesService, budgetService, acqUnitsService);
  }

  @Bean
  FiscalYearService fiscalYearService(RestClient restClient){
    return new FiscalYearService(restClient);
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
                                          TransactionTotalService transactionTotalService) {
    return new LedgerTotalsService(fiscalYearService, budgetService, transactionTotalService);
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
  GroupExpenseClassTotalsService groupExpenseClassTotalsService(GroupFundFiscalYearService groupFundFiscalYearService, TransactionService transactionService, ExpenseClassService expenseClassService) {
    return new GroupExpenseClassTotalsService(groupFundFiscalYearService, transactionService, expenseClassService);
  }

  @Bean
  LedgerDetailsService ledgerDetailsService(FiscalYearService fiscalYearService, LedgerService ledgerService, ConfigurationEntriesService configurationEntriesService) {
    return new LedgerDetailsService(fiscalYearService, ledgerService, configurationEntriesService);
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
                                                             TransactionService transactionService,
                                                             FundDetailsService fundDetailsService) {
    return new CreateBudgetService(restClient, groupFundFiscalYearService, fundFiscalYearService,
      budgetExpenseClassService, transactionService,  fundDetailsService);
  }

  @Bean
  GroupFiscalYearTotalsService groupFiscalYearTotalsService(RestClient restClient, GroupFundFiscalYearService groupFundFiscalYearService,
                                                            TransactionService transactionService) {
    return new GroupFiscalYearTotalsService(restClient, groupFundFiscalYearService, transactionService);
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
  public RecalculateBudgetService recalculateBudgetService(BudgetService budgetService, TransactionService transactionService) {
    return new RecalculateBudgetService(budgetService, transactionService);
  }

  @Bean
  FundCodeExpenseClassesService fundCodeExpenseClassesService(BudgetService budgetService, BudgetExpenseClassService budgetExpenseClassService,
                                                              FundService fundService, LedgerService ledgerService,
                                                              FiscalYearService fiscalYearService, LedgerDetailsService ledgerDetailsService,
                                                              ExpenseClassService expenseClassService) {
    return new FundCodeExpenseClassesService(budgetService, budgetExpenseClassService,
      fundService, ledgerService, fiscalYearService, ledgerDetailsService, expenseClassService);
  }

  @Bean
  FundUpdateLogService fundUpdateLogService(RestClient restClient) {
    return new FundUpdateLogService(restClient);
  }

  @Bean
  FinanceDataService financeDataService(RestClient restClient, AcqUnitsService acqUnitsService,
                                        FundUpdateLogService fundUpdateLogService, FinanceDataValidator financeDataValidator) {
    return new FinanceDataService(restClient, acqUnitsService, fundUpdateLogService, financeDataValidator);
  }
}
