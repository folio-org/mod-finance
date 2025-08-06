package org.folio.rest.impl;

import javax.money.convert.MonetaryConversions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.config.ApplicationConfig;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.spring.SpringContextUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * The class initializes vertx context adding spring context
 */
public class InitAPIs implements InitAPI {
  private final Logger logger = LogManager.getLogger(InitAPIs.class);

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> resultHandler) {
    vertx.executeBlocking(() -> {
      initJavaMoney();
      SpringContextUtil.init(vertx, context, ApplicationConfig.class);
      return true;
    }).onComplete(result -> {
      if (result.succeeded()) {
        resultHandler.handle(Future.succeededFuture(true));
      } else {
        logger.error("Failure to init API", result.cause());
        resultHandler.handle(Future.failedFuture(result.cause()));
      }
    });
  }

  private void initJavaMoney() {
    try {
      logger.info("Available currency rates providers {}", MonetaryConversions.getDefaultConversionProviderChain());
    } catch (Exception e) {
      logger.error("Java Money API preload failed", e);
    }
  }
}
