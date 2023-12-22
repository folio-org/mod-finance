package org.folio.rest.core.models;

import java.util.Collections;
import java.util.Map;

import io.vertx.core.Context;

public record RequestContext(Context context, Map<String, String> headers) {

  @Override
  public Map<String, String> headers() {
    return Collections.unmodifiableMap(headers);
  }
}
