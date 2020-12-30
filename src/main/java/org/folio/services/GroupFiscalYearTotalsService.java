package org.folio.services;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummaryCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.folio.rest.util.HelperUtils;

public class GroupFiscalYearTotalsService {

  private final RestClient budgetRestClient;
  private final GroupFundFiscalYearService groupFundFiscalYearService;

  public GroupFiscalYearTotalsService(RestClient budgetRestClient, GroupFundFiscalYearService groupFundFiscalYearService) {
    this.budgetRestClient = budgetRestClient;
    this.groupFundFiscalYearService = groupFundFiscalYearService;
  }

  public CompletableFuture<GroupFiscalYearSummaryCollection> getGroupFiscalYearSummaries(String query,
      RequestContext requestContext) {
    return budgetRestClient.get(query, 0, Integer.MAX_VALUE, requestContext, BudgetsCollection.class)
      .thenCombine(groupFundFiscalYearService.getGroupFundFiscalYears(query, 0, Integer.MAX_VALUE, requestContext),
          (budgetsCollection, groupFundFiscalYearCollection) -> {

            Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetsMap = budgetsCollection.getBudgets()
              .stream()
              .collect(groupingBy(Budget::getFundId, groupingBy(Budget::getFiscalYearId, toList())));

            List<GroupFiscalYearSummary> summaries = groupSummariesByGroupIdAndFiscalYearId(groupFundFiscalYearCollection,
                fundIdFiscalYearIdBudgetsMap).values()
                  .stream()
                  .flatMap(summary -> summary.values()
                    .stream())
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(toList());

            return new GroupFiscalYearSummaryCollection().withGroupFiscalYearSummaries(summaries)
              .withTotalRecords(summaries.size());
          });
  }

  private Map<String, Map<String, Optional<GroupFiscalYearSummary>>> groupSummariesByGroupIdAndFiscalYearId(
      GroupFundFiscalYearCollection groupFundFiscalYearCollection,
      Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetsMap) {
    return groupFundFiscalYearCollection.getGroupFundFiscalYears()
      .stream()
      .collect(groupingBy(GroupFundFiscalYear::getGroupId,
          groupingBy(GroupFundFiscalYear::getFiscalYearId, mapping(map(fundIdFiscalYearIdBudgetsMap), reducing(reduce())))));
  }

  private Function<GroupFundFiscalYear, GroupFiscalYearSummary> map(
      Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetMap) {
    return groupFundFiscalYear -> {
      String fundId = groupFundFiscalYear.getFundId();
      String fiscalYearId = groupFundFiscalYear.getFiscalYearId();
      String groupId = groupFundFiscalYear.getGroupId();
      if (isBudgetExists(fundIdFiscalYearIdBudgetMap, fundId, fiscalYearId)) {
        return buildGroupFiscalYearSummary(fiscalYearId, groupId, fundIdFiscalYearIdBudgetMap.get(fundId)
          .get(fiscalYearId));
      } else {
        return buildDefaultGroupFiscalYearSummary(fiscalYearId, groupId);
      }
    };
  }

  private BinaryOperator<GroupFiscalYearSummary> reduce() {
    return (original, update) -> {
      updateGroupFiscalYearSummary(original, update);
      return original;
    };
  }

  private GroupFiscalYearSummary buildDefaultGroupFiscalYearSummary(String fiscalYearId, String groupId) {
    return new GroupFiscalYearSummary().withGroupId(groupId)
      .withFiscalYearId(fiscalYearId)
      .withAllocated(0d)
      .withAvailable(0d)
      .withUnavailable(0d)
      .withAllocationFrom(0d)
      .withAllocationTo(0d)
      .withAwaitingPayment(0d)
      .withEncumbered(0d)
      .withExpenditures(0d)
      .withNetTransfers(0d)
      .withInitialAllocation(0d)
      .withTotalFunding(0d)
      .withCashBalance(0d)
      .withOverEncumbrance(0d)
      .withOverExpended(0d);
  }

  private GroupFiscalYearSummary buildGroupFiscalYearSummary(String fiscalYearId, String groupId, List<Budget> budgets) {
    GroupFiscalYearSummary summary = buildDefaultGroupFiscalYearSummary(fiscalYearId, groupId);

    double allocatedTotal = HelperUtils.calculateTotals(budgets, Budget::getAllocated);
    double availableTotal = HelperUtils.calculateTotals(budgets, Budget::getAvailable);
    double unavailableTotal = HelperUtils.calculateTotals(budgets, Budget::getUnavailable);
    double netTransfersTotal = HelperUtils.calculateTotals(budgets, Budget::getNetTransfers);
    double initialAllocation = HelperUtils.calculateTotals(budgets, Budget::getInitialAllocation);
    double allocationTo = HelperUtils.calculateTotals(budgets, Budget::getAllocationTo);
    double allocationFrom = HelperUtils.calculateTotals(budgets, Budget::getAllocationFrom);
    double awaitingPayment = HelperUtils.calculateTotals(budgets, Budget::getAwaitingPayment);
    double encumbered = HelperUtils.calculateTotals(budgets, Budget::getEncumbered);
    double expenditures = HelperUtils.calculateTotals(budgets, Budget::getExpenditures);
    double overEncumbrance = HelperUtils.calculateTotals(budgets, Budget::getOverEncumbrance);
    double overExpended = HelperUtils.calculateTotals(budgets, Budget::getOverExpended);
    double totalFunding = HelperUtils.calculateTotals(budgets, Budget::getTotalFunding);
    double cashBalance = HelperUtils.calculateTotals(budgets, Budget::getCashBalance);

    return summary.withAllocated(allocatedTotal)
      .withAvailable(availableTotal)
      .withUnavailable(unavailableTotal)
      .withNetTransfers(netTransfersTotal)
      .withInitialAllocation(initialAllocation)
      .withAllocationTo(allocationTo)
      .withAllocationFrom(allocationFrom)
      .withAwaitingPayment(awaitingPayment)
      .withEncumbered(encumbered)
      .withExpenditures(expenditures)
      .withOverEncumbrance(overEncumbrance)
      .withOverExpended(overExpended)
      .withTotalFunding(totalFunding)
      .withCashBalance(cashBalance);
  }

  private void updateGroupFiscalYearSummary(GroupFiscalYearSummary original, GroupFiscalYearSummary update) {
    original.setAllocated(BigDecimal.valueOf(original.getAllocated())
      .add(BigDecimal.valueOf(update.getAllocated()))
      .doubleValue());
    original.setAvailable(BigDecimal.valueOf(original.getAvailable())
      .add(BigDecimal.valueOf(update.getAvailable()))
      .doubleValue());
    original.setUnavailable(BigDecimal.valueOf(original.getUnavailable())
      .add(BigDecimal.valueOf(update.getUnavailable()))
      .doubleValue());
    original.setNetTransfers(BigDecimal.valueOf(original.getNetTransfers())
      .add(BigDecimal.valueOf(update.getNetTransfers()))
      .doubleValue());
    original.setInitialAllocation(BigDecimal.valueOf(original.getInitialAllocation())
      .add(BigDecimal.valueOf(update.getInitialAllocation()))
      .doubleValue());
    original.setAllocationTo(BigDecimal.valueOf(original.getAllocationTo())
      .add(BigDecimal.valueOf(update.getAllocationTo()))
      .doubleValue());
    original.setAllocationFrom(BigDecimal.valueOf(original.getAllocationFrom())
      .add(BigDecimal.valueOf(update.getAllocationFrom()))
      .doubleValue());
    original.setAwaitingPayment(BigDecimal.valueOf(original.getAwaitingPayment())
      .add(BigDecimal.valueOf(update.getAwaitingPayment()))
      .doubleValue());
    original.setEncumbered(BigDecimal.valueOf(original.getEncumbered())
      .add(BigDecimal.valueOf(update.getEncumbered()))
      .doubleValue());
    original.setExpenditures(BigDecimal.valueOf(original.getExpenditures())
      .add(BigDecimal.valueOf(update.getExpenditures()))
      .doubleValue());
    original.setTotalFunding(BigDecimal.valueOf(original.getTotalFunding())
      .add(BigDecimal.valueOf(update.getTotalFunding()))
      .doubleValue());
    original.setCashBalance(BigDecimal.valueOf(original.getCashBalance())
      .add(BigDecimal.valueOf(update.getCashBalance()))
      .doubleValue());
    original.setOverEncumbrance(BigDecimal.valueOf(original.getOverEncumbrance())
      .add(BigDecimal.valueOf(update.getOverEncumbrance()))
      .doubleValue());
    original.setOverExpended(BigDecimal.valueOf(original.getOverExpended())
      .add(BigDecimal.valueOf(update.getOverExpended()))
      .doubleValue());

  }

  private boolean isBudgetExists(Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetMap, String fundId,
      String fiscalYearId) {
    return Objects.nonNull(fundIdFiscalYearIdBudgetMap.get(fundId)) && Objects.nonNull(fundIdFiscalYearIdBudgetMap.get(fundId)
      .get(fiscalYearId));
  }
}
