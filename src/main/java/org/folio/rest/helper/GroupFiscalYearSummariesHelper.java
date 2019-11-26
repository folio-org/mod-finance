package org.folio.rest.helper;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

import io.vertx.core.Context;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummaryCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Function;


public class GroupFiscalYearSummariesHelper extends AbstractHelper {

  private BudgetsHelper budgetsHelper;
  private GroupFundFiscalYearHelper groupFundFiscalYearHelper;

  public GroupFiscalYearSummariesHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
    budgetsHelper = new BudgetsHelper(httpClient, okapiHeaders, ctx, lang);
    groupFundFiscalYearHelper = new GroupFundFiscalYearHelper(httpClient, okapiHeaders, ctx, lang);
  }

  public CompletableFuture<GroupFiscalYearSummaryCollection> getGroupFiscalYearSummaries(int limit, int offset, String query) {
    return budgetsHelper.getBudgets(Integer.MAX_VALUE, 0, query)
      .thenCombine(groupFundFiscalYearHelper.getGroupFundFiscalYears(limit, offset, query), (budgetsCollection, groupFundFiscalYearCollection) -> {

        Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetsMap = budgetsCollection.getBudgets().stream()
          .collect(groupingBy(Budget::getFundId,
            groupingBy(Budget::getFiscalYearId, toList()))
          );

        List<GroupFiscalYearSummary> summaries = groupSummariesByGroupIdAndFiscalYearId(groupFundFiscalYearCollection, fundIdFiscalYearIdBudgetsMap)
          .values().stream()
          .flatMap(summary -> summary.values().stream())
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(toList());

        return new GroupFiscalYearSummaryCollection().withGroupFiscalYearSummaries(summaries).withTotalRecords(summaries.size());
      });
  }

  private Map<String, Map<String, Optional<GroupFiscalYearSummary>>> groupSummariesByGroupIdAndFiscalYearId(GroupFundFiscalYearCollection groupFundFiscalYearCollection, Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetsMap) {
    return groupFundFiscalYearCollection.getGroupFundFiscalYears().stream()
      .collect(
        groupingBy(GroupFundFiscalYear::getGroupId,
          groupingBy(GroupFundFiscalYear::getFiscalYearId,
            mapping(map(fundIdFiscalYearIdBudgetsMap),
              reducing(reduce())
            )
          )
        )
      );
  }

  private Function<GroupFundFiscalYear, GroupFiscalYearSummary> map(Map<String, Map<String, List<Budget>>>  fundIdFiscalYearIdBudgetMap) {
    return groupFundFiscalYear -> {
      String fundId = groupFundFiscalYear.getFundId();
      String fiscalYearId = groupFundFiscalYear.getFiscalYearId();
      String groupId = groupFundFiscalYear.getGroupId();
      if(isBudgetExists(fundIdFiscalYearIdBudgetMap, fundId, fiscalYearId)) {
        return buildGroupFiscalYearSummary(fiscalYearId, groupId, fundIdFiscalYearIdBudgetMap.get(fundId).get(fiscalYearId));
      } else {
        return buildDefaultGroupFiscalYearSummary(fiscalYearId, groupId);
      }
    };
  }

  private BinaryOperator<GroupFiscalYearSummary> reduce() {
    return (original, update) -> {
      updateGroupFiscalYearSummary(original, update.getAllocated(), update.getAvailable(), update.getUnavailable());
      return original;
    };
  }

  private GroupFiscalYearSummary buildDefaultGroupFiscalYearSummary(String fiscalYearId, String groupId) {
    return new GroupFiscalYearSummary()
      .withGroupId(groupId).withFiscalYearId(fiscalYearId).withAllocated(0d).withAvailable(0d).withUnavailable(0d);
  }

  private GroupFiscalYearSummary buildGroupFiscalYearSummary(String fiscalYearId, String groupId, List<Budget> budgets) {
    GroupFiscalYearSummary summary = buildDefaultGroupFiscalYearSummary(fiscalYearId, groupId);
    for(Budget budget : budgets) {
      updateGroupFiscalYearSummary(summary, budget.getAllocated(), budget.getAvailable(), budget.getUnavailable());
    }
    return summary;
  }

  private void updateGroupFiscalYearSummary(GroupFiscalYearSummary summary, double allocated, double available, double unavailable) {
    summary.setAllocated(summary.getAllocated() + allocated);
    summary.setAvailable(summary.getAvailable() + available);
    summary.setUnavailable(summary.getUnavailable() + unavailable);
  }

  private boolean isBudgetExists(Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetMap, String fundId, String fiscalYearId) {
    return Objects.nonNull(fundIdFiscalYearIdBudgetMap.get(fundId)) && Objects.nonNull(fundIdFiscalYearIdBudgetMap.get(fundId).get(fiscalYearId));
  }

}
