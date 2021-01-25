package org.folio.services.protection;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Fund;

public interface ProtectionService {
  CompletableFuture<Void> checkOperationsRestrictions(List<String> unitIds, Set<ProtectedOperationType> operations,
                                                      String lang, RequestContext requestContext);
  CompletableFuture<Void> validateAcqUnitsOnUpdate(Fund updatedOrg, Fund currentOrg,
                                                   String lang, RequestContext requestContext);
}
