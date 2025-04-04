package org.folio.services.exchange;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

@UtilityClass
public class ExchangeUtil {

  public static String getFiscalQuarterLastDay(ZonedDateTime currentDateTime) {
    var currentQuarter = currentDateTime.get(IsoFields.QUARTER_OF_YEAR);

    var lastQuarterFirstDay = switch (currentQuarter) {
      case 1 -> LocalDate.of(currentDateTime.getYear() - 1, 9, 1);
      case 2 -> LocalDate.of(currentDateTime.getYear() - 1, 12, 1);
      case 3 -> LocalDate.of(currentDateTime.getYear(), 1, 1);
      case 4 -> LocalDate.of(currentDateTime.getYear(), 3, 1);
      default -> throw new IllegalStateException("Unsupported operation");
    };

    return lastQuarterFirstDay
      .with(TemporalAdjusters.lastDayOfMonth())
      .format(DateTimeFormatter.ISO_LOCAL_DATE);
  }
}
