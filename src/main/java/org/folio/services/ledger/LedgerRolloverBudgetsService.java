package org.folio.services.ledger;

import static org.folio.rest.util.ResourcePathResolver.LEDGER_ROLLOVERS_BUDGETS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudget;
import org.folio.rest.jaxrs.model.LedgerFiscalYearRolloverBudgetCollection;
import org.folio.services.configuration.CommonSettingsService;
import org.javamoney.moneta.Money;

import io.vertx.core.Future;

import javax.money.Monetary;
import javax.money.MonetaryOperator;
import java.util.Objects;

public class LedgerRolloverBudgetsService {
  private final RestClient restClient;
  private final CommonSettingsService commonSettingsService;

  public LedgerRolloverBudgetsService(RestClient restClient, CommonSettingsService commonSettingsService) {
    this.restClient = restClient;
    this.commonSettingsService = commonSettingsService;
  }

  public Future<LedgerFiscalYearRolloverBudget> retrieveLedgerRolloverBudgetById(String id, RequestContext requestContext) {
    return restClient.get(resourceByIdPath(LEDGER_ROLLOVERS_BUDGETS_STORAGE, id), LedgerFiscalYearRolloverBudget.class, requestContext)
    .compose(budget -> commonSettingsService.getSystemCurrency(requestContext)
        .map(currency -> withRoundedAmounts(budget, currency)));
  }

  public Future<LedgerFiscalYearRolloverBudgetCollection> retrieveLedgerRolloverBudgets(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(LEDGER_ROLLOVERS_BUDGETS_STORAGE))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), LedgerFiscalYearRolloverBudgetCollection.class, requestContext)
      .compose(collection -> commonSettingsService.getSystemCurrency(requestContext)
        .map(currency -> {
          collection.getLedgerFiscalYearRolloverBudgets().forEach(budget -> withRoundedAmounts(budget, currency));
          return collection;
        }));
  }

  private LedgerFiscalYearRolloverBudget withRoundedAmounts(LedgerFiscalYearRolloverBudget budget, String currency) {
    MonetaryOperator rounding = Monetary.getDefaultRounding();
    return budget
      .withInitialAllocation(roundAmount(budget.getInitialAllocation(), currency, rounding))
      .withAllocationTo(roundAmount(budget.getAllocationTo(), currency, rounding))
      .withAllocationFrom(roundAmount(budget.getAllocationFrom(), currency, rounding))
      .withNetTransfers(roundAmount(budget.getNetTransfers(), currency, rounding))
      .withEncumbered(roundAmount(budget.getEncumbered(), currency, rounding))
      .withAwaitingPayment(roundAmount(budget.getAwaitingPayment(), currency, rounding))
      .withExpenditures(roundAmount(budget.getExpenditures(), currency, rounding))
      .withCredits(roundAmount(budget.getCredits(), currency, rounding))
      .withUnavailable(roundAmount(budget.getUnavailable(), currency, rounding))
      .withAvailable(roundAmount(budget.getAvailable(), currency, rounding))
      .withCashBalance(roundAmount(budget.getCashBalance(), currency, rounding))
      .withOverEncumbrance(roundAmount(budget.getOverEncumbrance(), currency, rounding))
      .withOverExpended(roundAmount(budget.getOverExpended(), currency, rounding))
      .withTotalFunding(roundAmount(budget.getTotalFunding(), currency, rounding));
  }

  private Double roundAmount(Double amount, String currency, MonetaryOperator rounding) {
    if (Objects.isNull(amount)) {
      return null;
    }
    return Money.of(amount, currency).with(rounding).getNumber().doubleValue();
  }
}
