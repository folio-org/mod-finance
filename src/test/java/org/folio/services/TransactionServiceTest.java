package org.folio.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.dao.TransactionDAO;
import org.folio.rest.jaxrs.model.Budget;
import org.folio.rest.jaxrs.model.Transaction;
import org.folio.rest.jaxrs.model.TransactionCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.impl.EventLoopContext;

public class TransactionServiceTest {

  @InjectMocks
  private TransactionService transactionService;

  @Mock
  private TransactionDAO transactionDAOMock;

  @Mock
  private Map<String, String> okapiHeadersMock;

  @Mock
  private EventLoopContext ctxMock;

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

    when(transactionDAOMock.get(anyString(), anyInt(), anyInt(), eq(ctxMock), eq(okapiHeadersMock)))
      .thenReturn(CompletableFuture.completedFuture(transactionCollection));

    CompletableFuture<List<Transaction>> result = transactionService.getTransactions(budget, ctxMock, okapiHeadersMock);

    String expectedQuery = String.format("fromFundId==%s AND fiscalYearId==%s", fundId, fiscalYearId);
    verify(transactionDAOMock)
      .get(eq(expectedQuery), eq(0), eq(Integer.MAX_VALUE), eq(ctxMock), eq(okapiHeadersMock));

    List<Transaction> resultTransactions = result.join();
    assertEquals(transactions, resultTransactions);

  }
}
