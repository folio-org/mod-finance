package org.folio.services.fund;

import static org.folio.rest.RestConstants.NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.FUND_NOT_FOUND_ERROR;
import static org.folio.rest.util.HelperUtils.combineCqlExpressions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundsCollection;
import org.folio.services.protection.AcqUnitsService;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class FundService {
  private static final Logger logger = LogManager.getLogger(FundService.class);

  private final RestClient fundStorageRestClient;
  private final AcqUnitsService acqUnitsService;


  public FundService(RestClient fundStorageRestClient, AcqUnitsService acqUnitsService) {
    this.fundStorageRestClient = fundStorageRestClient;
    this.acqUnitsService = acqUnitsService;
  }

  public CompletableFuture<Fund> retrieveFundById(String fundId, RequestContext requestContext) {
    return fundStorageRestClient.getById(fundId, requestContext, Fund.class)
                                .exceptionally(t -> {
                                  if (t instanceof HttpException) {
                                    HttpException httpException = (HttpException) t;
                                    if (httpException.getCode() == NOT_FOUND) {
                                      Error error = new Error().withCode(FUND_NOT_FOUND_ERROR.getCode()).withMessage(String.format(FUND_NOT_FOUND_ERROR.getDescription(), fundId));
                                      throw new CompletionException(new HttpException(NOT_FOUND, error));
                                    }
                                  }
                                  throw new CompletionException(t);
                                });
  }

  public CompletableFuture<FundsCollection> getFundsWithAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
   return acqUnitsService.buildAcqUnitsCqlClause(requestContext)
      .thenApply(clause -> StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query))
      .thenCompose(effectiveQuery -> fundStorageRestClient.get(effectiveQuery, offset, limit, requestContext, FundsCollection.class));
  }

  public CompletableFuture<FundsCollection> getFundsWithoutAcqUnitsRestriction(String query, int offset, int limit, RequestContext requestContext) {
    return fundStorageRestClient.get(query, offset, limit, requestContext, FundsCollection.class);
  }
}
