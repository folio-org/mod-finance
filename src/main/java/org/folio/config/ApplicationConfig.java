package org.folio.config;

import org.folio.dao.BudgetDAO;
import org.folio.dao.BudgetExpenseClassDAO;
import org.folio.dao.BudgetExpenseClassHttpDAO;
import org.folio.dao.BudgetHttpDAO;
import org.folio.dao.ExpenseClassDAO;
import org.folio.dao.ExpenseClassHttpDAO;
import org.folio.dao.TransactionDAO;
import org.folio.dao.TransactionHttpDAO;
import org.folio.services.BudgetExpenseClassService;
import org.folio.services.BudgetExpenseClassTotalsService;
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
  public ExpenseClassDAO expenseClassDAO() {
    return new ExpenseClassHttpDAO() ;
  }

  @Bean
  @Autowired
  public ExpenseClassService expenseClassService(ExpenseClassDAO expenseClassDAO) {
    return new ExpenseClassService(expenseClassDAO);
  }

  @Bean
  public BudgetDAO budgetDAO() {
    return new BudgetHttpDAO();
  }

  @Bean
  public TransactionDAO transactionDAO() {
    return new TransactionHttpDAO();
  }

  @Bean
  public BudgetExpenseClassDAO budgetExpenseClassDAO() {
    return new BudgetExpenseClassHttpDAO();
  }

  @Bean
  public BudgetExpenseClassService budgetExpenseClassService(BudgetExpenseClassDAO budgetExpenseClassDAO) {
    return new BudgetExpenseClassService(budgetExpenseClassDAO);
  }

  @Bean
  public TransactionService transactionService(TransactionDAO transactionDAO) {
    return new TransactionService(transactionDAO);
  }

  @Bean
  public BudgetExpenseClassTotalsService budgetExpenseClassTotalsService(BudgetDAO budgetDAO,
                                                                         ExpenseClassService expenseClassService,
                                                                         TransactionService transactionService,
                                                                         BudgetExpenseClassService budgetExpenseClassService)  {
    return new BudgetExpenseClassTotalsService(budgetDAO, expenseClassService, transactionService, budgetExpenseClassService);
  }
}
