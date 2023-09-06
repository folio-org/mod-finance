package org.folio.services.ledger;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class LedgerServiceTest {

  @InjectMocks
  private LedgerService ledgerService;
  @Mock
  private RestClient restClient;
  @Mock
  private LedgerTotalsService ledgerTotalsMockService;
  @Mock
  private AcqUnitsService acqUnitsService;

  private RequestContext requestContextMock;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContextMock = new RequestContext(context, okapiHeaders);
  }

  @Test
  void shouldCallRestClientPostWhenCalledCreateLedger(VertxTestContext vertxTestContext) {
    Ledger ledger = new Ledger()
      .withId(UUID.randomUUID().toString());

    when(restClient.post(anyString(), any(), any(), any())).thenReturn(succeededFuture(ledger));

    var future = ledgerService.createLedger(ledger, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var resultLedger = result.result();
        assertEquals(ledger, resultLedger);

        verify(restClient).post(anyString(), eq(ledger), eq(Ledger.class), eq(requestContextMock));
        vertxTestContext.completeNow();
      });


  }

  @Test
  void testShouldRetrieveFundsWithAcqUnits(VertxTestContext vertxTestContext) {
    String ledgerId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId);
    LedgersCollection ledgersCollection = new LedgersCollection().withLedgers(List.of(ledger)).withTotalRecords(1);

    doReturn(succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL)).when(acqUnitsService).buildAcqUnitsCqlClause(requestContextMock);
    doReturn(succeededFuture(ledgersCollection)).when(restClient).get(anyString(), eq(LedgersCollection.class), eq(requestContextMock));

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(ledgersCollection));
    when(ledgerTotalsMockService.populateLedgersTotals(any(), anyString(), any())).thenReturn(succeededFuture(ledgersCollection));

    var future = ledgerService.retrieveLedgersWithAcqUnitsRestrictionAndTotals(StringUtils.EMPTY, 0,10, fiscalYearId, requestContextMock);
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());

          var actLedgers = result.result();
          assertThat(ledgersCollection, equalTo(actLedgers));
          verify(ledgerTotalsMockService).populateLedgersTotals(eq(ledgersCollection), eq(fiscalYearId), eq(requestContextMock));
          vertxTestContext.completeNow();
        });
  }

  @Test
  void shouldCallPopulateLedgersTotalsWhenCallRetrieveLedgersWithTotalsWithFiscalYearParameter(VertxTestContext vertxTestContext) {

    String query = "name==Test name";
    int offset = 1;
    int limit = 100;
    String fiscalYearId = UUID.randomUUID().toString();

    Ledger ledger1 = new Ledger().withId(UUID.randomUUID().toString());
    Ledger ledger2 = new Ledger().withId(UUID.randomUUID().toString());
    Ledger ledger3 = new Ledger().withId(UUID.randomUUID().toString());
    List<Ledger> ledgers = Arrays.asList(ledger1, ledger2, ledger3);
    LedgersCollection ledgersCollection = new LedgersCollection()
      .withLedgers(ledgers)
      .withTotalRecords(3);

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(ledgersCollection));
    when(ledgerTotalsMockService.populateLedgersTotals(any(), anyString(), any())).thenReturn(succeededFuture(ledgersCollection));

    var future = ledgerService.retrieveLedgersWithTotals(query, offset, limit, fiscalYearId,requestContextMock);
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());

          verify(restClient).get(assertQueryContains(query), eq(LedgersCollection.class), eq(requestContextMock));
          verify(ledgerTotalsMockService).populateLedgersTotals(eq(ledgersCollection), eq(fiscalYearId), eq(requestContextMock));
          vertxTestContext.completeNow();
        });

  }

  @Test
  void shouldNotCallPopulateLedgersTotalsWhenCallRetrieveLedgersWithTotalsEmptyFiscalYearParameter() {

    String query = "name==Test name";
    int offset = 1;
    int limit = 100;
    String fiscalYearId = null;

    Ledger ledger1 = new Ledger().withId(UUID.randomUUID().toString());
    Ledger ledger2 = new Ledger().withId(UUID.randomUUID().toString());
    Ledger ledger3 = new Ledger().withId(UUID.randomUUID().toString());
    List<Ledger> ledgers = Arrays.asList(ledger1, ledger2, ledger3);
    LedgersCollection ledgersCollection = new LedgersCollection()
      .withLedgers(ledgers)
      .withTotalRecords(3);

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(ledgersCollection));

    ledgerService.retrieveLedgersWithTotals(query, offset, limit, fiscalYearId, requestContextMock);

    verify(restClient).get(assertQueryContains(query),eq(LedgersCollection.class), eq(requestContextMock));
    verify(ledgerTotalsMockService, never()).populateLedgersTotals(any(), any(), any());
  }

  @Test
  void shouldCallPopulateLedgerTotalsWhenCallRetrieveLedgerWithTotalsWithFiscalYearParameter(VertxTestContext vertxTestContext) {

    String fiscalYearId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(UUID.randomUUID().toString());

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(ledger));
    when(ledgerTotalsMockService.populateLedgerTotals(any(), anyString(), any())).thenReturn(succeededFuture(ledger));


    var future = ledgerService.retrieveLedgerWithTotals(ledger.getId(), fiscalYearId, requestContextMock);
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());
          verify(restClient).get(assertQueryContains(ledger.getId()), eq(Ledger.class), eq(requestContextMock));
          verify(ledgerTotalsMockService).populateLedgerTotals(eq(ledger), eq(fiscalYearId), eq(requestContextMock));
          vertxTestContext.completeNow();
        });

  }

  @Test
  void shouldNotCallPopulateLedgerTotalsWhenCallRetrieveLedgerWithTotalsWithEmptyFiscalYearParameter(VertxTestContext vertxTestContext) {

    String fiscalYearId = null;

    Ledger ledger = new Ledger().withId(UUID.randomUUID().toString());

    when(restClient.get(anyString(), any(), any())).thenReturn(succeededFuture(ledger));

    var future = ledgerService.retrieveLedgerWithTotals(ledger.getId(), fiscalYearId, requestContextMock);

    vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());

          verify(restClient).get(assertQueryContains(ledger.getId()), eq(Ledger.class), eq(requestContextMock));
          verify(ledgerTotalsMockService, never()).populateLedgerTotals(any(), anyString(), any());
          vertxTestContext.completeNow();
        });
  }

  @Test
  void shouldCallRestClientPutWhenCallUpdateLedger(VertxTestContext vertxTestContext) {
    Ledger ledger = new Ledger().withId(UUID.randomUUID().toString());
    when(restClient.put(anyString(), any(), any())).thenReturn(succeededFuture(null));

    var future = ledgerService.updateLedger(ledger, requestContextMock);
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());
          verify(restClient).put(assertQueryContains(ledger.getId()), eq(ledger), eq(requestContextMock));

          vertxTestContext.completeNow();
        });

  }

  @Test
  void shouldCallRestClientDeleteWhenCallDeleteLedger(VertxTestContext vertxTestContext) {
    String ledgerId = UUID.randomUUID().toString();
    when(restClient.delete(anyString(), any())).thenReturn(succeededFuture(null));

    var future = ledgerService.deleteLedger(ledgerId, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {

        verify(restClient).delete(contains(ledgerId), eq(requestContextMock));
        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetLedgers(VertxTestContext vertxTestContext) {
    //Given
    LedgersCollection ledgersCollection = new LedgersCollection();
    Ledger ledger1 = new Ledger().withId("5");
    Ledger ledger2 = new Ledger().withId("7");
    List<String> ids = Arrays.asList("5", "7");
    List<Ledger> ledgerList = new ArrayList<>();
    ledgerList.add(ledger1);
    ledgerList.add(ledger2);
    ledgersCollection.setLedgers(ledgerList);
    //When
    when(restClient.get(anyString(), eq(LedgersCollection.class), any())).thenReturn(succeededFuture(ledgersCollection));
    var future = ledgerService.getLedgers(ids, requestContextMock);
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertTrue(result.succeeded());

        var ledgers = result.result();
        assertEquals(ledgersCollection.getLedgers().get(0).getId(), ledgers.get(0).getId());
         assertEquals(ledgersCollection.getLedgers().get(1).getId(), ledgers.get(1).getId());

         vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetLedgersByIds(VertxTestContext vertxTestContext) {
    //Given
    LedgersCollection ledgersCollection = new LedgersCollection();
    Ledger ledger1 = new Ledger().withId("5");
    Ledger ledger2 = new Ledger().withId("7");
    List<String> ids = Arrays.asList("5", "7");
    List<Ledger> ledgerList = new ArrayList<>();
    ledgerList.add(ledger1);
    ledgerList.add(ledger2);
    ledgersCollection.setLedgers(ledgerList);
    //When
    when(restClient.get(anyString(), eq(LedgersCollection.class), any())).thenReturn(succeededFuture(ledgersCollection));


    var future = ledgerService.getLedgersByIds(ids, requestContextMock);
      vertxTestContext.assertComplete(future)
        .onComplete(result -> {
          assertTrue(result.succeeded());

          var ledgers = result.result();
          assertEquals(ledgersCollection.getLedgers().get(0).getId(), ledgers.get(0).getId());
          assertEquals(ledgersCollection.getLedgers().get(1).getId(), ledgers.get(1).getId());

          vertxTestContext.completeNow();
        });


  }
}
