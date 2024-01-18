package org.folio.services.transactions;

import io.vertx.core.Future;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Batch;

import static org.folio.rest.util.ResourcePathResolver.BATCH_TRANSACTIONS_STORAGE;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

public class BatchTransactionService {
  final RestClient restClient;

  public BatchTransactionService(RestClient restClient) {
    this.restClient = restClient;
  }

  public Future<Void> processBatch(Batch batch, RequestContext requestContext) {
    return restClient.postEmptyResponse(resourcesPath(BATCH_TRANSACTIONS_STORAGE), batch, requestContext);
  }
}
