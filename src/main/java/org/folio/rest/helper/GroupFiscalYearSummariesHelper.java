package org.folio.rest.helper;

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
import org.folio.services.GroupFundFiscalYearService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Vertx;


public class GroupFiscalYearSummariesHelper extends AbstractHelper {

  @Autowired
  private RestClient budgetRestClient;
  @Autowired
  private GroupFundFiscalYearService groupFundFiscalYearService;

  public GroupFiscalYearSummariesHelper(Map<String, String> okapiHeaders, Context ctx, String lang) {
    super(okapiHeaders, ctx, lang);
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  public CompletableFuture<GroupFiscalYearSummaryCollection> getGroupFiscalYearSummaries(String query) {
    return budgetRestClient.get(query, 0, Integer.MAX_VALUE, new RequestContext(ctx, okapiHeaders), BudgetsCollection.class)
      .thenCombine(groupFundFiscalYearService.getGroupFundFiscalYears(query, 0, Integer.MAX_VALUE, new RequestContext(ctx, okapiHeaders)), (budgetsCollection, groupFundFiscalYearCollection) -> {

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
    summary.setAllocated(BigDecimal.valueOf(summary.getAllocated()).add(BigDecimal.valueOf(allocated)).doubleValue());
    summary.setAvailable(BigDecimal.valueOf(summary.getAvailable()).add(BigDecimal.valueOf(available)).doubleValue());
    summary.setUnavailable(BigDecimal.valueOf(summary.getUnavailable()).add(BigDecimal.valueOf(unavailable)).doubleValue());
  }

  private boolean isBudgetExists(Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetMap, String fundId, String fiscalYearId) {
    return Objects.nonNull(fundIdFiscalYearIdBudgetMap.get(fundId)) && Objects.nonNull(fundIdFiscalYearIdBudgetMap.get(fundId).get(fiscalYearId));
  }

}
