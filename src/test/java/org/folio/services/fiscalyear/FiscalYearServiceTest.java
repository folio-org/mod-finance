package org.folio.services.fiscalyear;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.util.TestUtils.assertQueryContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.FiscalYear;
import org.folio.rest.jaxrs.model.FiscalYearsCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class FiscalYearServiceTest {
  private FiscalYearService fiscalYearService;
  private AutoCloseable mockitoMocks;

  @Mock
  private RestClient restClient;
  @Mock
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
    fiscalYearService = new FiscalYearService(restClient);
  }

  @AfterEach
  public void afterEach() throws Exception {
    mockitoMocks.close();
  }

  @Test
  void testGetFiscalYearByFiscalYearCode(VertxTestContext vertxTestContext) {
    FiscalYear fiscalYear = new FiscalYear()
      .withCode("FUND CODE");
    String fiscalYearCode = "FiscalCode";
    List<FiscalYear> fiscalYearList = new ArrayList<>();
    fiscalYearList.add(fiscalYear);
    FiscalYearsCollection fiscalYearsCollection = new FiscalYearsCollection();
    fiscalYearsCollection.setTotalRecords(10);
    fiscalYearsCollection.setFiscalYears(fiscalYearList);
    when(restClient.get(anyString(), eq(FiscalYearsCollection.class), eq(requestContext)))
      .thenReturn(succeededFuture(fiscalYearsCollection));

    assertThat(fiscalYearsCollection.getFiscalYears(), is(not(empty())));

    var future = fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext);

    vertxTestContext.assertComplete(future)
      .onSuccess(result -> {
        assertEquals("FUND CODE", future.result().getCode());

        vertxTestContext.completeNow();
      });
  }

  @Test
  void testGetFiscalYearByFiscalYearCodeWithEmptyCollection(VertxTestContext vertxTestContext) {
    String fiscalYearCode = "FiscalCode";
    FiscalYearsCollection fiscalYearsCollection = new FiscalYearsCollection();
    when(restClient.get(anyString(), eq(FiscalYearsCollection.class), eq(requestContext)))
      .thenReturn(succeededFuture(fiscalYearsCollection));
    Future<FiscalYear> future = fiscalYearService.getFiscalYearByFiscalYearCode(fiscalYearCode, requestContext);

    vertxTestContext.assertFailure(future)
      .onComplete(result -> {
        HttpException httpException = (HttpException) result.cause();
        assertEquals(400, httpException.getCode());
        vertxTestContext.completeNow();
      });
  }


  @Test
  void testShouldRetrieveFiscalYearsWithoutAcqUnits(VertxTestContext vertxTestContext) {
    //Given
    FiscalYear fiscalYear = new FiscalYear().withId(UUID.randomUUID().toString()).withCode("TST");
    FiscalYearsCollection fiscalYearsCollection = new FiscalYearsCollection().withFiscalYears(List.of(fiscalYear)).withTotalRecords(1);
    doReturn(succeededFuture(fiscalYearsCollection))
      .when(restClient).get(anyString(), eq(FiscalYearsCollection.class), eq(requestContext));
    //When
    var future = fiscalYearService.getFiscalYearsWithoutAcqUnitsRestriction("test_query", 0,10 , requestContext);
    //Then
    vertxTestContext.assertComplete(future)
      .onComplete(result -> {
        assertThat(fiscalYearsCollection, equalTo(result.result()));
        verify(restClient).get(assertQueryContains("test_query"), eq(FiscalYearsCollection.class), eq(requestContext));
        vertxTestContext.completeNow();
      });
  }

}
