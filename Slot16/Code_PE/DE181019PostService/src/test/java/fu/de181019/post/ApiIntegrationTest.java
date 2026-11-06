package fu.de181019.post;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import tools.jackson.databind.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:post;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class ApiIntegrationTest {
  @Autowired Environment env;
  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  private JsonNode call(String method, String path, String body, int expected, int status)
      throws Exception {
    var b =
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + env.getProperty("local.server.port") + path))
            .header("Content-Type", "application/json");
    var response =
        http.send(
            b.method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(expected, response.statusCode(), response.body());
    JsonNode json = mapper.readTree(response.body());
    assertEquals(status, json.get("status").asInt(), response.body());
    assertNotNull(java.time.Instant.parse(json.get("timestamp").asString()));
    return json.get("data");
  }

  static com.sun.net.httpserver.HttpServer remote;
  static volatile int remoteStatus = 200;
  static volatile String userStatus = "ACTIVE";
  static volatile long rate = 500000;

  static {
    try {
      remote = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      remote.createContext(
          "/api/users",
          exchange -> {
            String response =
                "{\"status\":"
                    + (remoteStatus == 200 ? 1 : 4)
                    + ",\"message\":\"test\",\"timestamp\":\"2026-07-24T00:00:00Z\",\"data\":{\"userId\":1,\"username\":\"creator_a\",\"email\":\"a@example.com\",\"role\":\"CREATOR\",\"ratePerPost\":"
                    + rate
                    + ",\"maxDailyPosts\":3,\"level\":1,\"status\":\""
                    + userStatus
                    + "\"}}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(remoteStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
          });
      remote.start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @org.springframework.test.context.DynamicPropertySource
  static void properties(org.springframework.test.context.DynamicPropertyRegistry registry) {
    registry.add("user.service.url", () -> "http://127.0.0.1:" + remote.getAddress().getPort());
  }

  @AfterAll
  static void stop() {
    remote.stop(0);
  }

  @Test
  void campaignLifecycleAndFeignFailures() throws Exception {
    String valid =
        "{\"advertiserName\":\"Shop"
            + " ABC\",\"advertiserEmail\":\"ads@example.com\",\"advertiserPhone\":\"0900000000\",\"startDate\":\"25/07/2026\",\"endDate\":\"28/07/2026\",\"numberOfPosts\":2,\"userId\":1}";
    var created = call("POST", "/api/posts", valid, 201, 1);
    long id = created.get("postId").asLong();
    assertEquals("SCHEDULED", created.get("status").asString());
    assertEquals(1500000, created.get("totalCost").asLong());
    assertEquals("25/07/2026", created.get("startDate").asString());
    var detail = call("GET", "/api/posts/" + id, null, 200, 1);
    assertNotNull(detail.get("user"));
    assertNull(detail.get("userId"));
    rate = 600000;
    assertEquals(
        1800000,
        call("PUT", "/api/posts/" + id, "{\"advertiserName\":\"New shop\"}", 200, 1)
            .get("totalCost")
            .asLong());
    call("PUT", "/api/posts/" + id, "{\"endDate\":\"24/07/2026\"}", 406, 2);
    call("PUT", "/api/posts/" + id, "{\"numberOfPosts\":4}", 406, 2);
    call("POST", "/api/posts", valid.replace("28/07/2026", "31/02/2026"), 406, 2);
    call("POST", "/api/posts", valid.replace("28/07/2026", "25/07/2026"), 406, 2);
    call("POST", "/api/posts", valid.replace("ads@example.com", "bad-email"), 406, 2);
    call("POST", "/api/posts", "{}", 406, 2);
    assertEquals(
        1,
        call("GET", "/api/posts?advertiserName=shop&status=SCHEDULED", null, 200, 1)
            .get("totalElements")
            .asLong());
    assertEquals(
        0,
        call("GET", "/api/posts?advertiserName=missing", null, 200, 1)
            .get("totalElements")
            .asLong());
    call("GET", "/api/posts?size=101", null, 406, 2);
    call("GET", "/api/posts?status=INVALID", null, 406, 2);
    call("GET", "/api/posts/99999", null, 404, 4);
    call("PUT", "/api/posts/99999", "{}", 404, 4);
    call("DELETE", "/api/posts/99999", null, 404, 4);
    userStatus = "SUSPENDED";
    call("POST", "/api/posts", valid, 400, 5);
    userStatus = "DEACTIVATED";
    call("POST", "/api/posts", valid, 400, 5);
    userStatus = "ACTIVE";
    remoteStatus = 404;
    call("POST", "/api/posts", valid, 404, 4);
    remoteStatus = 503;
    call("POST", "/api/posts", valid, 500, 0);
    remoteStatus = 200;
    assertTrue(call("DELETE", "/api/posts/" + id, null, 200, 1).isNull());
    assertEquals(
        "CANCELLED", call("GET", "/api/posts/" + id, null, 200, 1).get("status").asString());
  }

  @Test
  void swagger() throws Exception {
    var response =
        http.send(
            HttpRequest.newBuilder(
                    URI.create(
                        "http://localhost:"
                            + env.getProperty("local.server.port")
                            + "/v3/api-docs"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("/api/posts"));
  }
}
