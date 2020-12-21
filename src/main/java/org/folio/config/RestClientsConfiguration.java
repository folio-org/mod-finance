package org.folio.config;

import org.folio.rest.core.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.folio.rest.util.ResourcePathResolver.*;

@Configuration
public class RestClientsConfiguration {

  @Bean
  RestClient budgetRestClient() {
    return new RestClient(resourcesPath(BUDGETS_STORAGE));
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
    return new RestClient(resourcesPath(FISCAL_YEARS_STORAGE));
  }

  @Bean
  RestClient groupFundFiscalYearRestClient() {
    return new RestClient(resourcesPath(GROUP_FUND_FISCAL_YEARS));
  }

  @Bean
  RestClient fundStorageRestClient() {
    return new RestClient(resourcesPath(FUNDS_STORAGE));
  }

  @Bean
  RestClient ledgerStorageRestClient() {
    return new RestClient(resourcesPath(LEDGERS_STORAGE));
  }

  @Bean
  RestClient ledgerRolloverStorageRestClient() {
    return new RestClient(resourcesPath(LEDGER_ROLLOVERS_STORAGE));
  }

  @Bean
  RestClient ledgerRolloverProgressStorageRestClient() {
    return new RestClient(resourcesPath(LEDGER_ROLLOVERS_PROGRESS_STORAGE));
  }

  @Bean
  RestClient ledgerRolloverErrorsStorageRestClient() {
    return new RestClient(resourcesPath(LEDGER_ROLLOVERS_ERRORS_STORAGE));
  }

}
