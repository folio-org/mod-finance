package org.folio.rest.impl;

import org.folio.rest.util.HelperUtils;
import org.junit.Assert;
import org.junit.Test;

public class HelperUtilsTest {
  @Test
  public void errorMessagaIsJSON() {
    String errorMsg = "{\"message\":\"Test\",\"code\":\"Test\",\"parameters\":[]}";
    boolean act = HelperUtils.isErrorMessageJson(errorMsg);
    Assert.assertTrue(act);
  }
}
