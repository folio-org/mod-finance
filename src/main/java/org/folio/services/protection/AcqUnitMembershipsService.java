package org.folio.services.protection;

import static org.folio.rest.util.ResourcePathResolver.ACQUISITIONS_MEMBERSHIPS;
import static org.folio.rest.util.ResourcePathResolver.resourcesPath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembershipCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;

public class AcqUnitMembershipsService {

  public final Logger log = LogManager.getLogger();

  private final RestClient restClient;

  public AcqUnitMembershipsService(RestClient acqUnitMembershipsRestClient) {
    this.restClient = acqUnitMembershipsRestClient;
  }

  public Future<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, RequestContext requestContext) {
    var requestEntry = new RequestEntry(resourcesPath(ACQUISITIONS_MEMBERSHIPS))
      .withOffset(offset)
      .withLimit(limit)
      .withQuery(query);
    return restClient.get(requestEntry.buildEndpoint(), AcquisitionsUnitMembershipCollection.class, requestContext);
  }
}
