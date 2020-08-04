package org.folio.config;

import org.folio.rest.core.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS;
import static org.folio.rest.util.ResourcePathResolver.BUDGET_EXPENSE_CLASSES;
import static org.folio.rest.util.ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.LEDGERS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

@Configuration
@ComponentScan({"org.folio"})
public class RestClientConfiguration {
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
  RestClient fiscalYearStorageRestClient() {
    return new RestClient(resourcesPath(FISCAL_YEARS_STORAGE));
  }

  @Bean
  RestClient groupFundFiscalYearRestClient() {
    return new RestClient(resourcesPath(GROUP_FUND_FISCAL_YEARS));
  }

  @Bean
  RestClient ledgerStorageRestClient() {
    return new RestClient(resourcesPath(LEDGERS_STORAGE));
  }
}
