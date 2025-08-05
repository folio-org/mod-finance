package org.folio.rest.util;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.Context;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CacheUtils {

  public static <K, V> Cache<K, V> buildCache(Context context, long cacheExpirationTime) {
    return buildCache(task -> context.runOnContext(v -> task.run()), cacheExpirationTime);
  }

  private static <K, V> Cache<K, V> buildCache(Executor executor, long cacheExpirationTime) {
    return Caffeine.newBuilder()
      .expireAfterWrite(cacheExpirationTime, TimeUnit.SECONDS)
      .executor(executor)
      .build();
  }
}
