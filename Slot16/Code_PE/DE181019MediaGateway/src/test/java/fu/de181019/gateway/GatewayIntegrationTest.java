package fu.de181019.gateway;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {
  static HttpServer downstream;

  static {
    try {
      downstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      downstream.createContext(
          "/api",
          e -> {
            byte[] bytes = e.getRequestURI().getPath().getBytes();
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.sendResponseHeaders(200, bytes.length);
            e.getResponseBody().write(bytes);
            e.close();
          });
      downstream.start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    for (String service : new String[] {"user", "post"})
      registry.add(
          service + ".service.url", () -> "http://127.0.0.1:" + downstream.getAddress().getPort());
  }

  @AfterAll
  static void stop() {
    downstream.stop(0);
  }

  @Autowired Environment env;

  @Test
  void routesCorsAndSwagger() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    String host = "http://localhost:" + env.getProperty("local.server.port");
    for (String path : new String[] {"/api/users", "/api/users/1", "/api/posts", "/api/posts/1"}) {
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create(host + path)).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode(), response.body());
      assertEquals(path, response.body());
    }
    var cors =
        client.send(
            HttpRequest.newBuilder(URI.create(host + "/api/users"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://example.com")
                .header("Access-Control-Request-Method", "POST")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, cors.statusCode());
    assertEquals("*", cors.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    for (String path :
        new String[] {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui/index.html"}) {
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create(host + path)).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode(), response.body());
      if (path.endsWith("swagger-config")) {
        assertTrue(response.body().contains("user-service"));
        assertTrue(response.body().contains("post-service"));
      }
    }
  }
}
