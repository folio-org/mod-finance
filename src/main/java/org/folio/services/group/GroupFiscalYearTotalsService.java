package org.folio.services.group;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.convertIdsToCqlQuery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.models.GroupFiscalYearTransactionsHolder;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummaryCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.folio.rest.util.HelperUtils;
import org.folio.services.GroupFundFiscalYearService;
import org.folio.services.transactions.TransactionService;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GroupFiscalYearTotalsService {
  private static final Logger LOG = LoggerFactory.getLogger(GroupFiscalYearTotalsService.class);
  public static int MAX_FUND_PER_QUERY = 5;
  public static final String ALLOCATION_TRANSACTION_TO_QUERY = "(fiscalYearId==%s AND transactionType==Allocation) AND (%s) AND (%s)";
  public static final String ALLOCATION_TRANSACTION_FROM_QUERY = "(fiscalYearId==%s AND transactionType==Allocation) AND (%s) AND (%s)";
  private final RestClient budgetRestClient;
  private final GroupFundFiscalYearService groupFundFiscalYearService;
  private final TransactionService transactionService;

  public GroupFiscalYearTotalsService(RestClient budgetRestClient, GroupFundFiscalYearService groupFundFiscalYearService,
                                      TransactionService transactionService) {
    this.budgetRestClient = budgetRestClient;
    this.groupFundFiscalYearService = groupFundFiscalYearService;
    this.transactionService = transactionService;
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

            GroupFiscalYearSummaryCollection collection = new GroupFiscalYearSummaryCollection().withGroupFiscalYearSummaries(summaries)
              .withTotalRecords(summaries.size());
            return buildHolderSkeletons(collection, groupFundFiscalYearCollection);
          })
      .thenCompose(holders -> updateHoldersWithAllocation(holders, requestContext)
                              .thenApply(holder -> updateGroupSummaryWithAllocations(holders)));
  }

  private GroupFiscalYearSummaryCollection updateGroupSummaryWithAllocations(List<GroupFiscalYearTransactionsHolder> holders) {
    holders.forEach(holder -> {
      removeInitialAllocationByFunds(holder);
      GroupFiscalYearSummary summary = holder.getGroupFiscalYearSummary();
      summary.withAllocationTo(HelperUtils.calculateTotals(holder.getToTransactions(), Transaction::getAmount))
             .withAllocationFrom(HelperUtils.calculateTotals(holder.getFromTransactions(), Transaction::getAmount));
    });
    List<GroupFiscalYearSummary> summaries = holders.stream().map(GroupFiscalYearTransactionsHolder::getGroupFiscalYearSummary).collect(toList());
    return  new GroupFiscalYearSummaryCollection().withGroupFiscalYearSummaries(summaries).withTotalRecords(summaries.size());
  }

  private void removeInitialAllocationByFunds(GroupFiscalYearTransactionsHolder holder) {
    Map<String, List<Transaction>> fundToTransactions = holder.getToTransactions().stream().collect(groupingBy(Transaction::getToFundId));
    fundToTransactions.forEach((fundToId, transactions) -> {
       transactions.sort(Comparator.comparing(tr -> tr.getMetadata().getCreatedDate()));
      if (CollectionUtils.isNotEmpty(transactions)) {
        transactions.remove(0);
      }
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

  private Function<GroupFundFiscalYear, GroupFiscalYearSummary> map(Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetMap) {
    return groupFundFiscalYear -> {
      String fundId = groupFundFiscalYear.getFundId();
      String fiscalYearId = groupFundFiscalYear.getFiscalYearId();
      String groupId = groupFundFiscalYear.getGroupId();
      if (isBudgetExists(fundIdFiscalYearIdBudgetMap, fundId, fiscalYearId)) {
        return buildGroupFiscalYearSummary(fiscalYearId, groupId, fundIdFiscalYearIdBudgetMap.get(fundId).get(fiscalYearId));
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

    summary.withAllocated(HelperUtils.calculateTotals(budgets, Budget::getAllocated))
      .withAvailable(HelperUtils.calculateTotals(budgets, Budget::getAvailable))
      .withUnavailable(HelperUtils.calculateTotals(budgets, Budget::getUnavailable))
      .withNetTransfers(HelperUtils.calculateTotals(budgets, Budget::getNetTransfers))
      .withInitialAllocation(HelperUtils.calculateTotals(budgets, Budget::getInitialAllocation))
      .withAwaitingPayment(HelperUtils.calculateTotals(budgets, Budget::getAwaitingPayment))
      .withEncumbered(HelperUtils.calculateTotals(budgets, Budget::getEncumbered))
      .withExpenditures(HelperUtils.calculateTotals(budgets, Budget::getExpenditures))
      .withOverEncumbrance(HelperUtils.calculateTotals(budgets, Budget::getOverEncumbrance))
      .withOverExpended(HelperUtils.calculateTotals(budgets, Budget::getOverExpended))
      .withTotalFunding(HelperUtils.calculateTotals(budgets, Budget::getTotalFunding))
      .withCashBalance(HelperUtils.calculateTotals(budgets, Budget::getCashBalance));
    return summary;
  }

  private List<GroupFiscalYearTransactionsHolder> buildHolderSkeletons(GroupFiscalYearSummaryCollection groupFiscalYearSummaryCollection,
                                                                        GroupFundFiscalYearCollection groupFundFiscalYearCollection) {
    List<GroupFiscalYearTransactionsHolder> holders = new ArrayList<>();
    if (groupFiscalYearSummaryCollection != null
      && groupFundFiscalYearCollection != null
      && (!CollectionUtils.isEmpty(groupFiscalYearSummaryCollection.getGroupFiscalYearSummaries()))
      && (!CollectionUtils.isEmpty(groupFundFiscalYearCollection.getGroupFundFiscalYears())))
    {
      groupFiscalYearSummaryCollection.getGroupFiscalYearSummaries().forEach(groupFiscalYearSummary -> {
        String fiscalYearId = groupFiscalYearSummary.getFiscalYearId();
        String groupId = groupFiscalYearSummary.getGroupId();
        List<String> groupFundIds = groupFundFiscalYearCollection.getGroupFundFiscalYears().stream()
          .filter(groupFundFiscalYear -> groupFundFiscalYear.getGroupId().equals(groupId)
                                         && groupFundFiscalYear.getFiscalYearId().equals(fiscalYearId))
          .map(GroupFundFiscalYear::getFundId)
          .collect(Collectors.toList());
        holders.add( new GroupFiscalYearTransactionsHolder(groupFiscalYearSummary).withGroupFundIds(groupFundIds));
      });
    }
    return holders;
  }

  private CompletableFuture<Void> updateHoldersWithAllocation(List<GroupFiscalYearTransactionsHolder> holders,
                                                              RequestContext requestContext) {
    List<CompletableFuture<GroupFiscalYearTransactionsHolder>> futures = new ArrayList<>();
    holders.forEach(holder -> {
       List<String> groupFundIds = holder.getGroupFundIds();
       String fiscalYearId = holder.getGroupFiscalYearSummary().getFiscalYearId();
       futures.add(getToTransactionsByFundIds(groupFundIds, fiscalYearId, requestContext)
                       .thenCombine(getFromTransactionsByFundIds(groupFundIds, fiscalYearId, requestContext), (toTransactions, fromTransactions) -> {
                                 return holder.withToTransactions(toTransactions).withFromTransactions(fromTransactions);
                        }));
    });
    return collectResultsOnSuccess(futures).thenAccept(result -> LOG.debug("Number of holders updated : " + result.size()));
  }

  private CompletableFuture<List<Transaction>> retrieveToTransactions(List<String> groupFundIds, String fiscalYearId, RequestContext requestContext) {
    String toFundQuery = convertIdsToCqlQuery(groupFundIds, "toFundId", "==", " OR ");
    String fromFundQuery = convertIdsToCqlQuery(groupFundIds, "fromFundId", "<>", " AND ");
    String query = String.format(ALLOCATION_TRANSACTION_TO_QUERY, fiscalYearId, toFundQuery, fromFundQuery);
    return transactionService.retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
                             .thenApply(trCollection -> trCollection.getTransactions().stream().skip(1).collect(Collectors.toList()));
  }

  private CompletableFuture<List<Transaction>> retrieveFromTransactions(List<String> groupFundIds, String fiscalYearId, RequestContext requestContext) {
    String fromFundQuery = convertIdsToCqlQuery(groupFundIds, "fromFundId", "==", " OR ");
    String toFundQuery = convertIdsToCqlQuery(groupFundIds, "toFundId", "<>", " AND ");
    String query = String.format(ALLOCATION_TRANSACTION_FROM_QUERY, fiscalYearId, toFundQuery, fromFundQuery);
    return transactionService.retrieveTransactions(query, 0, Integer.MAX_VALUE, requestContext)
      .thenApply(TransactionCollection::getTransactions);
  }

  private CompletableFuture<List<Transaction>> getFromTransactionsByFundIds(List<String> groupFundIds, String fiscalYearId, RequestContext requestContext) {
    return collectResultsOnSuccess(
      ofSubLists(new ArrayList<>(groupFundIds), MAX_FUND_PER_QUERY).map(ids -> retrieveFromTransactions(groupFundIds, fiscalYearId, requestContext))
        .toList()).thenApply(lists -> lists.stream().flatMap(Collection::stream).collect(Collectors.toList()));
  }

  private CompletableFuture<List<Transaction>> getToTransactionsByFundIds(List<String> groupFundIds, String fiscalYearId, RequestContext requestContext) {
    return collectResultsOnSuccess(
      ofSubLists(new ArrayList<>(groupFundIds), MAX_FUND_PER_QUERY).map(ids -> retrieveToTransactions(groupFundIds, fiscalYearId, requestContext))
        .toList()).thenApply(lists -> lists.stream().flatMap(Collection::stream).collect(Collectors.toList()));
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
