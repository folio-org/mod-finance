package org.folio.config;

import org.folio.dao.ExpenseClassDAO;
import org.folio.dao.ExpenseClassHttpDAO;
import org.folio.services.ExpenseClassService;
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
}
