package org.folio.services.group;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static org.folio.rest.util.BudgetUtils.TRANSFER_TRANSACTION_TYPES;
import static org.folio.rest.util.HelperUtils.collectResultsOnSuccess;
import static org.folio.rest.util.HelperUtils.removeInitialAllocationByFunds;
import static org.folio.rest.util.ResourcePathResolver.BUDGETS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.models.GroupFiscalYearTransactionsHolder;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.BudgetsCollection;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummary;
import org.folio.rest.jaxrs.model.GroupFiscalYearSummaryCollection;
import org.folio.rest.jaxrs.model.GroupFundFiscalYear;
import org.folio.rest.jaxrs.model.GroupFundFiscalYearCollection;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.Transaction.TransactionType;
import org.folio.rest.util.HelperUtils;
import org.folio.services.transactions.TransactionService;

import io.vertx.core.Future;

public class GroupFiscalYearTotalsService {

  private static final Logger log = LogManager.getLogger();

  private final RestClient restClient;
  private final GroupFundFiscalYearService groupFundFiscalYearService;
  private final TransactionService transactionService;

  public GroupFiscalYearTotalsService(RestClient restClient, GroupFundFiscalYearService groupFundFiscalYearService,
      TransactionService transactionService) {
    this.restClient = restClient;
    this.groupFundFiscalYearService = groupFundFiscalYearService;
    this.transactionService = transactionService;
  }

  public Future<GroupFiscalYearSummaryCollection> getGroupFiscalYearSummaries(String query, RequestContext requestContext) {
    log.debug("getGroupFiscalYearSummaries:: Getting group fiscal year summaries by query={}", query);
    var requestEntry = new RequestEntry(resourcesPath(BUDGETS_STORAGE))
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE)
      .withQuery(query);

    return restClient.get(requestEntry.buildEndpoint(), BudgetsCollection.class, requestContext)
      .compose(budgetsCollection -> groupFundFiscalYearService.getGroupFundFiscalYears(query, 0, Integer.MAX_VALUE, requestContext)
        .map(groupFundFiscalYearsCollection -> {
          Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetsMap = budgetsCollection.getBudgets()
            .stream()
            .collect(groupingBy(Budget::getFundId, groupingBy(Budget::getFiscalYearId, toList())));

          List<GroupFiscalYearSummary> summaries = groupSummariesByGroupIdAndFiscalYearId(groupFundFiscalYearsCollection, fundIdFiscalYearIdBudgetsMap)
            .values().stream()
            .flatMap(summary -> summary.values().stream())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

          GroupFiscalYearSummaryCollection collection = new GroupFiscalYearSummaryCollection()
            .withGroupFiscalYearSummaries(summaries)
            .withTotalRecords(summaries.size());

          return buildHolderSkeletons(fundIdFiscalYearIdBudgetsMap, collection, groupFundFiscalYearsCollection);
        }))
      .compose(holders -> updateHoldersWithAllocations(holders, requestContext)
        .map(v -> {
          log.debug("getGroupFiscalYearSummaries:: Updating group summary with allocation fields for '{}' holder(s)", holders.size());
          updateGroupSummaryWithAllocation(holders);
          return null;
        })
        .compose(v -> updateHoldersWithTransfers(holders, requestContext))
        .map(v -> {
          log.debug("getGroupFiscalYearSummaries:: Updating group summary with calculated fields for '{}' holder(s)", holders.size());
          updateGroupSummaryWithCalculatedFields(holders);
          return null;
        })
        .map(v -> convertHolders(holders)));
  }

  /**
   * The method follows this formula: <br>
   * <p>
   * allocated = initialAllocation + allocationTo - allocationFrom <br>
   * totalFunding = allocated + netTransfers <br>
   * available = totalFunding - (encumbered + awaitingPayment - expended - credited) <br>
   * cashBalance = totalFunding - expended + credited <br>
   * overExpended = max(expended - credited + awaitingPayment - max(totalFunding, 0), 0)
   * overCommitted = max(unavailable - max(totalFunding, 0), 0)
   * overEncumbered = overCommitted - overExpended <br>
   * </p>
   * @param holders GroupFiscalYearTransactionsHolder list
   */
  private void updateGroupSummaryWithCalculatedFields(List<GroupFiscalYearTransactionsHolder> holders) {
    holders.forEach(holder -> {
      GroupFiscalYearSummary summary = holder.getGroupFiscalYearSummary();
      double toTransfer = HelperUtils.calculateTotals(holder.getToTransfers(), Transaction::getAmount);
      double fromTransfer = HelperUtils.calculateTotals(holder.getFromTransfers(), Transaction::getAmount);
      BigDecimal netTransfers = BigDecimal.valueOf(toTransfer).subtract(BigDecimal.valueOf(fromTransfer));
      summary.withNetTransfers(netTransfers.doubleValue());

      BigDecimal initialAllocation = BigDecimal.valueOf(summary.getInitialAllocation());
      BigDecimal allocationTo = BigDecimal.valueOf(summary.getAllocationTo());
      BigDecimal allocationFrom = BigDecimal.valueOf(summary.getAllocationFrom());
      BigDecimal allocated = initialAllocation.add(allocationTo).subtract(allocationFrom);
      summary.withAllocated(allocated.doubleValue());

      BigDecimal totalFunding = allocated.add(netTransfers);
      summary.withTotalFunding(totalFunding.doubleValue());

      BigDecimal expended = BigDecimal.valueOf(summary.getExpenditures());
      BigDecimal credited = BigDecimal.valueOf(summary.getCredits());
      summary.withCashBalance(totalFunding.subtract(expended).add(credited).doubleValue());

      BigDecimal encumbered = BigDecimal.valueOf(summary.getEncumbered());
      BigDecimal awaitingPayment = BigDecimal.valueOf(summary.getAwaitingPayment());
      BigDecimal overExpended = expended.subtract(credited).add(awaitingPayment)
        .subtract(totalFunding.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
      summary.withOverExpended(overExpended.doubleValue());

      BigDecimal unavailableAmount = encumbered.add(awaitingPayment).add(expended).subtract(credited);
      BigDecimal unavailable = unavailableAmount.max(BigDecimal.ZERO);
      summary.withUnavailable(unavailable.doubleValue());
      BigDecimal available = totalFunding.subtract(unavailableAmount);
      summary.withAvailable(available.doubleValue());

      BigDecimal overCommitted = unavailable.subtract(totalFunding.max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
      BigDecimal overEncumbered = overCommitted.subtract(overExpended);
      summary.withOverEncumbrance(overEncumbered.doubleValue());

    });
  }

  private GroupFiscalYearSummaryCollection convertHolders(List<GroupFiscalYearTransactionsHolder> holders) {
    List<GroupFiscalYearSummary> summaries = holders.stream().map(GroupFiscalYearTransactionsHolder::getGroupFiscalYearSummary).collect(Collectors.toList());
    return new GroupFiscalYearSummaryCollection().withGroupFiscalYearSummaries(summaries).withTotalRecords(summaries.size());
  }

  private void updateGroupSummaryWithAllocation(List<GroupFiscalYearTransactionsHolder> holders) {
    holders.forEach(holder -> {
      holder.withToAllocations(new ArrayList<>(holder.getToAllocations()));
      removeInitialAllocationByFunds(holder.getToAllocations());
      GroupFiscalYearSummary summary = holder.getGroupFiscalYearSummary();
      summary.withAllocationTo(HelperUtils.calculateTotals(holder.getToAllocations(), Transaction::getAmount))
        .withAllocationFrom(HelperUtils.calculateTotals(holder.getFromAllocations(), Transaction::getAmount));
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
      .withCredits(0d)
      .withNetTransfers(0d)
      .withInitialAllocation(0d)
      .withTotalFunding(0d)
      .withCashBalance(0d)
      .withOverEncumbrance(0d)
      .withOverExpended(0d);
  }

  private GroupFiscalYearSummary buildGroupFiscalYearSummary(String fiscalYearId, String groupId, List<Budget> budgets) {
    GroupFiscalYearSummary summary = buildDefaultGroupFiscalYearSummary(fiscalYearId, groupId);

    summary
      .withInitialAllocation(HelperUtils.calculateTotals(budgets, Budget::getInitialAllocation))
      .withAwaitingPayment(HelperUtils.calculateTotals(budgets, Budget::getAwaitingPayment))
      .withEncumbered(HelperUtils.calculateTotals(budgets, Budget::getEncumbered))
      .withExpenditures(HelperUtils.calculateTotals(budgets, Budget::getExpenditures))
      .withCredits(HelperUtils.calculateTotals(budgets, Budget::getCredits));
    return summary;
  }

  private List<GroupFiscalYearTransactionsHolder> buildHolderSkeletons(Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetsMap,
                                                                       GroupFiscalYearSummaryCollection groupFiscalYearSummaryCollection,
                                                                       GroupFundFiscalYearCollection groupFundFiscalYearCollection) {
    log.debug("buildHolderSkeletons:: Building holder skeletons");
    List<GroupFiscalYearTransactionsHolder> holders = new ArrayList<>();
    if (groupFiscalYearSummaryCollection != null && groupFundFiscalYearCollection != null
      && CollectionUtils.isNotEmpty(groupFiscalYearSummaryCollection.getGroupFiscalYearSummaries())
      && CollectionUtils.isNotEmpty(groupFundFiscalYearCollection.getGroupFundFiscalYears())) {

      groupFiscalYearSummaryCollection.getGroupFiscalYearSummaries().forEach(groupFiscalYearSummary -> {
        String fiscalYearId = groupFiscalYearSummary.getFiscalYearId();
        String groupId = groupFiscalYearSummary.getGroupId();
        List<String> groupFundIds = groupFundFiscalYearCollection.getGroupFundFiscalYears().stream()
          .filter(groupFundFiscalYear -> groupFundFiscalYear.getGroupId().equals(groupId)
            && groupFundFiscalYear.getFiscalYearId().equals(fiscalYearId))
          .map(GroupFundFiscalYear::getFundId)
          .filter(fundId -> isBudgetExists(fundIdFiscalYearIdBudgetsMap, fundId, fiscalYearId))
          .collect(Collectors.toList());
        log.info("buildHolderSkeletons:: Adding groupFiscalYearTransactionHolder  to holders with '{}' groupFundId(s)", groupFundIds.size());
        holders.add(new GroupFiscalYearTransactionsHolder(groupFiscalYearSummary).withGroupFundIds(groupFundIds));
      });
    }
    return holders;
  }

  private Future<Void> updateHoldersWithAllocations(List<GroupFiscalYearTransactionsHolder> holders,
                                                    RequestContext requestContext) {
    List<Future<GroupFiscalYearTransactionsHolder>> futures = new ArrayList<>();
    holders.forEach(holder ->
      futures.add(updateHolderWithAllocations(requestContext, holder))
    );
    return collectResultsOnSuccess(futures)
      .onSuccess(result -> log.debug("updateHoldersWithAllocations:: Number of holders updated with allocations: {}", result.size()))
      .mapEmpty();
  }

  private Future<Void> updateHoldersWithTransfers(List<GroupFiscalYearTransactionsHolder> holders,
                                                  RequestContext requestContext) {
    List<Future<GroupFiscalYearTransactionsHolder>> futures = new ArrayList<>();
    holders.forEach(holder -> futures.add(updateHolderWithTransfers(requestContext, holder)));
    return collectResultsOnSuccess(futures)
      .onSuccess(result -> log.debug("updateHoldersWithTransfers:: Number of holders updated with transfers: {}", result.size()))
      .mapEmpty();
  }

  private Future<GroupFiscalYearTransactionsHolder> updateHolderWithAllocations(RequestContext requestContext, GroupFiscalYearTransactionsHolder holder) {
    List<String> groupFundIds = holder.getGroupFundIds();
    String fiscalYearId = holder.getGroupFiscalYearSummary().getFiscalYearId();
    var fromAllocations = transactionService.getTransactionsFromFunds(groupFundIds, fiscalYearId, List.of(TransactionType.ALLOCATION), requestContext);
    var toAllocations = transactionService.getTransactionsToFunds(groupFundIds, fiscalYearId, List.of(TransactionType.ALLOCATION), requestContext);

    return GenericCompositeFuture.join(List.of(fromAllocations, toAllocations))
      .map(cf -> holder.withToAllocations(toAllocations.result()).withFromAllocations(fromAllocations.result()));
  }

  private Future<GroupFiscalYearTransactionsHolder> updateHolderWithTransfers(RequestContext requestContext, GroupFiscalYearTransactionsHolder holder) {
    List<String> groupFundIds = holder.getGroupFundIds();
    String fiscalYearId = holder.getGroupFiscalYearSummary().getFiscalYearId();

    var fromTransfers = transactionService.getTransactionsFromFunds(groupFundIds, fiscalYearId, TRANSFER_TRANSACTION_TYPES, requestContext);
    var toTransfers = transactionService.getTransactionsToFunds(groupFundIds, fiscalYearId, TRANSFER_TRANSACTION_TYPES, requestContext);
    return GenericCompositeFuture.join(List.of(fromTransfers, toTransfers))
      .map(cf -> holder.withToTransfers(toTransfers.result()).withFromTransfers(fromTransfers.result()));
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
    original.setCredits(BigDecimal.valueOf(original.getCredits())
      .add(BigDecimal.valueOf(update.getCredits()))
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

  private boolean isBudgetExists(Map<String, Map<String, List<Budget>>> fundIdFiscalYearIdBudgetMap, String fundId, String fiscalYearId) {
    return Objects.nonNull(fundIdFiscalYearIdBudgetMap.get(fundId)) && Objects.nonNull(fundIdFiscalYearIdBudgetMap.get(fundId)
      .get(fiscalYearId));
  }
}
