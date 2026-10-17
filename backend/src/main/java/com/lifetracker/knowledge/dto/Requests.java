package com.lifetracker.knowledge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public final class Requests {
  private Requests() {}

  public record Reading(
      @NotNull UUID requestId,
      LocalDate startDate,
      LocalDate completionDate,
      @NotNull LocalDate localDate,
      @Size(max = 20000) String review) {}

  public record Create(
      @NotNull UUID requestId,
      @NotNull @Valid Metadata metadata,
      @NotNull @Valid Reading reading,
      UUID drawingId,
      UUID uploadId,
      Boolean confirmPossibleDuplicate) {}

  public record EditMetadata(@NotNull Long version, @NotNull @Valid Metadata metadata) {}

  public record EditReading(
      @NotNull Long version,
      LocalDate startDate,
      @NotNull LocalDate completionDate,
      @Size(max = 20000) String review) {}

  public record Attach(@NotNull UUID artworkId) {}
}
