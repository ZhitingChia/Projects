package com.lifetracker.knowledge.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3ArtworkStorage implements ArtworkStorage {
  private final S3Client client;
  private final String bucket;

  @org.springframework.beans.factory.annotation.Autowired
  public S3ArtworkStorage(
      @Value("${app.storage.bucket}") String bucket,
      @Value("${app.storage.region}") String region) {
    this(S3Client.builder().region(Region.of(region)).build(), bucket);
  }

  public S3ArtworkStorage(S3Client client, String bucket) {
    this.client = client;
    this.bucket = bucket;
  }

  public void put(String key, byte[] bytes) {
    client.putObject(
        b -> b.bucket(bucket).key(key).contentType("image/png"), RequestBody.fromBytes(bytes));
  }

  public byte[] read(String key) {
    return client.getObjectAsBytes(b -> b.bucket(bucket).key(key)).asByteArray();
  }

  public void delete(String key) {
    client.deleteObject(b -> b.bucket(bucket).key(key));
  }

  public java.util.List<String> olderThan(java.time.Instant instant) {
    return client.listObjectsV2Paginator(b -> b.bucket(bucket)).contents().stream()
        .filter(o -> o.lastModified().isBefore(instant) && o.key().matches("[a-f0-9-]{36}\\.png"))
        .limit(100)
        .map(o -> o.key())
        .toList();
  }

  @jakarta.annotation.PreDestroy
  public void close() {
    client.close();
  }
}
