package org.folio.services.fund;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.FundUpdateLog;
import org.folio.rest.jaxrs.model.FundUpdateLogCollection;
import org.folio.rest.jaxrs.model.JobNumber;
import org.folio.services.protection.AcqUnitsService;
import org.folio.util.CopilotGenerated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@CopilotGenerated(partiallyGenerated = true)
public class FundUpdateLogServiceTest {

  @Mock private RestClient restClient;
  @Mock private AcqUnitsService acqUnitsService;
  @InjectMocks private FundUpdateLogService fundUpdateLogService;

  private AutoCloseable openMocks;
  private RequestContext requestContext;

  @BeforeEach
  void setUp() {
    openMocks = MockitoAnnotations.openMocks(this);

    var okapiHeaders = new HashMap<String, String>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());

    requestContext = new RequestContext(Vertx.vertx().getOrCreateContext(), okapiHeaders);
  }

  @AfterEach
  @SneakyThrows
  void afterEach() {
    if (Objects.nonNull(openMocks)) {
      openMocks.close();
    }
  }

  private static Stream<Arguments> testGetFundUpdateLogsArgs() {
    return Stream.of(
      arguments(null, new FundUpdateLogCollection().withTotalRecords(2)),
      arguments("", new FundUpdateLogCollection().withTotalRecords(2)),
      arguments("clause", new FundUpdateLogCollection().withTotalRecords(1))
    );
  }

  @ParameterizedTest
  @MethodSource("testGetFundUpdateLogsArgs")
  void testGetFundUpdateLogs(String clause, FundUpdateLogCollection expectedCollection, VertxTestContext testContext) {
    when(acqUnitsService.buildAcqUnitsCqlClause(any())).thenReturn(Future.succeededFuture(clause));
    when(restClient.get(anyString(), any(), any())).thenReturn(Future.succeededFuture(expectedCollection));

    fundUpdateLogService.getFundUpdateLogs("query", 0, 10, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertEquals(expectedCollection.getTotalRecords(), result.getTotalRecords());
        testContext.completeNow();
      })));
  }

  @Test
  void testGetFundUpdateLogById(VertxTestContext testContext) {
    var fundUpdateLog = new FundUpdateLog().withId("1");
    when(restClient.get(anyString(), any(), any())).thenReturn(Future.succeededFuture(fundUpdateLog));

    fundUpdateLogService.getFundUpdateLogById("1", requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertEquals("1", result.getId());
        testContext.completeNow();
      })));
  }

  @Test
  void testCreateFundUpdateLog(VertxTestContext testContext) {
    var fundUpdateLog = new FundUpdateLog().withId("1");
    when(restClient.post(anyString(), any(), any(), any())).thenReturn(Future.succeededFuture(fundUpdateLog));

    fundUpdateLogService.createFundUpdateLog(fundUpdateLog, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertEquals("1", result.getId());
        testContext.completeNow();
      })));
  }

  @Test
  void testUpdateFundUpdateLog(VertxTestContext testContext) {
    var fundUpdateLog = new FundUpdateLog().withId("1");
    when(restClient.put(anyString(), any(), any())).thenReturn(Future.succeededFuture(null));

    fundUpdateLogService.updateFundUpdateLog(fundUpdateLog, requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(testContext::completeNow)));
  }

  @Test
  void testDeleteFundUpdateLog(VertxTestContext testContext) {
    when(restClient.delete(anyString(), any())).thenReturn(Future.succeededFuture(null));

    fundUpdateLogService.deleteFundUpdateLog("1", requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(testContext::completeNow)));
  }

  @Test
  void testGetJobNumber(VertxTestContext testContext) {
    var expectedJobNumber = new JobNumber().withType(JobNumber.Type.FUND_UPDATE_LOGS);
    when(restClient.get(anyString(), eq(JobNumber.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture(expectedJobNumber));

    fundUpdateLogService.getJobNumber(requestContext)
      .onComplete(testContext.succeeding(result -> testContext.verify(() -> {
        assertNotNull(result);
        assertEquals(expectedJobNumber.getType(), result.getType());
        testContext.completeNow();
      })));
  }
}
