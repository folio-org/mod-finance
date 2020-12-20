package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.resource.FinanceLedgerRolloversErrors;
import org.folio.services.LedgerRolloversErrorsService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.Pattern;
import javax.ws.rs.core.Response;
import java.util.Map;

public class LedgerRolloversErrorsApi extends BaseApi implements FinanceLedgerRolloversErrors {

  @Autowired
  private LedgerRolloversErrorsService ledgerRolloversErrorsService;

  @Override
  @Validate
  public void getFinanceLedgerRolloversErrors(@Pattern(regexp = "[a-zA-Z]{2}") String lang, String accept, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    //  TODO: call ledgerRolloversErrorsService and return response context with "Not implemented yet exception"
  }

  @Override
  @Validate
  public void getFinanceLedgerRolloversErrorsById(@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$") String id, @Pattern(regexp = "[a-zA-Z]{2}") String lang, String accept, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    //  TODO: call ledgerRolloversErrorsService and return response context with "Not implemented yet exception"
  }
}
