package org.folio.rest.util;

import org.javamoney.moneta.Money;

public class MoneyUtils {

  private MoneyUtils(){

  }

  public static Number sumValues(Number d1, Number d2, String currency) {
    return Money.of(d1, currency).add(Money.of(d2, currency)).getNumber();
  }

  public static Number subtractValues(Double d1, Double d2, String currency) {
    return Money.of(d1, currency).subtract(Money.of(d2, currency)).getNumber();
  }
}
