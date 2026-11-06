package fu.de181019.user;

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
      "spring.datasource.url=jdbc:h2:mem:user;DB_CLOSE_DELAY=-1",
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

  @Test
  void fullUserLifecycleAndValidation() throws Exception {
    String valid =
        "{\"username\":\"creator_a\",\"email\":\"a@example.com\",\"role\":\"CREATOR\",\"ratePerPost\":500000,\"maxDailyPosts\":3,\"level\":1}";
    JsonNode created = call("POST", "/api/users", valid, 201, 1);
    long id = created.get("userId").asLong();
    assertEquals("ACTIVE", created.get("status").asString());
    assertTrue(call("POST", "/api/users", valid, 226, 3).isNull());
    call("POST", "/api/users", valid.replace("a@example.com", "bad-email"), 406, 2);
    call("POST", "/api/users", valid.replace("500000", "0"), 406, 2);
    call("POST", "/api/users", valid.replace("CREATOR", "INVALID"), 406, 2);
    call("POST", "/api/users", "{}", 406, 2);
    var updated = call("PUT", "/api/users/" + id, "{\"level\":2}", 200, 1);
    assertEquals(2, updated.get("level").asInt());
    assertEquals("creator_a", updated.get("username").asString());
    call("PUT", "/api/users/" + id, "{\"maxDailyPosts\":11}", 406, 2);
    call("GET", "/api/users/" + id, null, 200, 1);
    assertEquals(
        1,
        call("GET", "/api/users?role=CREATOR&status=ACTIVE", null, 200, 1)
            .get("totalElements")
            .asLong());
    assertEquals(
        0, call("GET", "/api/users?role=ADMIN", null, 200, 1).get("totalElements").asLong());
    call("GET", "/api/users?size=101", null, 406, 2);
    call("GET", "/api/users?page=-1", null, 406, 2);
    call("GET", "/api/users?role=INVALID", null, 406, 2);
    call("GET", "/api/users/99999", null, 404, 4);
    call("PUT", "/api/users/99999", "{}", 404, 4);
    call("DELETE", "/api/users/99999", null, 404, 4);
    assertTrue(call("DELETE", "/api/users/" + id, null, 200, 1).isNull());
    assertEquals(
        "DEACTIVATED", call("GET", "/api/users/" + id, null, 200, 1).get("status").asString());
  }

  @Test
  void swaggerAndCors() throws Exception {
    var response =
        http.send(
            HttpRequest.newBuilder(
                    URI.create(
                        "http://localhost:"
                            + env.getProperty("local.server.port")
                            + "/v3/api-docs"))
                .header("Origin", "http://example.com")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("/api/users"));
    assertEquals("*", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
  }
}
