package com.lifetracker.knowledge;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lifetracker.knowledge.storage.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;

class StorageTest {
  @TempDir Path directory;

  @Test
  void localStorageRejectsTraversalAndRoundTripsImages() throws Exception {
    var storage = new LocalArtworkStorage(directory.toString());
    String key = UUID.randomUUID() + ".png";
    byte[] bytes = {1, 2, 3};
    storage.put(key, bytes);
    assertArrayEquals(bytes, storage.read(key));
    assertThrows(IllegalArgumentException.class, () -> storage.read("../secret"));
    storage.delete(key);
    storage.delete(key);
    assertThrows(IllegalStateException.class, () -> storage.read(key));
  }

  @Test
  void s3AdapterUsesPrivateObjectOperations() {
    var server = new WireMockServer(0);
    server.start();
    try {
      server.stubFor(
          put(urlEqualTo("/artwork/test.png"))
              .willReturn(aResponse().withStatus(200).withHeader("ETag", "test")));
      server.stubFor(
          get(urlEqualTo("/artwork/test.png"))
              .willReturn(aResponse().withStatus(200).withBody(new byte[] {1, 2, 3})));
      server.stubFor(
          delete(urlEqualTo("/artwork/test.png")).willReturn(aResponse().withStatus(204)));
      var client =
          S3Client.builder()
              .region(Region.US_EAST_1)
              .endpointOverride(URI.create(server.baseUrl()))
              .credentialsProvider(
                  StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
              .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
              .build();
      var storage = new S3ArtworkStorage(client, "artwork");
      storage.put("test.png", new byte[] {1, 2, 3});
      assertArrayEquals(new byte[] {1, 2, 3}, storage.read("test.png"));
      storage.delete("test.png");
      storage.close();
      server.verify(putRequestedFor(urlEqualTo("/artwork/test.png")).withoutHeader("x-amz-acl"));
      server.verify(deleteRequestedFor(urlEqualTo("/artwork/test.png")));
    } finally {
      server.stop();
    }
  }
}
