package org.folio.services.protection;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.acq.model.finance.AcquisitionsUnitMembershipCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AcqUnitMembershipsService {
  public final Logger logger = LoggerFactory.getLogger(AcqUnitMembershipsService.class);

  private final RestClient acqUnitMembershipsRestClient;

  public AcqUnitMembershipsService(RestClient acqUnitMembershipsRestClient) {
    this.acqUnitMembershipsRestClient = acqUnitMembershipsRestClient;
  }

  public CompletableFuture<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset,
                                                                                                 int limit, RequestContext requestContext) {
    return acqUnitMembershipsRestClient.get(query, offset, limit, requestContext, AcquisitionsUnitMembershipCollection.class);
  }
}
