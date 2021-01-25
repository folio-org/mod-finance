package org.folio.services.fund;

import static org.folio.rest.RestConstants.NOT_FOUND;
import static org.folio.rest.util.ErrorCodes.FUND_NOT_FOUND_ERROR;
import static org.folio.rest.util.ResourcePathResolver.FUNDS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;
import static org.folio.services.BaseService.SEARCH_PARAMS;
import static org.folio.services.BaseService.buildQuery;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.exception.HttpException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Fund;
import org.folio.rest.jaxrs.model.FundsCollection;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.services.BaseService;
import org.folio.services.protection.AcquisitionsUnitsService;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class FundService extends BaseService {
  private static final Logger logger = LoggerFactory.getLogger(FundService.class);
  public static final String GET_FUNDS_BY_QUERY = resourcesPath(FUNDS_STORAGE) + SEARCH_PARAMS;

  private final RestClient fundStorageRestClient;
  private final AcquisitionsUnitsService acquisitionUnitsService;


  public FundService(RestClient fundStorageRestClient, AcquisitionsUnitsService acquisitionUnitsService) {
    this.fundStorageRestClient = fundStorageRestClient;
    this.acquisitionUnitsService = acquisitionUnitsService;
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

  public CompletableFuture<FundsCollection> getFundsWithAcqUnitsRestriction(int limit, int offset, String query,
                                                                            String lang, RequestContext requestContext) {
   return acquisitionUnitsService.buildAcqUnitsCqlClause(query, offset, limit, lang, requestContext)
      .thenCompose(clause -> {
        String effectiveQuery = StringUtils.isEmpty(query) ? clause : combineCqlExpressions("and", clause, query);
        return fundStorageRestClient.get(effectiveQuery, offset, limit, requestContext, FundsCollection.class);
      });
  }

  public CompletableFuture<FundsCollection> getFundsWithoutAcqUnitsRestriction(int limit, int offset, String query,
      RequestContext requestContext) {
    return fundStorageRestClient.get(query, offset, limit, requestContext, FundsCollection.class);
  }
}
