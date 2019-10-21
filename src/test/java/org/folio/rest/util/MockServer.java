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
import static org.folio.rest.util.ResourcePathResolver.BUDGETS;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.FUNDS;
import static org.folio.rest.util.ResourcePathResolver.FUND_TYPES;
import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.LEDGERS;
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
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
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
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;

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

  public static List<JsonObject> getRecordById(String entryName) {
    return getRecordById(getRqRsEntries(HttpMethod.GET, entryName));
  }

  private static List<JsonObject> getCollectionRecords(List<JsonObject> entries) {
    return entries.stream().filter(json -> !json.containsKey(ID)).collect(toList());
  }

  private static List<JsonObject> getRecordById(List<JsonObject> entries) {
    return entries.stream().filter(json -> json.containsKey(ID)).collect(toList());
  }

  private Router defineRoutes() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.route(HttpMethod.POST, resourcesPath(BUDGETS))
      .handler(ctx -> handlePostEntry(ctx, Budget.class, TestEntities.BUDGET.name()));
    router.route(HttpMethod.POST, resourcesPath(FUNDS))
      .handler(ctx -> handlePostEntry(ctx, Fund.class, TestEntities.FUND.name()));
    router.route(HttpMethod.POST, resourcesPath(FISCAL_YEARS))
      .handler(ctx -> handlePostEntry(ctx, FiscalYear.class, TestEntities.FISCAL_YEAR.name()));
    router.route(HttpMethod.POST, resourcesPath(FUND_TYPES))
      .handler(ctx -> handlePostEntry(ctx, FundType.class, TestEntities.FUND_TYPE.name()));
    router.route(HttpMethod.POST, resourcesPath(GROUP_FUND_FISCAL_YEARS))
      .handler(ctx -> handlePostEntry(ctx, GroupFundFiscalYear.class, TestEntities.GROUP_FUND_FISCAL_YEAR.name()));
    router.route(HttpMethod.POST, resourcesPath(LEDGERS))
      .handler(ctx -> handlePostEntry(ctx, Ledger.class, TestEntities.LEDGER.name()));

    router.route(HttpMethod.GET, resourcesPath(BUDGETS))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.BUDGET));
    router.route(HttpMethod.GET, resourcesPath(FUNDS))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.FUND));
    router.route(HttpMethod.GET, resourcesPath(FISCAL_YEARS))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.FISCAL_YEAR));
    router.route(HttpMethod.GET, resourcesPath(FUND_TYPES))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.FUND_TYPE));
    router.route(HttpMethod.GET, resourcesPath(GROUP_FUND_FISCAL_YEARS))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.GROUP_FUND_FISCAL_YEAR));
    router.route(HttpMethod.GET, resourcesPath(LEDGERS))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.LEDGER));

    router.route(HttpMethod.GET, resourceByIdPath(BUDGETS))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.BUDGET));
    router.route(HttpMethod.GET, resourceByIdPath(FUNDS))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.FUND));
    router.route(HttpMethod.GET, resourceByIdPath(FISCAL_YEARS))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.FISCAL_YEAR));
    router.route(HttpMethod.GET, resourceByIdPath(FUND_TYPES))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.FUND_TYPE));
    router.route(HttpMethod.GET, resourceByIdPath(LEDGERS))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.LEDGER));

    router.route(HttpMethod.DELETE, resourceByIdPath(BUDGETS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.BUDGET.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(FUNDS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.FUND.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(FISCAL_YEARS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.FISCAL_YEAR.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(FUND_TYPES))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.FUND_TYPE.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(GROUP_FUND_FISCAL_YEARS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.GROUP_FUND_FISCAL_YEAR.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(LEDGERS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.LEDGER.name()));

    router.route(HttpMethod.PUT, resourceByIdPath(BUDGETS))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.BUDGET.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(FUNDS))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.FUND.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(FISCAL_YEARS))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.FISCAL_YEAR.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(FUND_TYPES))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.FUND_TYPE.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(LEDGERS))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.LEDGER.name()));

    return router;
  }

  private void handleGetCollection(RoutingContext ctx, TestEntities testEntity) {
    logger.info("handleGetCollection got: " + ctx.request().path());

    String query = StringUtils.trimToEmpty(ctx.request().getParam(QUERY));

    if (query.contains(ID_FOR_INTERNAL_SERVER_ERROR)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else if (query.contains(BAD_QUERY)) {
      serverResponse(ctx, 400, APPLICATION_JSON, BAD_REQUEST.getReasonPhrase());
    } else {
      try {

        List<String> ids = Collections.emptyList();
        if (query.startsWith("id==")) {
          ids = extractIdsFromQuery(query);
        }

        JsonObject collection = getCollectionOfRecords(testEntity, ids);
        addServerRqRsData(HttpMethod.GET, testEntity.name(), collection);

        ctx.response()
          .setStatusCode(200)
          .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
          .end(collection.encodePrettily());
      } catch (Exception e) {
        serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
      }
    }
  }

  private void handleGetRecordById(RoutingContext ctx, TestEntities testEntity) {
    logger.info("handleGetRecordById got: {}", ctx.request().path());
    String id = ctx.request().getParam(ID);

    // Register request
    addServerRqRsData(HttpMethod.GET, testEntity.name(), new JsonObject().put(ID, id));

    if (id.equals(ID_FOR_INTERNAL_SERVER_ERROR)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else {
      try {
        JsonObject record = getMockRecord(testEntity, id);
        if (record == null) {
          serverResponse(ctx, 404, APPLICATION_JSON, id);
        } else {
          serverResponse(ctx, 200, APPLICATION_JSON, record.encodePrettily());
        }
      } catch (Exception e) {
        serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
      }
    }
  }

  private JsonObject getBudgetsByIds(List<String> ids, boolean isCollection) {
    Supplier<List<Budget>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.BUDGET.getPathToFileWithData())).mapTo(BudgetsCollection.class).getBudgets();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<Budget> records = getMockEntries(TestEntities.BUDGET.name(), Budget.class).orElseGet(getFromFile);

    if (!ids.isEmpty()) {
      records.removeIf(item -> !ids.contains(item.getId()));
    }

    Object record;
    if (isCollection) {
      record = new BudgetsCollection().withBudgets(records).withTotalRecords(records.size());
    } else if (!records.isEmpty()) {
      record = records.get(0);
    } else {
      return null;
    }

    return JsonObject.mapFrom(record);
  }

  private JsonObject getFundsByIds(List<String> fundIds, boolean isCollection) {
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

    Object record;
    if (isCollection) {
      record = new FundsCollection().withFunds(funds).withTotalRecords(funds.size());
    } else if (!funds.isEmpty()) {
      record = funds.get(0);
    } else {
      return null;
    }

    return JsonObject.mapFrom(record);
  }

  private JsonObject getFiscalYearsByIds(List<String> fiscalYearIds, boolean isCollection) {
    Supplier<List<FiscalYear>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.FISCAL_YEAR.getPathToFileWithData())).mapTo(FiscalYearsCollection.class).getFiscalYears();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<FiscalYear> fiscalYears = getMockEntries(TestEntities.FISCAL_YEAR.name(), FiscalYear.class).orElseGet(getFromFile);

    if (!fiscalYearIds.isEmpty()) {
      fiscalYears.removeIf(item -> !fiscalYearIds.contains(item.getId()));
    }

    Object record;
    if (isCollection) {
      record = new FiscalYearsCollection().withFiscalYears(fiscalYears).withTotalRecords(fiscalYears.size());
    } else if (!fiscalYears.isEmpty()) {
      record = fiscalYears.get(0);
    } else {
      return null;
    }

    return JsonObject.mapFrom(record);
  }

  private JsonObject getFundTypesByIds(List<String> ids, boolean isCollection) {
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

    Object record;
    if (isCollection) {
      record = new FundTypesCollection().withFundTypes(types).withTotalRecords(types.size());
    } else if (!types.isEmpty()) {
      record = types.get(0);
    } else {
      return null;
    }

    return JsonObject.mapFrom(record);
  }

  private JsonObject getLedgersByIds(List<String> ids, boolean isCollection) {
    Supplier<List<Ledger>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.LEDGER.getPathToFileWithData())).mapTo(LedgersCollection.class)
          .getLedgers();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<Ledger> ledgers = getMockEntries(TestEntities.LEDGER.name(), Ledger.class).orElseGet(getFromFile);

    if (!ids.isEmpty()) {
      ledgers.removeIf(item -> !ids.contains(item.getId()));
    }

    Object record;
    if (isCollection) {
      record = new LedgersCollection().withLedgers(ledgers).withTotalRecords(ledgers.size());
    } else if (!ledgers.isEmpty()) {
      record = ledgers.get(0);
    } else {
      return null;
    }

    return JsonObject.mapFrom(record);
  }

  private JsonObject getGroupFundFiscalYearsByIds(List<String> ids, boolean isCollection) {
    Supplier<List<GroupFundFiscalYear>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.GROUP_FUND_FISCAL_YEAR.getPathToFileWithData())).mapTo(GroupFundFiscalYearCollection.class)
          .getGroupFundFiscalYears();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<GroupFundFiscalYear> groupFundFiscalYears = getMockEntries(TestEntities.GROUP_FUND_FISCAL_YEAR.name(), GroupFundFiscalYear.class).orElseGet(getFromFile);

    if (!ids.isEmpty()) {
      groupFundFiscalYears.removeIf(item -> !ids.contains(item.getId()));
    }

    Object record;
    if (isCollection) {
      record = new GroupFundFiscalYearCollection().withGroupFundFiscalYears(groupFundFiscalYears).withTotalRecords(groupFundFiscalYears.size());
    } else if (!groupFundFiscalYears.isEmpty()) {
      record = groupFundFiscalYears.get(0);
    } else {
      return null;
    }

    return JsonObject.mapFrom(record);
  }

  private JsonObject getCollectionOfRecords(TestEntities testEntity, List<String> ids) {
    return getEntries(testEntity, ids, true);
  }

  private JsonObject getMockRecord(TestEntities testEntity, String id) {
    return getEntries(testEntity, Collections.singletonList(id), false);
  }

  private JsonObject getEntries(TestEntities testEntity, List<String> ids, boolean isCollection) {
    switch (testEntity) {
    case BUDGET:
      return getBudgetsByIds(ids, isCollection);
    case FUND:
      return getFundsByIds(ids, isCollection);
    case FISCAL_YEAR:
      return getFiscalYearsByIds(ids, isCollection);
    case FUND_TYPE:
      return getFundTypesByIds(ids, isCollection);
    case GROUP_FUND_FISCAL_YEAR:
      return getGroupFundFiscalYearsByIds(ids, isCollection);
    case LEDGER:
      return getLedgersByIds(ids, isCollection);
    default:
      throw new IllegalArgumentException(testEntity.name() + " entity is unknown");
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
