package org.folio.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan({"org.folio"})
@Import({ServicesConfiguration.class})
public class ApplicationConfig {


}
