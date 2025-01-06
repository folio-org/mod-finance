package org.folio.services.transactions;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.TransactionTotal;
import org.folio.rest.jaxrs.model.TransactionTotalCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.BudgetUtils.TRANSFER_TRANSACTION_TOTAL_TYPES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
public class TransactionTotalApiServiceTest {

  @InjectMocks
  private TransactionTotalService transactionTotalService;

  @Mock(name = "restClient")
  private RestClient restClient;

  @Mock
  private RequestContext requestContext;

  private AutoCloseable mockitoMocks;

  @BeforeEach
  void setUp() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockitoMocks.close();
  }

  @Test
  void getTransactionsToFunds(VertxTestContext vertxTestContext) {
    var fiscalYearId = UUID.randomUUID().toString();

    var fundIds = Stream.generate(() -> UUID.randomUUID().toString())
      .limit(50)
      .collect(Collectors.toList());

    var transactionTotals = List.of(new TransactionTotal()
      .withFiscalYearId(fiscalYearId)
      .withToFundId(fundIds.get(0))
      .withTransactionType(TransactionTotal.TransactionType.ALLOCATION)
      .withAmount(1000.0));

    var transactionCollection = new TransactionTotalCollection()
      .withTransactionTotals(transactionTotals)
      .withTotalRecords(1);

    when(restClient.get(anyString(), eq(TransactionTotalCollection.class), any()))
      .thenReturn(succeededFuture(transactionCollection))
      .thenReturn(succeededFuture(new TransactionTotalCollection()));

    var future = transactionTotalService.getTransactionsToFunds(fundIds, fiscalYearId, List.of(TransactionTotal.TransactionType.ALLOCATION), requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertThat(result.result(), hasSize(1));
        verify(restClient, times(10)).get(anyString(), any(), any());

        vertxTestContext.completeNow();
      });
  }

  @Test
  void getTransactionsFromFunds(VertxTestContext vertxTestContext) {
    var fiscalYearId = UUID.randomUUID().toString();

    var fundIds = Stream.generate(() -> UUID.randomUUID().toString())
      .limit(50)
      .collect(Collectors.toList());

    var transactionTotals = List.of(new TransactionTotal()
      .withFiscalYearId(fiscalYearId)
      .withFromFundId(fundIds.get(0))
      .withTransactionType(TransactionTotal.TransactionType.TRANSFER)
      .withAmount(100.0));

    var transactionCollection = new TransactionTotalCollection()
      .withTransactionTotals(transactionTotals)
      .withTotalRecords(1);

    when(restClient.get(anyString(), eq(TransactionTotalCollection.class), any()))
      .thenReturn(succeededFuture(transactionCollection))
      .thenReturn(succeededFuture(new TransactionTotalCollection()));

    var future = transactionTotalService.getTransactionsFromFunds(fundIds, fiscalYearId, TRANSFER_TRANSACTION_TOTAL_TYPES, requestContext);

    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertThat(result.result(), hasSize(1));
        verify(restClient, times(10)).get(anyString(), any(), any());

        vertxTestContext.completeNow();
      });
  }
}
