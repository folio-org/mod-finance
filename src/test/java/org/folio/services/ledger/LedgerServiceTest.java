package org.folio.services.ledger;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Ledger;
import org.folio.rest.jaxrs.model.LedgersCollection;
import org.folio.services.protection.AcqUnitsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestConstants.OKAPI_URL;
import static org.folio.rest.util.TestConfig.mockPort;
import static org.folio.rest.util.TestConstants.X_OKAPI_TENANT;
import static org.folio.rest.util.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.util.TestConstants.X_OKAPI_USER_ID;
import static org.folio.services.protection.AcqUnitConstants.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LedgerServiceTest {

  @InjectMocks
  private LedgerService ledgerService;
  @Mock
  private RestClient ledgerStorageRestClientMock;
  @Mock
  private LedgerTotalsService ledgerTotalsMockService;
  @Mock
  private AcqUnitsService acqUnitsService;

  private RequestContext requestContextMock;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    Context context = Vertx.vertx().getOrCreateContext();
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + mockPort);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContextMock = new RequestContext(context, okapiHeaders);
  }

  @Test
  void shouldCallRestClientPostWhenCalledCreateLedger() {
    Ledger ledger = new Ledger()
      .withId(UUID.randomUUID().toString());

    when(ledgerStorageRestClientMock.post(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(ledger));

    CompletableFuture<Ledger> ledgerFuture = ledgerService.createLedger(ledger, requestContextMock);

    Ledger resultLedger = ledgerFuture.join();

    assertEquals(ledger, resultLedger);
    verify(ledgerStorageRestClientMock).post(eq(ledger), eq(requestContextMock), eq(Ledger.class));
  }

  @Test
  void testShouldRetrieveFundsWithAcqUnits() {
    String ledgerId = UUID.randomUUID().toString();
    String fiscalYearId = UUID.randomUUID().toString();
    Ledger ledger = new Ledger().withId(ledgerId);
    LedgersCollection ledgersCollection = new LedgersCollection().withLedgers(List.of(ledger)).withTotalRecords(1);
    doReturn(completedFuture(NO_ACQ_UNIT_ASSIGNED_CQL)).when(acqUnitsService).buildAcqUnitsCqlClause(requestContextMock);
    doReturn(completedFuture(ledgersCollection)).when(ledgerStorageRestClientMock).get(NO_ACQ_UNIT_ASSIGNED_CQL, 0, 10, requestContextMock, LedgersCollection.class);

    when(ledgerStorageRestClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(ledgersCollection));
    when(ledgerTotalsMockService.populateLedgersTotals(any(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(ledgersCollection));

    LedgersCollection actLedgers = ledgerService.retrieveLedgersWithAcqUnitsRestrictionAndTotals(StringUtils.EMPTY, 0,10, fiscalYearId, requestContextMock).join();

    assertThat(ledgersCollection, equalTo(actLedgers));
    verify(ledgerTotalsMockService).populateLedgersTotals(eq(ledgersCollection), eq(fiscalYearId), eq(requestContextMock));
  }

  @Test
  void shouldCallPopulateLedgersTotalsWhenCallRetrieveLedgersWithTotalsWithFiscalYearParameter() {

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

    when(ledgerStorageRestClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(ledgersCollection));
    when(ledgerTotalsMockService.populateLedgersTotals(any(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(ledgersCollection));

    ledgerService.retrieveLedgersWithTotals(query, offset, limit, fiscalYearId,requestContextMock).join();

    verify(ledgerStorageRestClientMock).get(eq(query), eq(offset), eq(limit), eq(requestContextMock), eq(LedgersCollection.class));
    verify(ledgerTotalsMockService).populateLedgersTotals(eq(ledgersCollection), eq(fiscalYearId), eq(requestContextMock));
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

    when(ledgerStorageRestClientMock.get(anyString(), anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture(ledgersCollection));

    ledgerService.retrieveLedgersWithTotals(query, offset, limit, fiscalYearId, requestContextMock).join();

    verify(ledgerStorageRestClientMock).get(eq(query), eq(offset), eq(limit), eq(requestContextMock), eq(LedgersCollection.class));
    verify(ledgerTotalsMockService, never()).populateLedgersTotals(any(), any(), any());
  }

  @Test
  void shouldCallPopulateLedgerTotalsWhenCallRetrieveLedgerWithTotalsWithFiscalYearParameter() {

    String fiscalYearId = UUID.randomUUID().toString();

    Ledger ledger = new Ledger().withId(UUID.randomUUID().toString());

    when(ledgerStorageRestClientMock.getById(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(ledger));
    when(ledgerTotalsMockService.populateLedgerTotals(any(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(ledger));

    ledgerService.retrieveLedgerWithTotals(ledger.getId(), fiscalYearId, requestContextMock).join();

    verify(ledgerStorageRestClientMock).getById(eq(ledger.getId()), eq(requestContextMock), eq(Ledger.class));
    verify(ledgerTotalsMockService).populateLedgerTotals(eq(ledger), eq(fiscalYearId), eq(requestContextMock));
  }

  @Test
  void shouldNotCallPopulateLedgerTotalsWhenCallRetrieveLedgerWithTotalsWithEmptyFiscalYearParameter() {

    String fiscalYearId = null;

    Ledger ledger = new Ledger().withId(UUID.randomUUID().toString());

    when(ledgerStorageRestClientMock.getById(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(ledger));

    ledgerService.retrieveLedgerWithTotals(ledger.getId(), fiscalYearId, requestContextMock).join();

    verify(ledgerStorageRestClientMock).getById(eq(ledger.getId()), eq(requestContextMock), eq(Ledger.class));
    verify(ledgerTotalsMockService, never()).populateLedgerTotals(any(), anyString(), any());
  }

  @Test
  void shouldCallRestClientPutWhenCallUpdateLedger() {
    Ledger ledger = new Ledger().withId(UUID.randomUUID().toString());
    when(ledgerStorageRestClientMock.put(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    ledgerService.updateLedger(ledger, requestContextMock).join();

    verify(ledgerStorageRestClientMock).put(eq(ledger.getId()), eq(ledger), eq(requestContextMock));
  }

  @Test
  void shouldCallRestClientDeleteWhenCallDeleteLedger() {
    String ledgerId = UUID.randomUUID().toString();
    when(ledgerStorageRestClientMock.delete(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

    ledgerService.deleteLedger(ledgerId, requestContextMock).join();

    verify(ledgerStorageRestClientMock).delete(eq(ledgerId), eq(requestContextMock));
  }

  @Test
  void testGetLedgers() {
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
    when(ledgerStorageRestClientMock.get(any(), any(), eq(LedgersCollection.class))).thenReturn(CompletableFuture.completedFuture(ledgersCollection));
    List<Ledger> ledgers = ledgerService.getLedgers(ids, requestContextMock).join();
    //Then
    assertEquals(ledgersCollection.getLedgers().get(0).getId(), ledgers.get(0).getId());
    assertEquals(ledgersCollection.getLedgers().get(1).getId(), ledgers.get(1).getId());
  }

  @Test
  void testGetLedgersByIds() {
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
    when(ledgerStorageRestClientMock.get(any(), any(), eq(LedgersCollection.class))).thenReturn(CompletableFuture.completedFuture(ledgersCollection));
    List<Ledger> ledgers = ledgerService.getLedgersByIds(ids, requestContextMock).join();
    //Then
    assertEquals(ledgersCollection.getLedgers().get(0).getId(), ledgers.get(0).getId());
    assertEquals(ledgersCollection.getLedgers().get(1).getId(), ledgers.get(1).getId());
  }
}
