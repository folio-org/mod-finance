package org.folio.services.fund;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.services.ledger.LedgerDetailsService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.util.ErrorCodes.CURRENT_FISCAL_YEAR_NOT_FOUND;

public class FundFiscalYearService {
  private final LedgerDetailsService ledgerDetailsService;
  private final FundService fundService;

  public FundFiscalYearService(LedgerDetailsService ledgerDetailsService, FundService fundService) {
    this.ledgerDetailsService = ledgerDetailsService;
    this.fundService = fundService;
  }

  public CompletableFuture<FiscalYear> retrieveCurrentFiscalYear(String fundId, RequestContext rqContext) {
    return fundService.retrieveFundById(fundId, rqContext)
      .thenApply(Fund::getLedgerId)
      .thenCompose(budgetLedgerId -> getCurrentFiscalYear(budgetLedgerId, rqContext));
  }

  public CompletableFuture<FiscalYear> retrievePlannedFiscalYear(String fundId, RequestContext rqContext) {
    return fundService.retrieveFundById(fundId, rqContext)
      .thenApply(Fund::getLedgerId)
      .thenCompose(budgetLedgerId -> getPlannedFiscalYear(budgetLedgerId, rqContext));
  }

  public CompletableFuture<FiscalYear> getCurrentFiscalYearByLedgerId(String budgetLedgerId, RequestContext rqContext) {
    return  getCurrentFiscalYear(budgetLedgerId, rqContext);
  }

  private CompletableFuture<FiscalYear> getCurrentFiscalYear(String budgetLedgerId, RequestContext rqContext) {
    return ledgerDetailsService.getCurrentFiscalYear(budgetLedgerId, rqContext)
      .thenApply(fiscalYear ->
        Optional.ofNullable(fiscalYear)
          .orElseThrow(() -> new HttpException(404, CURRENT_FISCAL_YEAR_NOT_FOUND.toError()))
      );
  }

  private CompletableFuture<FiscalYear> getPlannedFiscalYear(String budgetLedgerId, RequestContext rqContext) {
    return ledgerDetailsService.getPlannedFiscalYear(budgetLedgerId, rqContext);
  }
}
