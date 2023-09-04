package org.folio.services.fund;

import static org.folio.rest.util.ErrorCodes.CURRENT_FISCAL_YEAR_NOT_FOUND;

import java.util.Optional;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.services.ledger.LedgerDetailsService;

import io.vertx.core.Future;

public class FundFiscalYearService {
  private final LedgerDetailsService ledgerDetailsService;
  private final FundService fundService;

  public FundFiscalYearService(LedgerDetailsService ledgerDetailsService, FundService fundService) {
    this.ledgerDetailsService = ledgerDetailsService;
    this.fundService = fundService;
  }

  public Future<FiscalYear> retrieveCurrentFiscalYear(String fundId, RequestContext rqContext) {
    return fundService.getFundById(fundId, rqContext)
      .map(Fund::getLedgerId)
      .compose(budgetLedgerId -> getCurrentFiscalYear(budgetLedgerId, rqContext));
  }

  public Future<FiscalYear> retrievePlannedFiscalYear(String fundId, RequestContext rqContext) {
    return fundService.getFundById(fundId, rqContext)
      .map(Fund::getLedgerId)
      .compose(budgetLedgerId -> getPlannedFiscalYear(budgetLedgerId, rqContext));
  }

  private Future<FiscalYear> getCurrentFiscalYear(String budgetLedgerId, RequestContext rqContext) {
    return ledgerDetailsService.getCurrentFiscalYear(budgetLedgerId, rqContext)
      .map(fiscalYear -> Optional.ofNullable(fiscalYear)
        .orElseThrow(() -> new HttpException(404, CURRENT_FISCAL_YEAR_NOT_FOUND.toError())));
  }

  private Future<FiscalYear> getPlannedFiscalYear(String budgetLedgerId, RequestContext rqContext) {
    return ledgerDetailsService.getPlannedFiscalYear(budgetLedgerId, rqContext);
  }
}
