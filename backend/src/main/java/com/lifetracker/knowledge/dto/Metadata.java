package com.lifetracker.knowledge.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record Metadata(
    @NotBlank @Size(max = 500) String title,
    @Size(max = 500) String subtitle,
    @NotNull @Size(max = 30) List<@NotBlank @Size(max = 300) String> authors,
    @Size(max = 40) String isbn10,
    @Size(max = 40) String isbn13,
    @Size(max = 500) String publisher,
    @Size(max = 100) String publicationDate,
    @Size(max = 40) String language,
    @Positive @Max(100000) Integer pageCount,
    @Size(max = 2000) String coverUrl,
    @Size(max = 30) String provider,
    @Size(max = 100) String editionId,
    @Size(max = 100) String workId) {}
