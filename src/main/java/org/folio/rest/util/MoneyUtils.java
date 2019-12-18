package org.folio.rest.util;

import org.javamoney.moneta.Money;

public class MoneyUtils {

  private MoneyUtils(){

  }

  public static Number sumValues(Number d1, Number d2, String currency) {
    return Money.of(d1, currency).add(Money.of(d2, currency)).getNumber();
  }

  public static Number subtractValues(Number d1, Number d2, String currency) {
    return Money.of(d1, currency).subtract(Money.of(d2, currency)).getNumber();
  }

  public static Double sumDoubleValues(Double d1, Double d2, String currency) {
    return sumValues(d1, d2, currency).doubleValue();
  }

  public static Double subtractDoubleValues(Double d1, Double d2, String currency) {
    return subtractValues(d1, d2, currency).doubleValue();
  }

  public static Double multiplyDoubleValues(Double d1, Double d2, String currency) {
    return subtractValues(d1, d2, currency).doubleValue();
  }

  public static Double divideDoubleValues(Double d1, Double d2, String currency) {
    return subtractValues(d1, d2, currency).doubleValue();
  }
}
