package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.ApiTestSuite;
import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FundUpdateLogCollection;
import org.folio.rest.util.RestTestUtils;
import org.folio.services.fund.FundUpdateLogService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.util.MockServer.addMockEntry;
import static org.folio.rest.util.TestConfig.autowireDependencies;
import static org.folio.rest.util.TestConfig.clearVertxContext;
import static org.folio.rest.util.TestConfig.initSpringContext;
import static org.folio.rest.util.TestConfig.isVerticleNotDeployed;
import static org.folio.rest.util.TestEntities.FUND_UPDATE_LOG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class FundUpdateLogApiTest {

  private static boolean runningOnOwn;

  @Autowired public FundUpdateLogService fundUpdateLogService;

  public static final String GROUP_ENDPOINT = "finance/fund-update-logs";

  @BeforeAll
  static void init() throws InterruptedException, ExecutionException, TimeoutException {
    if (isVerticleNotDeployed()) {
      ApiTestSuite.before();
      runningOnOwn = true;
    }
    initSpringContext(ContextConfiguration.class);
  }

  @BeforeEach
  void beforeEach() {
    autowireDependencies(this);
  }

  @AfterAll
  static void afterAll() {
    if (runningOnOwn) {
      ApiTestSuite.after();
    }
    clearVertxContext();
  }

  @AfterEach
  void resetMocks() {
    reset(fundUpdateLogService);
  }

  @Test
  void testGetFundUpdateLog() {
    var fundUpdateLogCollection = new FundUpdateLogCollection()
      .withFundUpdateLogs(List.of(FUND_UPDATE_LOG.getMockObject().mapTo(FundUpdateLog.class)))
      .withTotalRecords(1);

    addMockEntry(FUND_UPDATE_LOG.name(), JsonObject.mapFrom(fundUpdateLogCollection));
    when(fundUpdateLogService.getFundUpdateLogs(anyString(), anyInt(), anyInt(), any()))
      .thenReturn(succeededFuture(fundUpdateLogCollection));

    RestTestUtils.verifyGetWithParam(GROUP_ENDPOINT, APPLICATION_JSON, HttpStatus.HTTP_OK.toInt(),
        Map.ofEntries(Map.entry("query", "recordsCount=IN_PROGRESS"), Map.entry("offset", 0), Map.entry("limit", 10)))
      .as(FundUpdateLogCollection.class);
  }

  static class ContextConfiguration {

    @Bean
    FundUpdateLogService fundUpdateLogService() {
      return mock(FundUpdateLogService.class);
    }
  }
}
