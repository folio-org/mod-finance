package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.folio.rest.annotations.Validate;

import org.folio.rest.jaxrs.resource.FinanceFunds;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class FinanceFundsImpl implements FinanceFunds {

  private static final Logger logger = LoggerFactory.getLogger(FinanceFundsImpl.class);
  private static final String NOT_SUPPORTED = "Not supported"; // To overcome sonarcloud warning

  @Override
  @Validate
  public void postFinanceFunds(String lang, org.folio.rest.jaxrs.model.Finance entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info(" === postFinanceFunds === ");
    asyncResultHandler.handle(succeededFuture(PostFinanceFundsResponse.respond500WithTextPlain(NOT_SUPPORTED)));

  }

  @Override
  @Validate
  public void getFinanceFunds(int offset, int limit, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info(" === getFinanceFunds === ");
    asyncResultHandler.handle(succeededFuture(GetFinanceFundsResponse.respond500WithTextPlain(NOT_SUPPORTED)));
  }

  @Override
  @Validate
  public void putFinanceFundsById(String id, String lang, org.folio.rest.jaxrs.model.Finance entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info(" === putFinanceFundsById === ");
    asyncResultHandler.handle(succeededFuture(PutFinanceFundsByIdResponse.respond500WithTextPlain(NOT_SUPPORTED)));

  }

  @Override
  @Validate
  public void getFinanceFundsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info(" === getFinanceFundsById === ");
    asyncResultHandler.handle(succeededFuture(GetFinanceFundsByIdResponse.respond500WithTextPlain(NOT_SUPPORTED)));

  }

  @Validate
  @Override
  public void deleteFinanceFundsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info(" === deleteFinanceFundsById === ");
    asyncResultHandler.handle(succeededFuture(DeleteFinanceFundsByIdResponse.respond500WithTextPlain(NOT_SUPPORTED)));
  }
}
