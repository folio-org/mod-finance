package org.folio.services.protection;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.acq.model.finance.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.finance.AcquisitionsUnitMembershipCollection;
import org.folio.rest.core.models.RequestContext;

public interface AcquisitionsUnitsService {
  CompletableFuture<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, String lang, RequestContext requestContext);
  CompletableFuture<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, String lang, RequestContext requestContext);
  CompletableFuture<String> buildAcqUnitsCqlClause(String query, int offset, int limit, String lang, RequestContext requestContext);
}
