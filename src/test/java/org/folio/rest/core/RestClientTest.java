package org.folio.rest.core;

import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearBatchRequest;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRollover.RolloverType;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.CopilotGenerated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@CopilotGenerated(model = "Claude Sonnet 4.5")
@ExtendWith(VertxExtension.class)
public class RestClientTest {
  public static final Header X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, "restclienttest");
  public static final Header X_OKAPI_TOKEN = new Header(OKAPI_HEADER_TOKEN, "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6Ijg3MTIyM2Q1LTYxZDEtNWRiZi1hYTcxLWVhNTcwOTc5MTQ1NSIsImlhdCI6MTU4NjUyMDA0NywidGVuYW50IjoiZGlrdSJ9._qlH5LDM_FaTH8MxIHKua-zsLmrBY7vpcJ-WrGupbHM");
  public static final Header X_OKAPI_USER_ID = new Header(OKAPI_USERID_HEADER, "d1d0a10b-c563-4c4b-ae22-e5a0c11623eb");

  @Test
  void testPostEmptyResponseShouldCreateEntity(Vertx vertx, VertxTestContext testContext) {
    var hostCheckpoint = testContext.checkpoint();
    var clientCheckpoint = testContext.checkpoint();
    vertx.createHttpServer()
      .requestHandler(request -> testContext.verify(() -> {
        assertThat(request.method(), is(HttpMethod.POST));
        assertThat(request.path(), is("/finance/ledger-rollovers"));
        assertThat(request.getHeader(OKAPI_HEADER_TENANT), is("cat"));
        assertThat(request.getHeader(OKAPI_HEADER_TOKEN), is("manekineko"));
        request.response().setStatusCode(201);
        request.response().end();
        request.bodyHandler(body -> testContext.verify(() -> {
          assertThat(body.toJsonObject().getString("rolloverType"), is("Preview"));
          hostCheckpoint.flag();
        }));
      }))
      .listen(0)
      .compose(host -> {
        var rollover = new LedgerFiscalYearRollover().withRolloverType(RolloverType.PREVIEW);
        return new RestClient().postEmptyResponse("/finance/ledger-rollovers", rollover, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.succeeding(x -> clientCheckpoint.flag()));
  }

  @Test
  void testPostEmptyResponseShouldThrowException(Vertx vertx, VertxTestContext testContext) {
    vertx.createHttpServer()
      .requestHandler(request -> request.response().setStatusCode(500).end())
      .listen(0)
      .compose(host -> {
        var rollover = new LedgerFiscalYearRollover().withRolloverType(RolloverType.PREVIEW);
        return new RestClient().postEmptyResponse("/finance/ledger-rollovers", rollover, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.failingThenComplete());
  }

  @Test
  void testPostShouldReturnEntity(Vertx vertx, VertxTestContext testContext) {
    vertx.createHttpServer()
      .requestHandler(request -> testContext.verify(() -> {
        assertThat(request.method(), is(HttpMethod.POST));
        assertThat(request.path(), is("/finance/ledger-rollovers"));
        assertThat(request.getHeader(OKAPI_HEADER_TENANT), is("cat"));
        assertThat(request.getHeader(OKAPI_HEADER_TOKEN), is("manekineko"));
        request.response().setStatusCode(201);
        request.bodyHandler(body -> {
          assertThat(body.toJsonObject().getString("rolloverType"), is("Preview"));
          request.response().end(body.toString());
        });
      }))
      .listen(0)
      .compose(host -> {
        var rollover = new LedgerFiscalYearRollover().withRolloverType(RolloverType.PREVIEW);
        return new RestClient().post("/finance/ledger-rollovers", rollover, LedgerFiscalYearRollover.class, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.succeeding(result -> {
        assertThat(result.getRolloverType(), is(RolloverType.PREVIEW));
        testContext.completeNow();
      }));
  }

  @Test
  void testPostShouldThrowException(Vertx vertx, VertxTestContext testContext) {
    vertx.createHttpServer()
      .requestHandler(request -> request.response().setStatusCode(500).end())
      .listen(0)
      .compose(host -> {
        var rollover = new LedgerFiscalYearRollover().withRolloverType(RolloverType.PREVIEW);
        return new RestClient().post("/finance/ledger-rollovers", rollover, LedgerFiscalYearRollover.class, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.failingThenComplete());
  }

  @Test
  void testGetShouldReturnEntity(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> testContext.verify(() -> {
        assertThat(request.method(), is(HttpMethod.GET));
        assertThat(request.path(), is("/finance/ledger-rollovers/" + rolloverId));
        assertThat(request.getHeader(OKAPI_HEADER_TENANT), is("bee"));
        assertThat(request.getHeader(OKAPI_HEADER_TOKEN), is("janetoken"));
        request.response().end("{ \"id\":\"" + rolloverId + "\", \"rolloverType\":\"Preview\" }");
      }))
      .listen(0)
      .compose(host -> new RestClient().get("/finance/ledger-rollovers/" + rolloverId, LedgerFiscalYearRollover.class, requestContext(vertx, host, "bee", "janetoken")))
      .onComplete(testContext.succeeding(rollover -> {
        assertThat(rollover.getId(), is(rolloverId));
        assertThat(rollover.getRolloverType(), is(RolloverType.PREVIEW));
        testContext.completeNow();
      }));
  }

  @Test
  void testGetShouldThrowException(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> request.response().setStatusCode(404).end())
      .listen(0)
      .compose(host -> new RestClient().get("/finance/ledger-rollovers/" + rolloverId, LedgerFiscalYearRollover.class, requestContext(vertx, host, "bee", "janetoken")))
      .onComplete(testContext.failingThenComplete());
  }

  @Test
  void testGetWithSkipError404ShouldNotThrowException(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> request.response().setStatusCode(404).end())
      .listen(0)
      .compose(host -> new RestClient().get("/finance/ledger-rollovers/" + rolloverId, true, LedgerFiscalYearRollover.class, requestContext(vertx, host, "bee", "janetoken")))
      .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void testPutShouldUpdateEntity(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> testContext.verify(() -> {
        assertThat(request.method(), is(HttpMethod.PUT));
        assertThat(request.path(), is("/finance/ledger-rollovers/" + rolloverId));
        assertThat(request.getHeader(OKAPI_HEADER_TENANT), is("cat"));
        assertThat(request.getHeader(OKAPI_HEADER_TOKEN), is("manekineko"));
        request.response().setStatusCode(204);
        request.bodyHandler(body -> {
          assertThat(body.toJsonObject().getString("rolloverType"), is("Commit"));
          request.response().end();
        });
      }))
      .listen(0)
      .compose(host -> {
        var rollover = new LedgerFiscalYearRollover().withId(rolloverId).withRolloverType(RolloverType.COMMIT);
        return new RestClient().put("/finance/ledger-rollovers/" + rolloverId, rollover, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void testPutShouldThrowException(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> request.response().setStatusCode(400).end())
      .listen(0)
      .compose(host -> {
        var rollover = new LedgerFiscalYearRollover().withId(rolloverId).withRolloverType(RolloverType.COMMIT);
        return new RestClient().put("/finance/ledger-rollovers/" + rolloverId, rollover, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.failingThenComplete());
  }

  @Test
  void testPutShouldReturnEntity(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> testContext.verify(() -> {
        assertThat(request.method(), is(HttpMethod.PUT));
        assertThat(request.path(), is("/finance/ledger-rollovers/" + rolloverId));
        assertThat(request.getHeader(OKAPI_HEADER_TENANT), is("cat"));
        assertThat(request.getHeader(OKAPI_HEADER_TOKEN), is("manekineko"));
        request.response().setStatusCode(200);
        request.bodyHandler(body -> {
          assertThat(body.toJsonObject().getString("rolloverType"), is("Commit"));
          request.response().end(body.toString());
        });
      }))
      .listen(0)
      .compose(host -> {
        var rollover = new LedgerFiscalYearRollover().withId(rolloverId).withRolloverType(RolloverType.COMMIT);
        return new RestClient().put("/finance/ledger-rollovers/" + rolloverId, rollover, LedgerFiscalYearRollover.class, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.succeeding(result -> {
        assertThat(result.getId(), is(rolloverId));
        assertThat(result.getRolloverType(), is(RolloverType.COMMIT));
        testContext.completeNow();
      }));
  }

  @Test
  void testDeleteShouldRemoveEntity(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> testContext.verify(() -> {
        assertThat(request.method(), is(HttpMethod.DELETE));
        assertThat(request.path(), is("/finance/ledger-rollovers/" + rolloverId));
        assertThat(request.getHeader(OKAPI_HEADER_TENANT), is("cat"));
        assertThat(request.getHeader(OKAPI_HEADER_TOKEN), is("manekineko"));
        request.response().setStatusCode(204);
        request.response().end();
      }))
      .listen(0)
      .compose(host -> new RestClient().delete("/finance/ledger-rollovers/" + rolloverId, requestContext(vertx, host, "cat", "manekineko")))
      .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void testDeleteShouldThrowException(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> request.response().setStatusCode(500).end())
      .listen(0)
      .compose(host -> new RestClient().delete("/finance/ledger-rollovers/" + rolloverId, requestContext(vertx, host, "cat", "manekineko")))
      .onComplete(testContext.failingThenComplete());
  }

  @Test
  void testDeleteWithSkipError404ShouldNotThrowException(Vertx vertx, VertxTestContext testContext) {
    var rolloverId = UUID.randomUUID().toString();
    vertx.createHttpServer()
      .requestHandler(request -> request.response().setStatusCode(404).end())
      .listen(0)
      .compose(host -> new RestClient().delete("/finance/ledger-rollovers/" + rolloverId, true, requestContext(vertx, host, "cat", "manekineko")))
      .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void testPostBatchShouldReturnCollection(Vertx vertx, VertxTestContext testContext) {
    var fundId1 = UUID.randomUUID().toString();
    var fundId2 = UUID.randomUUID().toString();
    var fiscalYearId = UUID.randomUUID().toString();

    vertx.createHttpServer()
      .requestHandler(request -> testContext.verify(() -> {
        assertThat(request.method(), is(HttpMethod.POST));
        assertThat(request.path(), is("/finance/group-fund-fiscal-years/batch"));
        assertThat(request.getHeader(OKAPI_HEADER_TENANT), is("cat"));
        assertThat(request.getHeader(OKAPI_HEADER_TOKEN), is("manekineko"));
        request.response().setStatusCode(200);
        request.bodyHandler(body -> {
          var jsonBody = body.toJsonObject();
          assertThat(jsonBody.getJsonArray("fundIds").size(), is(2));
          assertThat(jsonBody.getString("fiscalYearId"), is(fiscalYearId));
          request.response().end("{ \"groupFundFiscalYears\": [], \"totalRecords\": 0 }");
        });
      }))
      .listen(0)
      .compose(host -> {
        var batchRequest = new GroupFundFiscalYearBatchRequest()
          .withFundIds(List.of(fundId1, fundId2))
          .withFiscalYearId(fiscalYearId);
        return new RestClient().postBatch("/finance/group-fund-fiscal-years/batch", batchRequest,
          GroupFundFiscalYearCollection.class, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.succeeding(result -> {
        assertThat(result.getTotalRecords(), is(0));
        testContext.completeNow();
      }));
  }

  @Test
  void testPostBatchShouldThrowException(Vertx vertx, VertxTestContext testContext) {
    var fundId1 = UUID.randomUUID().toString();
    var fundId2 = UUID.randomUUID().toString();
    var fiscalYearId = UUID.randomUUID().toString();

    vertx.createHttpServer()
      .requestHandler(request -> request.response().setStatusCode(500).end())
      .listen(0)
      .compose(host -> {
        var batchRequest = new GroupFundFiscalYearBatchRequest()
          .withFundIds(List.of(fundId1, fundId2))
          .withFiscalYearId(fiscalYearId);
        return new RestClient().postBatch("/finance/group-fund-fiscal-years/batch", batchRequest,
          GroupFundFiscalYearCollection.class, requestContext(vertx, host, "cat", "manekineko"));
      }).onComplete(testContext.failingThenComplete());
  }

  private RequestContext requestContext(Vertx vertx, HttpServer httpServer, String tenant, String token) {
    var port = httpServer == null ? NetworkUtils.nextFreePort() : httpServer.actualPort();
    var headers = new CaseInsensitiveMap<>(Map.of(
      OKAPI_URL, "http://localhost:" + port,
      OKAPI_HEADER_TENANT, tenant,
      OKAPI_HEADER_TOKEN, token));
    return new RequestContext(vertx.getOrCreateContext(), headers);
  }
}
