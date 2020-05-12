package org.folio.rest.impl;

import org.folio.rest.util.HelperUtils;
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
}
