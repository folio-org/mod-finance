package org.folio.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TransactionServiceTest {

  @InjectMocks
  private TransactionService transactionService;

  @Mock
  private RestClient transactionRestClientMock;

  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void getTransactions() {
    String fundId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    Budget budget = new Budget().withFundId(fundId).withFiscalYearId(fiscalYearId);

    List<Transaction> transactions = Collections.singletonList(new Transaction().withId(UUID.randomUUID().toString()));
    TransactionCollection transactionCollection = new TransactionCollection().withTransactions(transactions).withTotalRecords(1);

    when(transactionRestClientMock.get(anyString(), anyInt(), anyInt(), eq(requestContext), any()))
      .thenReturn(CompletableFuture.completedFuture(transactionCollection));

    CompletableFuture<List<Transaction>> result = transactionService.getTransactions(budget, requestContext);

    String expectedQuery = String.format("fromFundId==%s AND fiscalYearId==%s", fundId, fiscalYearId);
    verify(transactionRestClientMock)
      .get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(requestContext), eq(TransactionCollection.class));

    List<Transaction> resultTransactions = result.join();
    assertEquals(transactions, resultTransactions);

  }
}
