package org.folio.rest;

public final class RestConstants {
  public static final String OKAPI_URL = "x-okapi-url";
  public static final int MAX_IDS_FOR_GET_RQ = 15;
  public static final String SEARCH_PARAMS_WITHOUT_LANG = "?limit=%s&offset=%s%s";
  public static final String SEARCH_PARAMS_WITH_LANG = SEARCH_PARAMS_WITHOUT_LANG + "&lang=%s";

  private RestConstants () {

  }
}
