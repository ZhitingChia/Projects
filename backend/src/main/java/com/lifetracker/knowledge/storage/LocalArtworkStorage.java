package com.lifetracker.knowledge.storage;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalArtworkStorage implements ArtworkStorage {
  private final Path root;

  public LocalArtworkStorage(@Value("${app.storage.path}") String path) throws IOException {
    root = Path.of(path).toAbsolutePath().normalize();
    Files.createDirectories(root);
  }

  private Path path(String key) {
    if (!key.matches("[a-f0-9-]{36}\\.png"))
      throw new IllegalArgumentException("Invalid storage key");
    Path p = root.resolve(key).normalize();
    if (!p.startsWith(root)) throw new IllegalArgumentException("Invalid storage path");
    return p;
  }

  public void put(String key, byte[] bytes) {
    try {
      Files.write(path(key), bytes, StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new IllegalStateException("Artwork could not be saved");
    }
  }

  public byte[] read(String key) {
    try {
      return Files.readAllBytes(path(key));
    } catch (IOException e) {
      throw new IllegalStateException("Artwork is unavailable");
    }
  }

  public void delete(String key) {
    try {
      Files.deleteIfExists(path(key));
    } catch (IOException e) {
      throw new IllegalStateException("Artwork cleanup failed");
    }
  }

  public List<String> olderThan(Instant instant) {
    try (var paths = Files.list(root)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(
              p -> {
                try {
                  return Files.getLastModifiedTime(p).toInstant().isBefore(instant);
                } catch (IOException e) {
                  return false;
                }
              })
          .map(p -> p.getFileName().toString())
          .filter(k -> k.matches("[a-f0-9-]{36}\\.png"))
          .limit(100)
          .toList();
    } catch (IOException e) {
      return List.of();
    }
  }
}
