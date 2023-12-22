package org.folio.rest.util;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.folio.rest.jaxrs.model.Error;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HelperUtilsTest {

  @Test
  public void errorMessageIsJSON() {
    String errorMsg = "{\"message\":\"Test\",\"code\":\"Test\",\"parameters\":[]}";
    boolean act = HelperUtils.isJsonOfType(errorMsg, Error.class);
    Assertions.assertTrue(act);
  }

  @Test
  public void errorMessageIsNotValidJSON() {
    String errorMsg = "{\"message\":\"Test\"}";
    boolean act = HelperUtils.isJsonOfType(errorMsg, Error.class);
    Assertions.assertTrue(act);
  }

  @Test
  public void testShouldReturnEmptyIfExpressionEmpty() {
    String actCql = HelperUtils.combineCqlExpressions("and");
    Assertions.assertEquals(EMPTY, actCql);
  }

  @Test
  public void testShouldReturnCqlWhereSortOptionInTheEnd() {
    String SORT_BY = " sortBy createdDate/sort.descending";
    String actCql = HelperUtils.combineCqlExpressions("and", SORT_BY);
    Assertions.assertTrue(actCql.contains(SORT_BY));
  }
}
