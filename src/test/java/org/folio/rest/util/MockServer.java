package org.folio.rest.util;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.ApiTestBase.BAD_QUERY;
import static org.folio.rest.impl.ApiTestBase.ID_DOES_NOT_EXIST;
import static org.folio.rest.impl.ApiTestBase.ID_FOR_INTERNAL_SERVER_ERROR;
import static org.folio.rest.impl.ApiTestBase.getMockData;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.ResourcePathResolver.FUNDS;
import static org.folio.rest.util.ResourcePathResolver.FUND_TYPES;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.FundTypesCollection;
import org.folio.rest.jaxrs.model.FundsCollection;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import one.util.streamex.StreamEx;

public class MockServer {

  private static final Logger logger = LoggerFactory.getLogger(MockServer.class);

  private static final String QUERY = "query";
  private static final String ERROR_TENANT = "error_tenant";
  private static final String ID_PATH_PARAM = "/:" + ID;

  public static final Header ERROR_X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, ERROR_TENANT);

  private final int port;
  private final Vertx vertx;

  public static Table<String, HttpMethod, List<JsonObject>> serverRqRs = HashBasedTable.create();
  public static HashMap<String, List<String>> serverRqQueries = new HashMap<>();

  public MockServer(int port) {
    this.port = port;
    this.vertx = Vertx.vertx();
  }

  public void start() throws InterruptedException, ExecutionException, TimeoutException {
    // Setup Mock Server...
    HttpServer server = vertx.createHttpServer();
    CompletableFuture<HttpServer> deploymentComplete = new CompletableFuture<>();
    server.requestHandler(defineRoutes()::accept).listen(port, result -> {
      if(result.succeeded()) {
        deploymentComplete.complete(result.result());
      }
      else {
        deploymentComplete.completeExceptionally(result.cause());
      }
    });
    deploymentComplete.get(60, TimeUnit.SECONDS);
  }

  public void close() {
    vertx.close(res -> {
      if (res.failed()) {
        logger.error("Failed to shut down mock server", res.cause());
        fail(res.cause().getMessage());
      } else {
        logger.info("Successfully shut down mock server");
      }
    });
  }

  public static List<JsonObject> getRqRsEntries(HttpMethod method, String objName) {
    List<JsonObject> entries = serverRqRs.get(objName, method);
    if (entries == null) {
      entries = new ArrayList<>();
    }
    return entries;
  }

  public static List<JsonObject> getCollectionRecords(String entryName) {
    return getCollectionRecords(getRqRsEntries(HttpMethod.GET, entryName));
  }

  public static List<JsonObject> getRecordsByIds(String entryName) {
    return getRecordsByIds(getRqRsEntries(HttpMethod.GET, entryName));
  }

  private static List<JsonObject> getCollectionRecords(List<JsonObject> entries) {
    return entries.stream().filter(json -> !json.containsKey(ID)).collect(toList());
  }

  private static List<JsonObject> getRecordsByIds(List<JsonObject> entries) {
    return entries.stream().filter(json -> json.containsKey(ID)).collect(toList());
  }

  private Router defineRoutes() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.route(HttpMethod.POST, resourcesPath(FUNDS))
      .handler(ctx -> handlePostEntry(ctx, Fund.class, TestEntities.FUND.name()));
    router.route(HttpMethod.POST, resourcesPath(FUND_TYPES))
      .handler(ctx -> handlePostEntry(ctx, FundType.class, TestEntities.FUND_TYPE.name()));

    router.route(HttpMethod.GET, resourcesPath(FUNDS))
      .handler(this::handleGetFunds);
    router.route(HttpMethod.GET, resourcesPath(FUND_TYPES))
      .handler(this::handleGetFundTypes);
    router.route(HttpMethod.GET, resourceByIdPath(FUNDS))
      .handler(this::handleGetFundById);
    router.route(HttpMethod.GET, resourceByIdPath(FUND_TYPES))
      .handler(this::handleGetFundTypeById);

    router.route(HttpMethod.DELETE, resourceByIdPath(FUNDS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.FUND.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(FUND_TYPES))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.FUND_TYPE.name()));

    router.route(HttpMethod.PUT, resourceByIdPath(FUNDS))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.FUND.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(FUND_TYPES))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.FUND_TYPE.name()));

    return router;
  }

  private void handleGetFunds(RoutingContext ctx) {
    logger.info("handleGetFundRecords got: " + ctx.request().path());

    String query = StringUtils.trimToEmpty(ctx.request().getParam(QUERY));

    if (query.contains(ID_FOR_INTERNAL_SERVER_ERROR)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else if (query.contains(BAD_QUERY)) {
      serverResponse(ctx, 400, APPLICATION_JSON, BAD_REQUEST.getReasonPhrase());
    } else {
      try {

        List<String> fundIds = Collections.emptyList();
        if (query.startsWith("id==")) {
          fundIds = extractIdsFromQuery(query);
        }

        List<Fund> funds = getFundsByIds(fundIds);

        FundsCollection fundCollection = new FundsCollection().withFunds(funds).withTotalRecords(funds.size());

        JsonObject fundsJson = JsonObject.mapFrom(fundCollection);
        addServerRqRsData(HttpMethod.GET, TestEntities.FUND.name(), fundsJson);

        ctx.response()
          .setStatusCode(200)
          .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
          .end(fundsJson.encodePrettily());
      } catch (Exception e) {
        serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
      }
    }
  }

  private List<Fund> getFundsByIds(List<String> fundIds) {
    Supplier<List<Fund>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.FUND.getPathToFileWithData())).mapTo(FundsCollection.class).getFunds();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<Fund> funds = getMockEntries(TestEntities.FUND.name(), Fund.class).orElseGet(getFromFile);

    if (!fundIds.isEmpty()) {
      funds.removeIf(item -> !fundIds.contains(item.getId()));
    }

    return funds;
  }

  private void handleGetFundTypes(RoutingContext ctx) {
    logger.info("handleGetFundTypes got: " + ctx.request().path());

    String query = StringUtils.trimToEmpty(ctx.request().getParam(QUERY));

    if (query.contains(ID_FOR_INTERNAL_SERVER_ERROR)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else if (query.contains(BAD_QUERY)) {
      serverResponse(ctx, 400, APPLICATION_JSON, BAD_REQUEST.getReasonPhrase());
    } else {
      try {
        List<String> typeIds = Collections.emptyList();
        if (query.startsWith("id==")) {
          typeIds = extractIdsFromQuery(query);
        }
        List<FundType> types = getFundTypesByIds(typeIds);

        FundTypesCollection collection = new FundTypesCollection().withFundTypes(types).withTotalRecords(types.size());

        JsonObject typesJson = JsonObject.mapFrom(collection);
        addServerRqRsData(HttpMethod.GET, TestEntities.FUND.name(), typesJson);

        ctx.response()
          .setStatusCode(200)
          .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
          .end(typesJson.encodePrettily());
      } catch (Exception e) {
        serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
      }
    }
  }

  private List<FundType> getFundTypesByIds(List<String> ids) {
    Supplier<List<FundType>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.FUND_TYPE.getPathToFileWithData())).mapTo(FundTypesCollection.class)
          .getFundTypes();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<FundType> types = getMockEntries(TestEntities.FUND_TYPE.name(), FundType.class).orElseGet(getFromFile);

    if (!ids.isEmpty()) {
      types.removeIf(item -> !ids.contains(item.getId()));
    }

    return types;
  }

  private void handleGetFundById(RoutingContext ctx) {
    logger.info("handleGetFundById got: {}", ctx.request().path());
    String id = ctx.request().getParam(ID);

    // Register request
    addServerRqRsData(HttpMethod.GET, TestEntities.FUND.name(), new JsonObject().put(ID, id));

    if (id.equals(ID_FOR_INTERNAL_SERVER_ERROR)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else {
      try {
        List<Fund> funds = getFundsByIds(Collections.singletonList(id));
        if (funds.isEmpty()) {
          serverResponse(ctx, 404, APPLICATION_JSON, id);
        } else {
          serverResponse(ctx, 200, APPLICATION_JSON, JsonObject.mapFrom(funds.get(0)).encodePrettily());
        }
      } catch (Exception e) {
        serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
      }
    }
  }

  private void handleGetFundTypeById(RoutingContext ctx) {
    logger.info("handleGetFundTypeById got: {}", ctx.request().path());
    String id = ctx.request().getParam(ID);

    // Register request
    addServerRqRsData(HttpMethod.GET, TestEntities.FUND_TYPE.name(), new JsonObject().put(ID, id));

    if (id.equals(ID_FOR_INTERNAL_SERVER_ERROR)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else {
      try {
        List<FundType> types = getFundTypesByIds(Collections.singletonList(id));
        if (types.isEmpty()) {
          serverResponse(ctx, 404, APPLICATION_JSON, id);
        } else {
          serverResponse(ctx, 200, APPLICATION_JSON, JsonObject.mapFrom(types.get(0)).encodePrettily());
        }
      } catch (Exception e) {
        serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
      }
    }
  }

  private String resourceByIdPath(String field) {
    return resourcesPath(field) + ID_PATH_PARAM;
  }

  private <T> void handlePostEntry(RoutingContext ctx, Class<T> tClass, String entryName) {
    logger.info("got: " + ctx.getBodyAsString());

    String tenant = ctx.request().getHeader(OKAPI_HEADER_TENANT);
    if (ERROR_TENANT.equals(tenant)) {
      serverResponse(ctx, 500, TEXT_PLAIN, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else {
      JsonObject body = ctx.getBodyAsJson();
      if (body.getString(ID) == null) {
        body.put(ID, UUID.randomUUID().toString());
      }
      T entry = body.mapTo(tClass);
      addServerRqRsData(HttpMethod.POST, entryName, body);

      serverResponse(ctx, 201, APPLICATION_JSON, JsonObject.mapFrom(entry).encodePrettily());
    }
  }

  private void handleDeleteRequest(RoutingContext ctx, String type) {
    logger.info("handleDeleteRequest got: DELETE {}", ctx.request().path());
    String id = ctx.request().getParam(ID);

    // Register request
    addServerRqRsData(HttpMethod.DELETE, type, new JsonObject().put(ID, id));

    if (ID_DOES_NOT_EXIST.equals(id)) {
      serverResponse(ctx, 404, TEXT_PLAIN, id);
    } else if (ID_FOR_INTERNAL_SERVER_ERROR.equals(id)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else {
      ctx.response()
        .setStatusCode(204)
        .end();
    }
  }

  private void handlePutGenericSubObj(RoutingContext ctx, String subObj) {
    logger.info("handlePutGenericSubObj got: PUT {} for {}", ctx.request().path());
    String id = ctx.request().getParam(ID);

    addServerRqRsData(HttpMethod.PUT, subObj, ctx.getBodyAsJson());

    if (ID_DOES_NOT_EXIST.equals(id)) {
      serverResponse(ctx, 404, APPLICATION_JSON, id);
    } else if (ID_FOR_INTERNAL_SERVER_ERROR.equals(id)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else {
      ctx.response()
        .setStatusCode(204)
        .end();
    }
  }

  private void serverResponse(RoutingContext ctx, int statusCode, String contentType, String body) {
    ctx.response()
      .setStatusCode(statusCode)
      .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
      .end(body);
  }

  private <T> Optional<List<T>> getMockEntries(String objName, Class<T> tClass) {
    List<T> entryList =  getRqRsEntries(HttpMethod.OTHER, objName).stream()
      .map(entries -> entries.mapTo(tClass))
      .collect(toList());
    return Optional.ofNullable(entryList.isEmpty()? null: entryList);
  }

  private static void addServerRqRsData(HttpMethod method, String objName, JsonObject data) {
    List<JsonObject> entries = getRqRsEntries(method, objName);
    entries.add(data);
    serverRqRs.put(objName, method, entries);
  }

  private void addServerRqQuery(String objName, String query) {
    serverRqQueries.computeIfAbsent(objName, key -> new ArrayList<>())
      .add(query);
  }

  static List<String> getQueryParams(String resourceType) {
    return serverRqQueries.getOrDefault(resourceType, Collections.emptyList());
  }

  private List<String> extractIdsFromQuery(String query) {
    return extractIdsFromQuery(query, "==");
  }

  private List<String> extractIdsFromQuery(String query, String relation) {
    return extractIdsFromQuery(ID, relation, query);
  }

  private List<String> extractIdsFromQuery(String fieldName, String relation, String query) {
    Matcher matcher = Pattern.compile(".*" + fieldName + relation + "\\(?(.+)\\).*").matcher(query);
    if (matcher.find()) {
      return StreamEx.split(matcher.group(1), " or ").toList();
    } else {
      return Collections.emptyList();
    }
  }
}
