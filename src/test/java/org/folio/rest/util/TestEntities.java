package org.folio.rest.util;

import static org.folio.rest.impl.ApiTestBase.getMockData;
import static org.folio.rest.util.HelperUtils.ID;

import java.io.IOException;
import java.util.function.Supplier;

import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundType;
import org.folio.rest.jaxrs.resource.FinanceFundTypes;
import org.folio.rest.jaxrs.resource.FinanceFunds;
import org.folio.rest.tools.parser.JsonPathParser;

import io.vertx.core.json.JsonObject;

public enum TestEntities {
  FUND(getEndpoint(FinanceFunds.class), Fund.class, "mockdata/funds/funds.json", "funds[0]", "name", "History", 1),
  FUND_TYPE(getEndpoint(FinanceFundTypes.class), FundType.class, "mockdata/fund-types/types.json", "fundTypes[0]", "name", "New type name", 1);

  TestEntities(String endpoint, Class clazz, String pathToSamples, String jsonPathToSample, String updatedFieldName,
      String updatedFieldValue, int collectionQuantity) {
    this.endpoint = endpoint;
    this.clazz = clazz;
    this.jsonPathToObject = jsonPathToSample;
    this.pathToFileWithData = pathToSamples;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
    this.collectionQuantity = collectionQuantity;
  }

  private int collectionQuantity;
  private String endpoint;
  private String jsonPathToObject;
  private String pathToFileWithData;
  private String updatedFieldName;
  private String updatedFieldValue;
  private Class clazz;

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

  public String getUpdatedFieldValue() {
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

  private static String getEndpoint(Class<?> clazz) {
    return HelperUtils.getEndpoint(clazz);
  }
}
