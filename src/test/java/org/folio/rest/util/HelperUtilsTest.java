package org.folio.rest.util;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class HelperUtilsTest {

  @Test
  public void errorMessageIsJSON() {
    String errorMsg = "{\"message\":\"Test\",\"code\":\"Test\",\"parameters\":[]}";
    boolean act = HelperUtils.isErrorMessageJson(errorMsg);
    Assert.assertTrue(act);
  }

  @Test
  public void errorMessageIsNotValidJSON() {
    String errorMsg = "{\"message\":\"Test\"}";
    boolean act = HelperUtils.isErrorMessageJson(errorMsg);
    Assert.assertFalse(act);
  }

  @Test
  public void testShouldReturnEmptyList() throws ExecutionException, InterruptedException {
    CompletableFuture<List<String>> actFuture= HelperUtils.emptyListFuture();
    assertEquals(Collections.<String>emptyList(), actFuture.get());
  }

  @Test
  public void testShouldReturnEmptyIfExpressionEmpty() {
    String actCql = HelperUtils.combineCqlExpressions("and");
    Assert.assertEquals(EMPTY, actCql);
  }

  @Test
  public void testShouldReturnCqlWhereSortOptionInTheEnd() {
    String SORT_BY = " sortBy createdDate/sort.descending";
    String actCql = HelperUtils.combineCqlExpressions("and", SORT_BY);
    Assert.assertTrue(actCql.contains(SORT_BY));
  }
}
