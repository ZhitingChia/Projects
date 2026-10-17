package com.lifetracker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lifetracker.identity.*;
import com.lifetracker.knowledge.repository.*;
import com.lifetracker.knowledge.service.*;
import com.lifetracker.knowledge.storage.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class LibraryIntegrationTest {
  @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    r.add("app.storage.path", () -> "target/test-artwork");
    r.add("app.base-url", () -> "http://localhost");
    r.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    r.add(
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener",
        () -> "OFF");
  }

  @Autowired Environment environment;
  @Autowired JdbcTemplate jdbc;
  @Autowired ArtworkService artwork;
  @Autowired ArtworkJanitor janitor;
  @Autowired ArtworkStorage storage;
  @Autowired LibraryService library;
  @Autowired jakarta.persistence.EntityManagerFactory entityManagers;
  @MockitoBean JavaMailSender mail;
  private final ObjectMapper json = JsonMapper.builder().build();
  private String base;

  @Test
  void expiredTokensAndPrivateArtworkCleanup() throws Exception {
    Browser a = new Browser(), b = new Browser();
    base = "http://localhost:" + environment.getProperty("local.server.port") + "/api/v1";
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String email = "art" + suffix + "@example.test";
    assertEquals(
        202,
        a.call(
                "POST",
                "/auth/register",
                Map.of("email", email, "password", "a long personal password"))
            .statusCode());
    String token = lastToken();
    jdbc.update(
        "update account_tokens set expires_at = now() - interval '1 day' where consumed_at is"
            + " null");
    assertEquals(400, a.call("POST", "/auth/verify-email", Map.of("token", token)).statusCode());
    assertEquals(
        202, a.call("POST", "/auth/resend-verification", Map.of("email", email)).statusCode());
    assertEquals(
        204, a.call("POST", "/auth/verify-email", Map.of("token", lastToken())).statusCode());
    a.login(email);
    register(b, "art-other" + suffix + "@example.test");
    var reading = Map.of("requestId", UUID.randomUUID(), "localDate", "2026-10-17");
    var create =
        Map.of(
            "requestId",
            UUID.randomUUID(),
            "metadata",
            Map.of("title", "Artwork test", "authors", List.of()),
            "reading",
            reading);
    var created = a.call("POST", "/library/books", create);
    assertEquals(201, created.statusCode(), created.body());
    var otherCreated = b.call("POST", "/library/books", create);
    assertEquals(201, otherCreated.statusCode(), otherCreated.body());
    String id = json.readTree(created.body()).path("id").asString();
    String otherId = json.readTree(otherCreated.body()).path("id").asString();
    var image = new java.awt.image.BufferedImage(10, 20, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", bytes);
    var staged = artwork.stage(library.owner(email), "UPLOAD", bytes.toByteArray(), "image/png");
    assertEquals(404, b.call("GET", "/artwork/" + staged.id + "/content", null).statusCode());
    assertEquals(
        404,
        b.call(
                "PUT",
                "/library/books/" + otherId + "/artwork/UPLOAD",
                Map.of("artworkId", staged.id))
            .statusCode());
    assertEquals(
        200,
        a.call("PUT", "/library/books/" + id + "/artwork/UPLOAD", Map.of("artworkId", staged.id))
            .statusCode());
    assertArrayEquals(storage.read(staged.storageKey), bytes.toByteArray());
    assertEquals(204, a.call("DELETE", "/library/books/" + id, null).statusCode());
    janitor.clean();
    assertThrows(IllegalStateException.class, () -> storage.read(staged.storageKey));
  }

  @Test
  void thousandBookLibraryUsesBoundedPageQueries() throws Exception {
    Browser reader = new Browser();
    String email = "large" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    register(reader, email);
    UUID owner = library.owner(email);
    List<Object[]> bookRows = new ArrayList<>(),
        entryRows = new ArrayList<>(),
        sessionRows = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      UUID book = UUID.randomUUID(), entry = UUID.randomUUID();
      bookRows.add(new Object[] {book, "Book " + i});
      entryRows.add(new Object[] {entry, owner, book, UUID.randomUUID()});
      sessionRows.add(new Object[] {UUID.randomUUID(), entry, UUID.randomUUID()});
    }
    jdbc.batchUpdate("insert into books(id,title) values (?,?)", bookRows);
    jdbc.batchUpdate(
        "insert into"
            + " library_entries(id,user_id,book_id,request_id,reading_status,spine_color,spine_width,spine_height)"
            + " values (?,?,?,?,'COMPLETED','#3e6560',44,190)",
        entryRows);
    jdbc.batchUpdate(
        "insert into reading_sessions(id,library_entry_id,request_id,completion_date) values"
            + " (?,?,?,'2026-10-17')",
        sessionRows);
    var stats = entityManagers.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    stats.clear();
    var response = reader.call("GET", "/library/books?page=0&size=100", null);
    assertEquals(200, response.statusCode(), response.body());
    var page = json.readTree(response.body());
    assertEquals(1000, page.path("total").asInt());
    assertEquals(100, page.path("items").size());
    assertTrue(page.path("hasMore").asBoolean());
    assertTrue(
        stats.getPrepareStatementCount() <= 10,
        "A page must batch fetch its related records; actual JPA statements: "
            + stats.getPrepareStatementCount());
    var create =
        Map.of(
            "requestId",
            UUID.randomUUID(),
            "metadata",
            Map.of("title", "The newest book", "authors", List.of()),
            "reading",
            Map.of("requestId", UUID.randomUUID(), "localDate", "2026-10-17"));
    var saved = reader.call("POST", "/library/books", create);
    assertEquals(201, saved.statusCode(), saved.body());
    var firstPage =
        json.readTree(reader.call("GET", "/library/books?page=0&size=100", null).body());
    assertEquals(
        json.readTree(saved.body()).path("id").asString(),
        firstPage.path("items").path(0).path("id").asString(),
        "Newly added books must be visible even when the library exceeds one page");
  }

  class Browser {
    HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    String csrf;

    HttpResponse<String> call(String method, String path, Object body) throws Exception {
      var builder = HttpRequest.newBuilder(URI.create(base + path));
      if (!method.equals("GET")) {
        if (csrf == null)
          csrf = json.readTree(call("GET", "/auth/csrf", null).body()).path("token").asString();
        builder.header("X-CSRF-TOKEN", csrf).header("Content-Type", "application/json");
      }
      return client.send(
          builder
              .method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }

    void login(String email) throws Exception {
      assertEquals(
          200,
          call(
                  "POST",
                  "/auth/login",
                  Map.of("email", email, "password", "a long personal password"))
              .statusCode());
      csrf = null;
    }
  }

  String lastToken() {
    ArgumentCaptor<SimpleMailMessage> captured = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mail, atLeastOnce()).send(captured.capture());
    String text = captured.getValue().getText();
    return text.substring(text.indexOf("#token=") + 7).split("\\s")[0];
  }

  void register(Browser b, String email) throws Exception {
    assertEquals(
        202,
        b.call(
                "POST",
                "/auth/register",
                Map.of("email", email, "password", "a long personal password"))
            .statusCode());
    String token = lastToken();
    assertEquals(204, b.call("POST", "/auth/verify-email", Map.of("token", token)).statusCode());
    assertEquals(400, b.call("POST", "/auth/verify-email", Map.of("token", token)).statusCode());
    b.login(email);
  }

  @BeforeEach
  void setup() {
    base = "http://localhost:" + environment.getProperty("local.server.port") + "/api/v1";
  }

  @Test
  void fullPersistenceOwnershipTransactionsAndAccountRecovery() throws Exception {
    Browser a = new Browser(), b = new Browser();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String email = "reader" + suffix + "@example.test";
    assertEquals(401, a.call("GET", "/library/books", null).statusCode());
    register(a, email);
    register(b, "other" + suffix + "@example.test");
    var metadata = new LinkedHashMap<String, Object>();
    metadata.put("title", "The test book");
    metadata.put("authors", List.of("An author"));
    metadata.put("isbn13", "9780306406157");
    var reading = new LinkedHashMap<String, Object>();
    reading.put("requestId", UUID.randomUUID());
    reading.put("localDate", "2026-10-17");
    reading.put("review", "First paragraph\n\nSecond paragraph");
    var create = new LinkedHashMap<String, Object>();
    create.put("requestId", UUID.randomUUID());
    create.put("metadata", metadata);
    create.put("reading", reading);
    create.put("confirmPossibleDuplicate", false);
    var saved = a.call("POST", "/library/books", create);
    assertEquals(201, saved.statusCode(), saved.body());
    JsonNode book = json.readTree(saved.body());
    String id = book.path("id").asString();
    assertEquals("2026-10-17", book.path("sessions").path(0).path("completionDate").asString());
    assertEquals(
        "First paragraph\n\nSecond paragraph",
        book.path("sessions").path(0).path("review").asString());
    assertEquals(
        id, json.readTree(a.call("POST", "/library/books", create).body()).path("id").asString());
    assertEquals(404, b.call("GET", "/library/books/" + id, null).statusCode());
    assertEquals(404, b.call("DELETE", "/library/books/" + id, null).statusCode());
    assertEquals(404, b.call("GET", "/library/books/" + id + "/description", null).statusCode());
    assertEquals(
        1, json.readTree(a.call("GET", "/library/books", null).body()).path("total").asInt());
    var noCsrf =
        a.client.send(
            HttpRequest.newBuilder(URI.create(base + "/library/books/" + id)).DELETE().build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(403, noCsrf.statusCode());
    create.put("requestId", UUID.randomUUID());
    assertEquals(409, a.call("POST", "/library/books", create).statusCode());
    metadata.put("title", "Other edition");
    metadata.put("isbn13", "9780140328721");
    create.put("drawingId", UUID.randomUUID());
    long before = jdbc.queryForObject("select count(*) from books", Long.class);
    assertEquals(404, a.call("POST", "/library/books", create).statusCode());
    assertEquals(before, jdbc.queryForObject("select count(*) from books", Long.class));
    String session = book.path("sessions").path(0).path("id").asString();
    var edit = new LinkedHashMap<String, Object>();
    edit.put("version", 0);
    edit.put("completionDate", "2026-10-18");
    edit.put("review", null);
    assertEquals(
        404, b.call("PATCH", "/library/books/" + id + "/sessions/" + session, edit).statusCode());
    assertEquals(
        200, a.call("PATCH", "/library/books/" + id + "/sessions/" + session, edit).statusCode());
    assertEquals(
        409, a.call("PATCH", "/library/books/" + id + "/sessions/" + session, edit).statusCode());
    assertEquals(
        409, a.call("DELETE", "/library/books/" + id + "/sessions/" + session, null).statusCode());
    reading.put("requestId", UUID.randomUUID());
    assertEquals(201, a.call("POST", "/library/books/" + id + "/sessions", reading).statusCode());
    assertEquals(
        204, a.call("DELETE", "/library/books/" + id + "/sessions/" + session, null).statusCode());
    var description = a.call("GET", "/library/books/" + id + "/description", null);
    assertEquals(200, description.statusCode());
    assertTrue(description.headers().firstValue("cache-control").orElse("").contains("no-store"));
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from information_schema.columns where table_name='books' and"
                + " column_name like '%description%'",
            Integer.class));
    assertEquals(204, a.call("DELETE", "/library/books/" + id, null).statusCode());
    assertEquals(
        0, json.readTree(a.call("GET", "/library/books", null).body()).path("total").asInt());
    assertEquals(before, jdbc.queryForObject("select count(*) from books", Long.class));
    assertEquals(202, a.call("POST", "/auth/forgot-password", Map.of("email", email)).statusCode());
    String reset = lastToken();
    Browser anonymous = new Browser();
    assertEquals(
        204,
        anonymous
            .call(
                "POST",
                "/auth/reset-password",
                Map.of("token", reset, "password", "another long personal password"))
            .statusCode());
    assertEquals(401, a.call("GET", "/auth/me", null).statusCode());
    assertEquals(
        400,
        anonymous
            .call(
                "POST",
                "/auth/reset-password",
                Map.of("token", reset, "password", "another long personal password"))
            .statusCode());
    assertEquals(
        200,
        anonymous
            .call(
                "POST",
                "/auth/login",
                Map.of("email", email, "password", "another long personal password"))
            .statusCode());
  }
}
