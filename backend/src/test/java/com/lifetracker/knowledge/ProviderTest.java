package com.lifetracker.knowledge;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lifetracker.knowledge.provider.*;
import com.lifetracker.knowledge.provider.googlebooks.GoogleBooksProvider;
import com.lifetracker.knowledge.provider.openlibrary.OpenLibraryProvider;
import java.util.List;
import org.junit.jupiter.api.*;

class ProviderTest {
  WireMockServer server;

  @BeforeEach
  void start() {
    server = new WireMockServer(0);
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop();
  }

  @Test
  void searchesSurviveOneProviderOutageAndNeverIncludeDescriptions() {
    server.stubFor(
        get(urlPathEqualTo("/volumes"))
            .willReturn(
                okJson(
                    """
                    {"items":[{"id":"abc","volumeInfo":{"title":"A book","authors":["A writer"],"description":"must not escape search","industryIdentifiers":[{"type":"ISBN_13","identifier":"9780306406157"}]}}]}
                    """)));
    server.stubFor(get(urlPathEqualTo("/search.json")).willReturn(aResponse().withStatus(429)));
    var http = new ProviderHttp();
    var service =
        new ProviderService(
            List.of(
                new GoogleBooksProvider(http, server.baseUrl(), ""),
                new OpenLibraryProvider(http, server.baseUrl())));
    var result = service.search("book", null, null, 0);
    assertEquals(1, result.items().size());
    assertEquals("A book", result.items().getFirst().title());
    assertEquals(1, result.warnings().size());
    assertFalse(result.toString().contains("must not escape"));
  }

  @Test
  void stripsDescriptionMarkupAndRestrictsCoverHosts() {
    server.stubFor(
        get(urlPathEqualTo("/volumes/abc"))
            .willReturn(
                okJson(
                    """
                    {"id":"abc","volumeInfo":{"description":"<p>Hello</p><script>alert(1)</script><p>World</p>"}}
                    """)));
    var description =
        new GoogleBooksProvider(new ProviderHttp(), server.baseUrl(), "").description("abc");
    assertTrue(description.text().contains("Hello"));
    assertFalse(description.text().contains("<script>"));
    assertNull(ProviderHttp.safeCover("https://evil.example/cover.png"));
    assertEquals(
        "https://covers.openlibrary.org/b/id/1-M.jpg",
        ProviderHttp.safeCover("http://covers.openlibrary.org/b/id/1-M.jpg"));
  }
}
