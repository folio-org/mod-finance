package org.folio.rest.util;

import java.util.List;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import org.folio.rest.jaxrs.model.Transaction;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;

public final class MoneyUtils {

  private MoneyUtils(){

  }

  public static double calculateExpendedPercentage(MonetaryAmount expended, double totalExpended) {
    return expended.divide(totalExpended).multiply(100).with(Monetary.getDefaultRounding()).getNumber().doubleValue();
  }

  public static double calculateCreditedPercentage(MonetaryAmount credited, double totalCredited) {
    return credited.divide(totalCredited).multiply(100).with(Monetary.getDefaultRounding()).getNumber().doubleValue();
  }

  public static MonetaryAmount calculateTotalAmount(List<Transaction> transactions, CurrencyUnit currency) {
    return transactions.stream()
            .map(transaction -> (MonetaryAmount) Money.of(transaction.getAmount(), currency))
            .reduce(MonetaryFunctions::sum).orElse(Money.zero(currency));
  }

  public static double calculateTotalAmountWithRounding(List<Transaction> transactions, CurrencyUnit currency) {
    return MoneyUtils.calculateTotalAmount(transactions, currency).with(Monetary.getDefaultRounding()).getNumber().doubleValue();
  }
}
