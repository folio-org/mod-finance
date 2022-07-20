package org.folio.services.email;

import org.folio.models.EmailEntity;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EmailService {

  private RestClient emailRestClient;

  public EmailService(RestClient emailRestClient) {
    this.emailRestClient = emailRestClient;
  }

  public CompletableFuture<EmailEntity> sendEmail(RequestContext requestContext) {
    // temporary solution, mod-email accepts tenant with upper case style,
    // also it requires text/plain Accept header also with upper case
    Map<String, String> newHeaders = new HashMap<>(requestContext.getHeaders());
    newHeaders.put("Accept", "text/plain");
    newHeaders.put("X-Okapi-Tenant", "diku");
    requestContext.withHeaders(newHeaders);

    EmailEntity emailEntity = new EmailEntity();
    emailEntity.setNotificationId(UUID.randomUUID().toString());
    emailEntity.setBody("<p style=\"color:blue\">Test email from Folio body.</p>");
    emailEntity.setHeader("Test email from Folio");
    emailEntity.setTo("svnosko@gmail.com");
    emailEntity.setOutputFormat("text/html");

    return emailRestClient.post(emailEntity, requestContext, EmailEntity.class);
  }
}
