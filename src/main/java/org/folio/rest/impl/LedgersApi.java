package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.util.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.rest.util.HelperUtils.getEndpoint;
import static org.folio.rest.util.HelperUtils.handleErrorResponse;
import static org.folio.rest.util.HelperUtils.populateDataFromLedgerFY;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.Response;

import one.util.streamex.StreamEx;
import org.folio.HttpStatus;
import org.folio.rest.acq.model.finance.LedgerFY;
import org.folio.rest.annotations.Validate;
import org.folio.rest.helper.FundsHelper;
import org.folio.rest.helper.LedgersHelper;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.rest.jaxrs.resource.FinanceLedgers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class LedgersApi implements FinanceLedgers {

  private static final String LEDGERS_LOCATION_PREFIX = getEndpoint(FinanceLedgers.class) + "/%s";

  @Validate
  @Override
  public void postFinanceLedgers(String lang, Ledger entity, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);
    helper.createLedger(entity)
      .thenAccept(type -> handler
        .handle(succeededFuture(helper.buildResponseWithLocation(String.format(LEDGERS_LOCATION_PREFIX, type.getId()), type))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceLedgers(String fiscalYear, int offset, int limit, String query, String lang, Map<String, String> headers,
     Handler<AsyncResult<Response>> handler, Context ctx) {

    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);

    if(Objects.isNull(fiscalYear)) {
      helper.getLedgers(limit, offset, query)
        .thenAccept(ledgersCollection -> handler.handle(succeededFuture(helper.buildOkResponse(ledgersCollection))))
        .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
    } else {
      helper.getLedgers(limit, offset, query)
        .thenCombine(helper.getLedgerFYsByFiscalYearId(fiscalYear), (ledgerCollection, ledgerFYCollection) -> {
          Map<String, LedgerFY> ledgerFYGroupedByLedgerId = StreamEx.of(ledgerFYCollection.getLedgerFY())
            .filter(ledgerFY -> ledgerFY.getFiscalYearId().equals(fiscalYear))
            .toMap(LedgerFY::getLedgerId, value -> value);
          List<Ledger> ledgers = StreamEx.of(ledgerCollection.getLedgers())
            .filter(ledger -> ledgerFYGroupedByLedgerId.containsKey(ledger.getId()))
            .map(ledger -> populateDataFromLedgerFY(ledger, ledgerFYGroupedByLedgerId.get(ledger.getId())))
            .toList();
          return new LedgersCollection().withLedgers(ledgers).withTotalRecords(ledgers.size());
        })
        .thenAccept(types -> handler.handle(succeededFuture(helper.buildOkResponse(types))))
        .exceptionally(fail -> handleErrorResponse(handler, helper, fail));;
    }
  }

  @Validate
  @Override
  public void putFinanceLedgersById(String id, String lang, Ledger entity, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context ctx) {
    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);

    // Set id if this is available only in path
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      helper.addProcessingError(MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
      handler.handle(succeededFuture(helper.buildErrorResponse(422)));
      return;
    }

    helper.updateLedger(entity)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceLedgersById(String ledgerId, String fiscalYearId, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);
    helper.getLedgerWithSummary(ledgerId, fiscalYearId)
      .thenAccept(type -> handler.handle(succeededFuture(helper.buildOkResponse(type))))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void deleteFinanceLedgersById(String id, String lang, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    LedgersHelper helper = new LedgersHelper(headers, ctx, lang);
    helper.deleteLedger(id)
      .thenAccept(types -> handler.handle(succeededFuture(helper.buildNoContentResponse())))
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

  @Validate
  @Override
  public void getFinanceLedgersCurrentFiscalYearById(String ledgerId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> handler, Context vertxContext) {
    FundsHelper helper = new FundsHelper(okapiHeaders, vertxContext, lang);
    helper.getCurrentFiscalYear(ledgerId)
      .thenAccept(currentFiscalYear -> {
        if(Objects.nonNull(currentFiscalYear)) {
          handler.handle(succeededFuture(helper.buildOkResponse(currentFiscalYear)));
        } else {
          handler.handle(succeededFuture(helper.buildErrorResponse(HttpStatus.HTTP_NOT_FOUND.toInt())));
        }
      })
      .exceptionally(fail -> handleErrorResponse(handler, helper, fail));
  }

}
