package org.folio.rest.util;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.FundsApiTest.X_CONFIG_HEADER_NAME;
import static org.folio.rest.impl.GroupFiscalYearSummariesTest.FUND_ID_FIRST_DIFFERENT_GROUP;
import static org.folio.rest.impl.GroupFiscalYearSummariesTest.FUND_ID_FIRST_SAME_GROUP;
import static org.folio.rest.impl.GroupFiscalYearSummariesTest.FUND_ID_SECOND_DIFFERENT_GROUP;
import static org.folio.rest.impl.GroupFiscalYearSummariesTest.FUND_ID_SECOND_SAME_GROUP;
import static org.folio.rest.impl.GroupFiscalYearSummariesTest.TO_ALLOCATION_FIRST_DIF_GROUP;
import static org.folio.rest.impl.GroupFiscalYearSummariesTest.TO_ALLOCATION_SECOND_DIF_GROUP;
import static org.folio.rest.impl.TransactionApiTest.DELETE_TRANSACTION_ID;
import static org.folio.rest.impl.TransactionApiTest.DELETE_CONNECTED_TRANSACTION_ID;
import static org.folio.rest.util.ResourcePathResolver.INVOICE_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.TestConstants.BAD_QUERY;
import static org.folio.rest.util.TestConstants.BASE_MOCK_DATA_PATH;
import static org.folio.rest.util.TestConstants.EMPTY_CONFIG_X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.ERROR_TENANT;
import static org.folio.rest.util.TestConstants.ID_DOES_NOT_EXIST;
import static org.folio.rest.util.TestConstants.ID_FOR_INTERNAL_SERVER_ERROR;
import static org.folio.rest.util.TestConstants.INVALID_CONFIG_X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.TOTAL_RECORDS;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestUtils.getMockData;
import static org.folio.rest.impl.BudgetsApiTest.BUDGET_WITH_BOUNDED_TRANSACTION_ID;
import static org.folio.rest.util.ErrorCodes.TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR;
import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.CONFIGURATIONS;
import static org.folio.rest.util.ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL;
import static org.folio.rest.util.ResourcePathResolver.FISCAL_YEARS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.FUNDS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.FUND_TYPES;
import static org.folio.rest.util.ResourcePathResolver.GROUPS;
import static org.folio.rest.util.ResourcePathResolver.GROUP_FUND_FISCAL_YEARS;
import static org.folio.rest.util.ResourcePathResolver.LEDGERS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.ORDER_TRANSACTION_SUMMARIES;
import static org.folio.rest.util.ResourcePathResolver.TRANSACTIONS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.services.configuration.ConfigurationEntriesService.SYSTEM_CONFIG_MODULE_NAME;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.ExpenseClass;
import org.folio.rest.jaxrs.model.ExpenseClassCollection;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.model.FundTypesCollection;
import org.folio.rest.jaxrs.model.FundsCollection;
import org.folio.rest.jaxrs.model.Group;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.folio.rest.jaxrs.model.GroupsCollection;
import org.folio.rest.jaxrs.model.InvoiceTransactionSummary;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.jaxrs.model.OrderTransactionSummary;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import one.util.streamex.StreamEx;

public class MockServer {

  private static final Logger logger = LogManager.getLogger(MockServer.class);
  private static final String QUERY = "query";
  private static final String ID_PATH_PARAM = "/:" + ID;
  static final String CONFIG_MOCK_PATH = BASE_MOCK_DATA_PATH + "configurationEntries/%s.json";
  private static final String LEDGER_FYS_MOCK_PATH = BASE_MOCK_DATA_PATH + "ledger-fiscal-year/ledger-fiscal-years.json";
  public static final String TRANSACTION_ALLOCATION_TO_QUERY = ".+transactionType==Allocation.+toFundId==.+(cql.allRecords=1 NOT fromFundId=\"\").+";
  public static final String TRANSACTION_ALLOCATION_FROM_QUERY = ".+transactionType==Allocation.+fromFundId==.+cql.allRecords=1 NOT toFundId=\"\".+";

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
    server.requestHandler(defineRoutes()).listen(port, result -> {
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

    router.route(HttpMethod.POST, resourcesPath(BUDGETS_STORAGE))
      .handler(ctx -> handlePostEntry(ctx, Budget.class, TestEntities.BUDGET.name()));
    router.route(HttpMethod.POST, resourcesPath(FUNDS_STORAGE))
      .handler(ctx -> handlePostEntry(ctx, Fund.class, TestEntities.FUND.name()));
    router.route(HttpMethod.POST, resourcesPath(FISCAL_YEARS_STORAGE))
      .handler(ctx -> handlePostEntry(ctx, FiscalYear.class, TestEntities.FISCAL_YEAR.name()));
    router.route(HttpMethod.POST, resourcesPath(FUND_TYPES))
      .handler(ctx -> handlePostEntry(ctx, FundType.class, TestEntities.FUND_TYPE.name()));
    router.route(HttpMethod.POST, resourcesPath(GROUP_FUND_FISCAL_YEARS))
      .handler(ctx -> handlePostEntry(ctx, GroupFundFiscalYear.class, TestEntities.GROUP_FUND_FISCAL_YEAR.name()));
    router.route(HttpMethod.POST, resourcesPath(LEDGERS_STORAGE))
      .handler(ctx -> handlePostEntry(ctx, Ledger.class, TestEntities.LEDGER.name()));
    router.route(HttpMethod.POST, resourcesPath(GROUPS))
      .handler(ctx -> handlePostEntry(ctx, Group.class, TestEntities.GROUP.name()));
    router.route(HttpMethod.POST, resourcesPath(TRANSACTIONS))
      .handler(ctx -> handleTransactionPostEntry(ctx, Transaction.class));
    router.route(HttpMethod.POST, resourcesPath(ResourcePathResolver.ORDER_TRANSACTION_SUMMARIES))
    .handler(ctx -> handlePostEntry(ctx, OrderTransactionSummary.class, TestEntities.ORDER_TRANSACTION_SUMMARY.name()));
    router.route(HttpMethod.POST, resourcesPath(ResourcePathResolver.INVOICE_TRANSACTION_SUMMARIES))
    .handler(ctx -> handlePostEntry(ctx, InvoiceTransactionSummary.class, TestEntities.INVOICE_TRANSACTION_SUMMARY.name()));
    router.route(HttpMethod.POST, resourcesPath(ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL))
      .handler(ctx -> handlePostEntry(ctx, ExpenseClass.class, TestEntities.EXPENSE_CLASSES.name()));

    router.route(HttpMethod.GET, resourcesPath(BUDGETS_STORAGE))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.BUDGET));
    router.route(HttpMethod.GET, resourcesPath(FUNDS_STORAGE))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.FUND));
    router.route(HttpMethod.GET, resourcesPath(FISCAL_YEARS_STORAGE))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.FISCAL_YEAR));
    router.route(HttpMethod.GET, resourcesPath(FUND_TYPES))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.FUND_TYPE));
    router.route(HttpMethod.GET, resourcesPath(GROUP_FUND_FISCAL_YEARS))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.GROUP_FUND_FISCAL_YEAR));
    router.route(HttpMethod.GET, resourcesPath(LEDGERS_STORAGE))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.LEDGER));
    router.route(HttpMethod.GET, resourcesPath(GROUPS))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.GROUP));
    router.route(HttpMethod.GET, resourcesPath(TRANSACTIONS))
      .handler(ctx -> handleGetTransactionsCollection(ctx, TestEntities.TRANSACTIONS));
    router.route(HttpMethod.GET, resourcesPath(CONFIGURATIONS))
      .handler(this::handleConfigurationModuleResponse);
    router.route(HttpMethod.GET, resourcesPath(ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL))
      .handler(ctx -> handleGetCollection(ctx, TestEntities.EXPENSE_CLASSES));

    router.route(HttpMethod.GET, resourceByIdPath(BUDGETS_STORAGE))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.BUDGET));
    router.route(HttpMethod.GET, resourceByIdPath(FUNDS_STORAGE))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.FUND));
    router.route(HttpMethod.GET, resourceByIdPath(FISCAL_YEARS_STORAGE))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.FISCAL_YEAR));
    router.route(HttpMethod.GET, resourceByIdPath(FUND_TYPES))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.FUND_TYPE));
    router.route(HttpMethod.GET, resourceByIdPath(LEDGERS_STORAGE))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.LEDGER));
    router.route(HttpMethod.GET, resourceByIdPath(GROUPS))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.GROUP));
    router.route(HttpMethod.GET, resourceByIdPath(TRANSACTIONS))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.TRANSACTIONS));
    router.route(HttpMethod.GET, resourceByIdPath(ResourcePathResolver.EXPENSE_CLASSES_STORAGE_URL))
      .handler(ctx -> handleGetRecordById(ctx, TestEntities.EXPENSE_CLASSES));

    router.route(HttpMethod.DELETE, resourceByIdPath(BUDGETS_STORAGE))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.BUDGET.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(FUNDS_STORAGE))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.FUND.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(FISCAL_YEARS_STORAGE))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.FISCAL_YEAR.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(FUND_TYPES))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.FUND_TYPE.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(GROUP_FUND_FISCAL_YEARS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.GROUP_FUND_FISCAL_YEAR.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(LEDGERS_STORAGE))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.LEDGER.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(GROUPS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.GROUP.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(TRANSACTIONS))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.TRANSACTIONS.name()));
    router.route(HttpMethod.DELETE, resourceByIdPath(EXPENSE_CLASSES_STORAGE_URL))
      .handler(ctx -> handleDeleteRequest(ctx, TestEntities.EXPENSE_CLASSES.name()));

    router.route(HttpMethod.PUT, resourceByIdPath(BUDGETS_STORAGE))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.BUDGET.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(FUNDS_STORAGE))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.FUND.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(FISCAL_YEARS_STORAGE))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.FISCAL_YEAR.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(FUND_TYPES))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.FUND_TYPE.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(LEDGERS_STORAGE))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.LEDGER.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(GROUPS))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.GROUP.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(GROUP_FUND_FISCAL_YEARS))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.GROUP_FUND_FISCAL_YEAR.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(TRANSACTIONS))
      .handler(ctx -> handleTransactionPutEntry(ctx, Transaction.class));
    router.route(HttpMethod.PUT, resourceByIdPath(ORDER_TRANSACTION_SUMMARIES))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.ORDER_TRANSACTION_SUMMARY.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(INVOICE_TRANSACTION_SUMMARIES))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.INVOICE_TRANSACTION_SUMMARY.name()));
    router.route(HttpMethod.PUT, resourceByIdPath(EXPENSE_CLASSES_STORAGE_URL))
      .handler(ctx -> handlePutGenericSubObj(ctx, TestEntities.EXPENSE_CLASSES.name()));
    return router;
  }

  private void handleGetTransactionsCollection(RoutingContext ctx, TestEntities testEntity) {
    logger.info("handleGetTransactionsCollection got: " + ctx.request().path());

    String query = StringUtils.trimToEmpty(ctx.request().getParam(QUERY));
    addServerRqQuery(testEntity.name(), query);
    if (query.contains(ID_FOR_INTERNAL_SERVER_ERROR)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else if (query.contains(BAD_QUERY)) {
      serverResponse(ctx, 400, APPLICATION_JSON, BAD_REQUEST.getReasonPhrase());
    } else if (query.contains(ID_DOES_NOT_EXIST)) {
      JsonObject emptyCollection = new JsonObject().put(testEntity.getName(), new JsonArray());
      emptyCollection.put(TOTAL_RECORDS, 0);
      serverResponse(ctx,200, APPLICATION_JSON, emptyCollection.encodePrettily());
    } else if (query.contains(FUND_ID_FIRST_SAME_GROUP) || query.contains(FUND_ID_SECOND_SAME_GROUP)
                  || (query.contains(FUND_ID_SECOND_DIFFERENT_GROUP) && (query.matches(TRANSACTION_ALLOCATION_FROM_QUERY)))
                  || (query.contains(FUND_ID_FIRST_DIFFERENT_GROUP) && (query.matches(TRANSACTION_ALLOCATION_TO_QUERY)))
                  || query.contains(DELETE_TRANSACTION_ID)) {
      JsonObject emptyCollection = new JsonObject().put(testEntity.getName(), new JsonArray());
      emptyCollection.put(TOTAL_RECORDS, 0);
      serverResponse(ctx,200, APPLICATION_JSON, emptyCollection.encodePrettily());
    } else if ( (query.contains(FUND_ID_SECOND_DIFFERENT_GROUP) && (query.matches(TRANSACTION_ALLOCATION_TO_QUERY)))
                  || (query.contains(FUND_ID_FIRST_DIFFERENT_GROUP) && (query.matches(TRANSACTION_ALLOCATION_FROM_QUERY)))) {
      Optional<List<Transaction>> allocation1 = getMockEntries(TO_ALLOCATION_FIRST_DIF_GROUP, Transaction.class);
      Optional<List<Transaction>> allocation2 = getMockEntries(TO_ALLOCATION_SECOND_DIF_GROUP, Transaction.class);
      List<Transaction> allocations = List.of(allocation1.get().get(0), allocation2.get().get(0));
      JsonObject jsonObject = JsonObject.mapFrom(new TransactionCollection().withTransactions(allocations).withTotalRecords(2));
      jsonObject.put(TOTAL_RECORDS, allocations.size());
      serverResponse(ctx, 200, APPLICATION_JSON, jsonObject.encodePrettily());
      addServerRqRsData(HttpMethod.GET, testEntity.name(), jsonObject);
    } else if (query.contains(DELETE_CONNECTED_TRANSACTION_ID)) {
      Transaction transaction = new Transaction().withAwaitingPayment((new AwaitingPayment()).withEncumbranceId(DELETE_CONNECTED_TRANSACTION_ID));
      List<Transaction> transactions = List.of(transaction);
      JsonObject collection = JsonObject.mapFrom(new TransactionCollection().withTransactions(transactions).withTotalRecords(1));
      collection.put(TOTAL_RECORDS, 1);
      serverResponse(ctx,200, APPLICATION_JSON, collection.encodePrettily());
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

  private void handleGetCollection(RoutingContext ctx, TestEntities testEntity) {
    logger.info("handleGetCollection got: " + ctx.request().path());

    String query = StringUtils.trimToEmpty(ctx.request().getParam(QUERY));
    addServerRqQuery(testEntity.name(), query);
    if (query.contains(ID_FOR_INTERNAL_SERVER_ERROR)) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else if (query.contains(BAD_QUERY)) {
      serverResponse(ctx, 400, APPLICATION_JSON, BAD_REQUEST.getReasonPhrase());
    } else if (query.contains(ID_DOES_NOT_EXIST)) {
      JsonObject emptyCollection = new JsonObject().put(testEntity.getName(), new JsonArray());
      emptyCollection.put(TOTAL_RECORDS, 0);
      serverResponse(ctx,200, APPLICATION_JSON, emptyCollection.encodePrettily());
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

  private JsonObject getTransactionsByIds(List<String> ids, boolean isCollection) {
    Supplier<List<Transaction>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.TRANSACTIONS.getPathToFileWithData())).mapTo(TransactionCollection.class)
          .getTransactions();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<Transaction> records = getMockEntries(TestEntities.TRANSACTIONS.name(), Transaction.class).orElseGet(getFromFile);

    if (!ids.isEmpty()) {
      records.removeIf(item -> !ids.contains(item.getId()));
    }

    Object record;
    if (isCollection) {
      record = new TransactionCollection().withTransactions(records)
        .withTotalRecords(records.size());
    } else if (!records.isEmpty()) {
      record = records.get(0);
    } else {
      return null;
    }

    return JsonObject.mapFrom(record);
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


  private JsonObject getGroupByIds(List<String> ids, boolean isCollection) {
    Supplier<List<Group>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.GROUP.getPathToFileWithData())).mapTo(GroupsCollection.class)
          .getGroups();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<Group> groups = getMockEntries(TestEntities.GROUP.name(), Group.class).orElseGet(getFromFile);

    if (!ids.isEmpty()) {
      groups.removeIf(item -> !ids.contains(item.getId()));
    }

    Object record;
    if (isCollection) {
      record = new GroupsCollection().withGroups(groups).withTotalRecords(groups.size());
    } else if (!groups.isEmpty()) {
      record = groups.get(0);
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

  private JsonObject getExpenseClassesByIds(List<String> ids, boolean isCollection) {
    Supplier<List<ExpenseClass>> getFromFile = () -> {
      try {
        return new JsonObject(getMockData(TestEntities.EXPENSE_CLASSES.getPathToFileWithData())).mapTo(ExpenseClassCollection.class)
          .getExpenseClasses();
      } catch (IOException e) {
        return Collections.emptyList();
      }
    };

    List<ExpenseClass> expenseClasses = getMockEntries(TestEntities.EXPENSE_CLASSES.name(), ExpenseClass.class).orElseGet(getFromFile);

    if (!ids.isEmpty()) {
      expenseClasses.removeIf(item -> !ids.contains(item.getId()));
    }

    Object record;
    if (isCollection) {
      record = new ExpenseClassCollection().withExpenseClasses(expenseClasses).withTotalRecords(expenseClasses.size());
    } else if (!expenseClasses.isEmpty()) {
      record = expenseClasses.get(0);
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
    case GROUP:
      return getGroupByIds(ids, isCollection);
    case TRANSACTIONS:
      return getTransactionsByIds(ids, isCollection);
    case EXPENSE_CLASSES:
        return getExpenseClassesByIds(ids, isCollection);
    default:
      throw new IllegalArgumentException(testEntity.name() + " entity is unknown");
    }
  }

  private String resourceByIdPath(String field) {
    return resourcesPath(field) + ID_PATH_PARAM;
  }

  private <T> void handleTransactionPutEntry(RoutingContext ctx, Class<T> tClass) {
    logger.info("got: " + ctx.getBodyAsString());

    String tenant = ctx.request()
      .getHeader(OKAPI_HEADER_TENANT);
    if (ERROR_TENANT.equals(tenant)) {
      serverResponse(ctx, 500, TEXT_PLAIN, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else {
      JsonObject body = ctx.getBodyAsJson();
      if (body.getString(ID) == null) {
        body.put(ID, UUID.randomUUID()
          .toString());
      }
      T entry = body.mapTo(tClass);
      Transaction t = ctx.getBodyAsJson().mapTo(Transaction.class);

      if (ID_DOES_NOT_EXIST.equals(t.getId())) {
        serverResponse(ctx, 404, APPLICATION_JSON, t.getId());
        return;
      } else if (t.getId().equals(ID_FOR_INTERNAL_SERVER_ERROR)) {
        serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
        return;
      }
      if (t.getTransactionType() == Transaction.TransactionType.PENDING_PAYMENT) {
        addServerRqRsData(HttpMethod.PUT, TestEntities.TRANSACTIONS_PENDING_PAYMENT.name(), body);
      } else {
        addServerRqRsData(HttpMethod.PUT, TestEntities.TRANSACTIONS.name(), body);
      }

      serverResponse(ctx, 201, APPLICATION_JSON, JsonObject.mapFrom(entry)
        .encodePrettily());
    }
  }

  private <T> void handleTransactionPostEntry(RoutingContext ctx, Class<T> tClass) {
    logger.info("got: " + ctx.getBodyAsString());

    String tenant = ctx.request()
      .getHeader(OKAPI_HEADER_TENANT);
    if (ERROR_TENANT.equals(tenant)) {
      serverResponse(ctx, 500, TEXT_PLAIN, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else {
      JsonObject body = ctx.getBodyAsJson();
      if (body.getString(ID) == null) {
        body.put(ID, UUID.randomUUID()
          .toString());
      }
      T entry = body.mapTo(tClass);
      Transaction t = ctx.getBodyAsJson().mapTo(Transaction.class);
      if (t.getTransactionType() == Transaction.TransactionType.ALLOCATION) {
        addServerRqRsData(HttpMethod.POST, TestEntities.TRANSACTIONS_ALLOCATION.name(), body);
      } else if (t.getTransactionType() == Transaction.TransactionType.TRANSFER) {
        addServerRqRsData(HttpMethod.POST, TestEntities.TRANSACTIONS_TRANSFER.name(), body);
      } else if (t.getTransactionType() == Transaction.TransactionType.ENCUMBRANCE) {
        addServerRqRsData(HttpMethod.POST, TestEntities.TRANSACTIONS_ENCUMBRANCE.name(), body);
      } else if (t.getTransactionType() == Transaction.TransactionType.PAYMENT) {
        addServerRqRsData(HttpMethod.POST, TestEntities.TRANSACTIONS_PAYMENT.name(), body);
      } else if (t.getTransactionType() == Transaction.TransactionType.PENDING_PAYMENT) {
        addServerRqRsData(HttpMethod.POST, TestEntities.TRANSACTIONS_PENDING_PAYMENT.name(), body);
      } else if (t.getTransactionType() == Transaction.TransactionType.CREDIT) {
        addServerRqRsData(HttpMethod.POST, TestEntities.TRANSACTIONS_CREDIT.name(), body);
      }

      serverResponse(ctx, 201, APPLICATION_JSON, JsonObject.mapFrom(entry)
        .encodePrettily());
    }
  }

  private <T> void handlePostEntry(RoutingContext ctx, Class<T> tClass, String entryName) {
    logger.info("got: " + ctx.getBodyAsString());

    String tenant = ctx.request().getHeader(OKAPI_HEADER_TENANT);
    if (ERROR_TENANT.equals(tenant)) {
      serverResponse(ctx, 500, TEXT_PLAIN, INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else if (!getRqRsEntries(HttpMethod.POST, entryName).isEmpty() && tClass == Group.class){
      String constraintStorageError = "{\"errors\":[{\"message\":\"Test\",\"code\":\"uniqueFieldGroupNameError\"" +
        ",\"parameters\":[]}],\"total_records\":1}";
      List<JsonObject> rqRs = getRqRsEntries(HttpMethod.POST, entryName);
      JsonObject rsGroup = rqRs.get(0);
      Group requestEntry = ctx.getBodyAsJson().mapTo(Group.class);
      Group dbEntry = rsGroup.mapTo(Group.class);
      if (requestEntry.getId().equals(dbEntry.getId())) {
        serverResponse(ctx, 400, APPLICATION_JSON, constraintStorageError);
      }
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
    } else if (BUDGET_WITH_BOUNDED_TRANSACTION_ID.equals(id) ){
      serverResponse(ctx, 400, TEXT_PLAIN, TRANSACTION_IS_PRESENT_BUDGET_DELETE_ERROR.getCode());
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
    List<T> entryList =  getRqRsEntries(HttpMethod.SEARCH, objName).stream()
      .map(entries -> entries.mapTo(tClass))
      .collect(toList());
    return Optional.ofNullable(entryList.isEmpty()? null: entryList);
  }

  public static void addMockEntry(String objName, JsonObject data) {
    List<JsonObject> entries = getRqRsEntries(HttpMethod.SEARCH, objName);
    entries.add(data);
    serverRqRs.put(objName, HttpMethod.SEARCH, entries);
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

  public static List<String> getQueryParams(String resourceType) {
    return serverRqQueries.getOrDefault(resourceType, Collections.emptyList());
  }

  private List<String> extractIdsFromQuery(String query) {
    return extractIdsFromQuery(query, "==");
  }

  private List<String> extractIdsFromQuery(String query, String relation) {
    return extractIdsFromQuery(ID, relation, query);
  }

  private List<String> extractIdsFromQuery(String fieldName, String relation, String query) {
    Matcher matcher = Pattern.compile(".*" + fieldName + relation + "\\(?([^)]+).*").matcher(query);
    if (matcher.find()) {
      return StreamEx.split(matcher.group(1), " or ").toList();
    } else {
      return Collections.emptyList();
    }
  }

  private void handleConfigurationModuleResponse(RoutingContext ctx) {
    try {
      String tenant = ctx.request().getHeader(OKAPI_HEADER_TENANT) ;
      String orgConfig = ctx.request().getHeader(X_CONFIG_HEADER_NAME) ;

      String fileName = StringUtils.EMPTY;
      if (EMPTY_CONFIG_X_OKAPI_TENANT.getValue().equals(tenant)) {
        fileName = EMPTY_CONFIG_X_OKAPI_TENANT.getValue();
      } else if (INVALID_CONFIG_X_OKAPI_TENANT.getValue().equals(tenant)) {
        fileName = "invalid_config";
      } else if (X_OKAPI_TENANT.getValue().equals(tenant) || (ctx.request().absoluteURI().contains(SYSTEM_CONFIG_MODULE_NAME))) {
        fileName = "config_localeSEK";
      }

      serverResponse(ctx, 200, APPLICATION_JSON, TestUtils.getMockData(String.format(CONFIG_MOCK_PATH, fileName)));
    } catch (IOException e) {
      serverResponse(ctx, 500, APPLICATION_JSON, INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

  }
}
