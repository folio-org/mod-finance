package org.folio.rest.util;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

public class HelperUtilsTest {

  @Test
  public void testShouldReturnEmptyList() throws ExecutionException, InterruptedException {
    CompletableFuture<List<String>> actFuture= HelperUtils.emptyListFuture();
    assertEquals(Collections.<String>emptyList(), actFuture.get());
  }
}
