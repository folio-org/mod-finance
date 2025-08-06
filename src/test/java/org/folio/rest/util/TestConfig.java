package org.folio.rest.util;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.spring.SpringContextUtil;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class TestConfig {
  public static final int mockPort = NetworkUtils.nextFreePort();
  public static final Header X_OKAPI_URL = new Header(TestConstants.OKAPI_URL, "http://localhost:" + mockPort);

  private static MockServer mockServer;
  private static final Vertx vertx = Vertx.vertx();

  public static void deployVerticle() throws InterruptedException, ExecutionException, TimeoutException {
    int okapiPort = NetworkUtils.nextFreePort();
    RestAssured.baseURI = "http://localhost:" + okapiPort;
    RestAssured.port = okapiPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    final JsonObject conf = new JsonObject();
    conf.put("http.port", okapiPort);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    vertx.deployVerticle(RestVerticle.class.getName(), opt)
      .toCompletionStage().toCompletableFuture().get(60, TimeUnit.SECONDS);
  }

  public static void initSpringContext(Class<?> defaultConfiguration) {
    SpringContextUtil.init(vertx, getFirstContextFromVertx(vertx), defaultConfiguration);
  }

  public static void autowireDependencies(Object target) {
    SpringContextUtil.autowireDependenciesFromFirstContext(target, getVertx());
  }

  public static void clearVertxContext() {
    Context context = getFirstContextFromVertx(vertx);
    context.remove("springContext");
  }

  public static void startMockServer() throws InterruptedException, ExecutionException, TimeoutException {
    mockServer = new MockServer(mockPort);
    mockServer.start();
  }

  public static Vertx getVertx() {
    return vertx;
  }

  public static void clearServiceInteractions() {
    MockServer.serverRqRs.clear();
    MockServer.serverRqQueries.clear();
  }

  public static void closeMockServer() {
    mockServer.close();
  }

  public static void closeVertx() {
    vertx.close();
  }

  public static boolean isVerticleNotDeployed() {
    return vertx.deploymentIDs().isEmpty();
  }

  private static Context getFirstContextFromVertx(Vertx vertx) {
    return vertx.deploymentIDs().stream()
      .flatMap(id -> ((VertxImpl) vertx).deploymentManager().deployment(id).deployment().contexts().stream())
      .filter(Objects::nonNull)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Spring context was not created"));
  }

}
