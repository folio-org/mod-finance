package org.folio.services.transactions;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.AwaitingPayment;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Batch;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.folio.services.fiscalyear.FiscalYearService;
import org.folio.services.fund.FundService;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.folio.rest.jaxrs.model.Transaction.TransactionType.PENDING_PAYMENT;
import static org.folio.rest.util.ResourcePathResolver.BATCH_TRANSACTIONS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionApiServiceTest {
  private TransactionApiService transactionApiService;
  private AutoCloseable mockitoMocks;

  @Mock
  private RestClient restClient;
  @Mock
  private RequestContext requestContext;
  @Mock
  private FiscalYearService fiscalYearService;
  @Mock
  private AcqUnitsService acqUnitsService;

  @BeforeEach
  void init() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
    FundService fundService = new FundService(restClient, acqUnitsService);
    TransactionService transactionService = new TransactionService(restClient, fiscalYearService);
    transactionApiService = new TransactionApiService(transactionService, fundService);
  }

  @AfterEach
  public void afterEach() throws Exception {
    mockitoMocks.close();
  }

  @Test
  void testRemovingEncumbranceWithPendingPaymentUpdate() {
    String encumbranceId = UUID.randomUUID().toString();
    String pendingPaymentId = UUID.randomUUID().toString();
    String invoiceId = UUID.randomUUID().toString();
    String invoiceLineId = UUID.randomUUID().toString();
    String fundId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    Transaction existingPendingPayment = new Transaction()
      .withId(pendingPaymentId)
      .withTransactionType(PENDING_PAYMENT)
      .withSourceInvoiceId(invoiceId)
      .withSourceInvoiceLineId(invoiceLineId)
      .withFromFundId(fundId)
      .withFiscalYearId(fiscalYearId)
      .withCurrency("USD")
      .withInvoiceCancelled(true)
      .withAwaitingPayment(new AwaitingPayment()
        .withEncumbranceId(encumbranceId));
    TransactionCollection existingPendingPaymentCollection = new TransactionCollection()
      .withTransactions(List.of(existingPendingPayment))
      .withTotalRecords(1);
    Transaction newPendingPayment = JsonObject.mapFrom(existingPendingPayment).mapTo(Transaction.class);
    newPendingPayment.getAwaitingPayment().setEncumbranceId(null);
    Batch batch = new Batch()
      .withIdsOfTransactionsToDelete(List.of(encumbranceId))
      .withTransactionsToUpdate(List.of(newPendingPayment));

    String expectedQuery = URLEncoder.encode("awaitingPayment.encumbranceId==(" + encumbranceId + ")", StandardCharsets.UTF_8);
    when(restClient.get(contains(expectedQuery), any(), eq(requestContext)))
      .thenReturn(Future.succeededFuture(existingPendingPaymentCollection));
    when(restClient.postEmptyResponse(eq(resourcesPath(BATCH_TRANSACTIONS_STORAGE)), any(Batch.class), eq(requestContext)))
      .thenReturn(Future.succeededFuture());

    Future<Void> result = transactionApiService.processBatch(batch, requestContext);
    assertTrue(result.succeeded());
  }
}
