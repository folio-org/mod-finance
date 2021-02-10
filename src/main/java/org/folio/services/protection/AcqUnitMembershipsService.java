package org.folio.services.protection;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.acq.model.finance.AcquisitionsUnitMembershipCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class AcqUnitMembershipsService {
  public final Logger logger = LogManager.getLogger(AcqUnitMembershipsService.class);

  private final RestClient acqUnitMembershipsRestClient;

  public AcqUnitMembershipsService(RestClient acqUnitMembershipsRestClient) {
    this.acqUnitMembershipsRestClient = acqUnitMembershipsRestClient;
  }

  public CompletableFuture<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset,
                                                                                                 int limit, RequestContext requestContext) {
    return acqUnitMembershipsRestClient.get(query, offset, limit, requestContext, AcquisitionsUnitMembershipCollection.class);
  }
}
