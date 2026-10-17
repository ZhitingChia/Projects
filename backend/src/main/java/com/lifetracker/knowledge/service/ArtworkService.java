package com.lifetracker.knowledge.service;

import com.lifetracker.knowledge.entity.*;
import com.lifetracker.knowledge.repository.*;
import com.lifetracker.knowledge.storage.*;
import com.lifetracker.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtworkService {
  private final Artworks artworks;
  private final Cleanups cleanups;
  private final ArtworkStorage storage;
  private final ImageValidator images;

  public ArtworkService(
      Artworks artworks, Cleanups cleanups, ArtworkStorage storage, ImageValidator images) {
    this.artworks = artworks;
    this.cleanups = cleanups;
    this.storage = storage;
    this.images = images;
  }

  public static String type(String type) {
    String t = type.toUpperCase(Locale.ROOT);
    if (!Set.of("DRAWING", "UPLOAD").contains(t)) throw ApiException.bad("Invalid artwork type");
    return t;
  }

  @Transactional
  public Artwork stage(UUID owner, String type, byte[] bytes, String contentType) {
    ImageValidator.SafeImage image = images.validate(bytes, contentType);
    Artwork a = new Artwork();
    a.ownerId = owner;
    a.artworkType = type(type);
    a.storageKey = a.id + ".png";
    a.mimeType = "image/png";
    a.byteSize = image.bytes().length;
    a.width = image.width();
    a.height = image.height();
    a.color = image.color();
    a.expiresAt = Instant.now().plus(Duration.ofHours(24));
    storage.put(a.storageKey, image.bytes());
    // Register compensation for DB rollback, including staging failures after the file was written.
    org.springframework.transaction.support.TransactionSynchronizationManager
        .registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
              public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED)
                  try {
                    storage.delete(a.storageKey);
                  } catch (RuntimeException ignored) {
                  }
              }
            });
    return artworks.save(a);
  }

  @Transactional
  public void attach(UUID owner, LibraryEntry entry, String type, UUID id) {
    Artwork a = artworks.findByIdAndOwnerId(id, owner).orElseThrow(ApiException::missing);
    if (!a.artworkType.equals(type(type))) throw ApiException.bad("Artwork type mismatch");
    if (a.libraryEntryId != null && !a.libraryEntryId.equals(entry.id))
      throw ApiException.conflict("Artwork belongs to another book");
    if (a.expiresAt != null && a.expiresAt.isBefore(Instant.now()))
      throw ApiException.bad("Artwork upload expired; upload it again");
    artworks
        .findByLibraryEntryIdAndArtworkType(entry.id, a.artworkType)
        .filter(old -> !old.id.equals(a.id))
        .ifPresent(this::discard);
    artworks.flush();
    a.libraryEntryId = entry.id;
    a.expiresAt = null;
    artworks.save(a);
    updateColor(entry);
  }

  @Transactional
  public void remove(LibraryEntry entry, String type) {
    artworks.findByLibraryEntryIdAndArtworkType(entry.id, type(type)).ifPresent(this::discard);
    artworks.flush();
    updateColor(entry);
  }

  public void discard(Artwork a) {
    StorageCleanup cleanup = new StorageCleanup();
    cleanup.storageKey = a.storageKey;
    cleanups.save(cleanup);
    artworks.delete(a);
  }

  public void updateColor(LibraryEntry entry) {
    List<Artwork> art = artworks.findByLibraryEntryId(entry.id);
    entry.spineColor =
        art.stream()
            .sorted(Comparator.comparing(a -> a.artworkType.equals("DRAWING") ? 0 : 1))
            .findFirst()
            .map(a -> a.color)
            .orElseGet(() -> LibraryService.palette(entry.bookId));
  }

  @Transactional(readOnly = true)
  public byte[] content(UUID owner, UUID id) {
    Artwork a = artworks.findByIdAndOwnerId(id, owner).orElseThrow(ApiException::missing);
    return storage.read(a.storageKey);
  }
}
