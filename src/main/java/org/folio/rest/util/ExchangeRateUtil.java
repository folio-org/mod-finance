package org.folio.rest.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.ExchangeRate;

@UtilityClass
public class ExchangeRateUtil {

  public static ExchangeRate.OperationMode getManualOperationMode(String operationMode) {
    return StringUtils.isNotBlank(operationMode)
      ? ExchangeRate.OperationMode.fromValue(operationMode.toUpperCase())
      : null;
  }
}
