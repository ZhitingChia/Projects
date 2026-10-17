package com.lifetracker.knowledge.storage;

import java.io.*;
import java.time.Instant;
import java.util.List;

public interface ArtworkStorage {
  void put(String key, byte[] bytes);

  byte[] read(String key);

  void delete(String key);

  default List<String> olderThan(Instant instant) {
    return List.of();
  }
}
