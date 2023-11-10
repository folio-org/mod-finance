package org.folio.rest.util;

import static org.folio.rest.util.HelperUtils.ID;
import static org.folio.rest.util.TestUtils.getMockData;

import java.io.IOException;
import java.util.function.Supplier;

import org.folio.rest.jaxrs.model.FundCodeExpenseClassesCollection;
import org.folio.rest.jaxrs.resource.FinanceFundCodesExpenseClasses;
import org.folio.rest.tools.parser.JsonPathParser;

import io.vertx.core.json.JsonObject;

public enum EntityForTest {
  FUND_CODE_EXPENSE_CLASS("fundCodeExpenseClass", getEndpoint(FinanceFundCodesExpenseClasses.class),
    FundCodeExpenseClassesCollection.class, "mockdata/finance/fund-codes-expense-class.json",
    "fund-codes-expense-class[0]", "name", "Updated name", 5, "allocated");

  EntityForTest(String name, String endpoint, Class<?> clazz, String pathToSamples, String jsonPathToSample, String updatedFieldName,
               Object updatedFieldValue, int collectionQuantity, String ignoreProperties) {
    this.name = name;
    this.endpoint = endpoint;
    this.clazz = clazz;
    this.jsonPathToObject = jsonPathToSample;
    this.pathToFileWithData = pathToSamples;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
    this.collectionQuantity = collectionQuantity;
    this.ignoreProperties = ignoreProperties;
  }

  private final String name;
  private final int collectionQuantity;
  private final String endpoint;
  private final String jsonPathToObject;
  private final String pathToFileWithData;
  private final String updatedFieldName;
  private String ignoreProperties;
  private final Object updatedFieldValue;
  private final Class<?> clazz;

  public String getEndpoint() {
    return endpoint;
  }

  public String getEndpointWithDefaultId() {
    return endpoint + "/" + getMockObject().getString(ID);
  }

  public String getEndpointWithId(String id) {
    return endpoint + "/" + id;
  }

  public String getUpdatedFieldName() {
    return updatedFieldName;
  }

  public Object getUpdatedFieldValue() {
    return updatedFieldValue;
  }

  public int getCollectionQuantity() {
    return collectionQuantity;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public String getPathToFileWithData() {
    return pathToFileWithData;
  }

  public JsonObject getMockObject() {
    return getMockObject(jsonPathToObject);
  }

  public JsonObject getMockObject(String jsonPath) {
    Supplier<JsonObject> jsonFromFile = () -> {
      try {
        return new JsonObject(getMockData(pathToFileWithData));
      } catch (IOException e) {
        return null;
      }
    };
    return (JsonObject) new JsonPathParser(jsonFromFile.get()).getValueAt(jsonPath);
  }

  public static String getEndpoint(Class<?> clazz) {
    return HelperUtils.getEndpoint(clazz);
  }

  public String getName() {
    return name;
  }

  public String getIgnoreProperties() {
    return ignoreProperties;
  }

  public void setIgnoreProperties(String ignoreProperties) {
    this.ignoreProperties = ignoreProperties;
  }
}
