package org.folio.services;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Ledger;

public class LedgerService {

  private final RestClient ledgerStorageRestClient;

  public LedgerService(RestClient ledgerStorageRestClient) {
    this.ledgerStorageRestClient = ledgerStorageRestClient;
  }

  public CompletableFuture<Ledger> retrieveLedgerById(String ledgerId, RequestContext requestContext) {
    return ledgerStorageRestClient.getById(ledgerId, requestContext, Ledger.class);
  }
}
