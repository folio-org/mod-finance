package org.folio.rest.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.mockito.ArgumentMatchers;

public final class TestUtils {

  private TestUtils() {}

  public static String getMockData(String path) throws IOException {

    try (InputStream resourceAsStream = TestConstants.class.getClassLoader().getResourceAsStream(path)) {
      if (resourceAsStream != null) {
        return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      } else {
        StringBuilder sb = new StringBuilder();
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
          lines.forEach(sb::append);
        }
        return sb.toString();
      }
    }
  }

  public static Date convertLocalDateTimeToDate(LocalDateTime dateToConvert) {
    return Date
      .from(dateToConvert.atZone(ZoneId.systemDefault())
        .toInstant());
  }

  public static String assertQueryContains(String query) {
    return ArgumentMatchers.contains(URLEncoder.encode(query, StandardCharsets.UTF_8));
  }
}
