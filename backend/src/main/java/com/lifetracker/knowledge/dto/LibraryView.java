package com.lifetracker.knowledge.dto;

import java.time.*;
import java.util.*;

public record LibraryView(
    UUID id,
    long version,
    Metadata metadata,
    String readingStatus,
    Instant dateAdded,
    String spineColor,
    int spineWidth,
    int spineHeight,
    String coverUrl,
    List<ArtView> artwork,
    List<SessionView> sessions) {
  public record ArtView(UUID id, String type, String url) {}

  public record SessionView(
      UUID id, long version, LocalDate startDate, LocalDate completionDate, String review) {}

  public record Page(List<LibraryView> items, boolean hasMore, long total) {}
}
