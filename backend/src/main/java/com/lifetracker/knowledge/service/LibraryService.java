package com.lifetracker.knowledge.service;

import com.lifetracker.identity.UserRepository;
import com.lifetracker.knowledge.dto.*;
import com.lifetracker.knowledge.entity.*;
import com.lifetracker.knowledge.provider.*;
import com.lifetracker.knowledge.repository.*;
import com.lifetracker.knowledge.validation.Isbn;
import com.lifetracker.shared.ApiException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LibraryService {
  private final UserRepository users;
  private final Books books;
  private final Identifiers identifiers;
  private final Sources sources;
  private final Entries entries;
  private final Sessions sessions;
  private final Artworks artworks;
  private final ArtworkService art;
  private final ProviderService providers;

  public LibraryService(
      UserRepository users,
      Books books,
      Identifiers identifiers,
      Sources sources,
      Entries entries,
      Sessions sessions,
      Artworks artworks,
      ArtworkService art,
      ProviderService providers) {
    this.users = users;
    this.books = books;
    this.identifiers = identifiers;
    this.sources = sources;
    this.entries = entries;
    this.sessions = sessions;
    this.artworks = artworks;
    this.art = art;
    this.providers = providers;
  }

  public UUID owner(String email) {
    return users.findByEmail(email).orElseThrow(ApiException::missing).id;
  }

  public LibraryEntry owned(UUID owner, UUID id) {
    return entries.findByIdAndUserId(id, owner).orElseThrow(ApiException::missing);
  }

  public static String palette(UUID book) {
    String[] colors = {"#735149", "#3e6560", "#805f38", "#595877", "#8a493e", "#4d6579", "#647047"};
    return colors[Math.floorMod(book.hashCode(), colors.length)];
  }

  private void version(long actual, Long expected) {
    if (expected == null || actual != expected)
      throw ApiException.conflict("This record changed. Reopen it before saving.");
  }

  private Map<String, Object> overrides(Metadata m) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("title", m.title().trim());
    data.put("subtitle", m.subtitle());
    data.put("authors", m.authors());
    data.put("publisher", m.publisher());
    data.put("publicationDate", m.publicationDate());
    data.put("language", m.language());
    data.put("pageCount", m.pageCount());
    return data;
  }

  private void checkMetadata(Metadata m) {
    String i10 = Isbn.normalize(m.isbn10()), i13 = Isbn.normalize(m.isbn13());
    if (i10 != null && i10.length() != 10)
      throw ApiException.bad("ISBN-10 must have 10 characters");
    if (i13 != null && i13.length() != 13)
      throw ApiException.bad("ISBN-13 must have 13 characters");
    if (i10 != null && i13 != null && !Isbn.to13(i10).equals(i13))
      throw ApiException.bad("ISBN-10 and ISBN-13 identify different editions");
    if (m.provider() != null && !Set.of("googlebooks", "openlibrary").contains(m.provider()))
      throw ApiException.bad("Unsupported provider");
    if ((m.provider() == null) != (m.editionId() == null))
      throw ApiException.bad("Provider and edition identifier must be supplied together");
    if (m.editionId() != null
        && !m.editionId()
            .matches(m.provider().equals("openlibrary") ? "OL[0-9]+M" : "[A-Za-z0-9_-]{1,100}"))
      throw ApiException.bad("Invalid edition identifier");
    if (m.workId() != null && !m.workId().matches("OL[0-9]+W"))
      throw ApiException.bad("Invalid work identifier");
  }

  private Book resolve(Metadata m) {
    checkMetadata(m);
    Set<UUID> matches = new HashSet<>();
    String i10 = Isbn.normalize(m.isbn10()), i13 = Isbn.normalize(m.isbn13());
    if (i13 == null && i10 != null) i13 = Isbn.to13(i10);
    if (i10 != null)
      identifiers.findByKindAndValue("ISBN10", i10).ifPresent(i -> matches.add(i.bookId));
    if (i13 != null)
      identifiers.findByKindAndValue("ISBN13", i13).ifPresent(i -> matches.add(i.bookId));
    if (m.provider() != null)
      sources
          .findByProviderAndEditionId(m.provider(), m.editionId())
          .ifPresent(s -> matches.add(s.bookId));
    if (matches.size() > 1)
      throw ApiException.conflict(
          "Identifiers conflict with existing editions. Correct the ISBN or select another"
              + " edition.");
    Book book;
    if (matches.isEmpty()) {
      book = new Book();
      book.title = m.title().trim();
      book.subtitle = m.subtitle();
      book.authors = m.authors();
      book.publisher = m.publisher();
      book.publicationDate = m.publicationDate();
      book.language = m.language();
      book.pageCount = m.pageCount();
      book.coverUrl = ProviderHttp.safeCover(m.coverUrl());
      books.saveAndFlush(book);
    } else book = books.findById(matches.iterator().next()).orElseThrow(ApiException::missing);
    addIdentifier(book.id, "ISBN10", i10);
    addIdentifier(book.id, "ISBN13", i13);
    if (m.provider() != null
        && sources.findByProviderAndEditionId(m.provider(), m.editionId()).isEmpty()) {
      BookSource s = new BookSource();
      s.bookId = book.id;
      s.provider = m.provider();
      s.editionId = m.editionId();
      s.workId = m.workId();
      sources.save(s);
    }
    return book;
  }

  private void addIdentifier(UUID book, String kind, String value) {
    if (value != null && identifiers.findByKindAndValue(kind, value).isEmpty()) {
      BookIdentifier i = new BookIdentifier();
      i.bookId = book;
      i.kind = kind;
      i.value = value;
      identifiers.save(i);
    }
  }

  private Metadata verifiedIdentity(Metadata requested) {
    checkMetadata(requested);
    if (requested.provider() == null) return requested;
    Metadata verified = providers.details(requested.provider(), requested.editionId());
    String requested13 = Isbn.normalize(requested.isbn13());
    String requested10 = Isbn.normalize(requested.isbn10());
    if (requested13 == null && requested10 != null) requested13 = Isbn.to13(requested10);
    String verified13 = Isbn.normalize(verified.isbn13());
    String verified10 = Isbn.normalize(verified.isbn10());
    if (verified13 == null && verified10 != null) verified13 = Isbn.to13(verified10);
    if (requested13 != null && !Objects.equals(requested13, verified13)) {
      throw ApiException.bad(
          "The ISBN does not match the selected provider edition. Use manual entry to correct its"
              + " identity.");
    }
    // Never let a client attach arbitrary ISBN/provider aliases to shared canonical books.
    return new Metadata(
        requested.title(),
        requested.subtitle(),
        requested.authors(),
        verified10,
        verified13,
        requested.publisher(),
        requested.publicationDate(),
        requested.language(),
        requested.pageCount(),
        verified.coverUrl(),
        requested.provider(),
        requested.editionId(),
        verified.workId());
  }

  public LibraryView create(UUID owner, Requests.Create request) {
    var previous = entries.findByUserIdAndRequestId(owner, request.requestId());
    if (previous.isPresent()) return view(previous.get());
    Metadata m = request.metadata();
    if (m.provider() == null
        && m.isbn10() == null
        && m.isbn13() == null
        && !Boolean.TRUE.equals(request.confirmPossibleDuplicate())) {
      boolean possible = entries.existsPossibleDuplicate(owner, m.title().trim());
      if (possible)
        throw ApiException.conflict(
            "A book with this title is already in your library. Confirm that this is a different"
                + " edition.");
    }
    Book book = resolve(verifiedIdentity(m));
    if (entries.findByUserIdAndBookId(owner, book.id).isPresent())
      throw ApiException.conflict(
          "This edition is already in your library. Use Add reread instead.");
    LibraryEntry e = new LibraryEntry();
    e.userId = owner;
    e.bookId = book.id;
    e.requestId = request.requestId();
    e.metadataOverrides = overrides(m);
    e.spineColor = palette(book.id);
    e.spineWidth = 38 + Math.floorMod(book.id.hashCode(), 18);
    e.spineHeight = 174 + Math.floorMod(book.id.hashCode(), 49);
    entries.saveAndFlush(e);
    addSession(owner, e.id, request.reading());
    if (request.uploadId() != null) art.attach(owner, e, "UPLOAD", request.uploadId());
    if (request.drawingId() != null) art.attach(owner, e, "DRAWING", request.drawingId());
    entries.flush();
    return view(e);
  }

  @Transactional(readOnly = true)
  public LibraryView.Page list(UUID owner, int page, int size) {
    if (page < 0 || page > 100000 || size < 1 || size > 100)
      throw ApiException.bad("Invalid pagination");
    var result =
        entries.findByUserIdAndReadingStatusOrderByDateAddedDescIdDesc(
            owner, "COMPLETED", PageRequest.of(page, size));
    if (result.isEmpty()) return new LibraryView.Page(List.of(), false, result.getTotalElements());
    var bookIds = result.stream().map(e -> e.bookId).toList();
    var entryIds = result.stream().map(e -> e.id).toList();
    var bookMap = books.findAllById(bookIds).stream().collect(Collectors.toMap(b -> b.id, b -> b));
    var idMap =
        identifiers.findByBookIdIn(bookIds).stream().collect(Collectors.groupingBy(i -> i.bookId));
    var sourceMap =
        sources.findByBookIdIn(bookIds).stream().collect(Collectors.groupingBy(s -> s.bookId));
    var artMap =
        artworks.findByLibraryEntryIdIn(entryIds).stream()
            .collect(Collectors.groupingBy(a -> a.libraryEntryId));
    var sessionMap =
        sessions.findByLibraryEntryIdInOrderByCompletionDateDescCreatedAtDesc(entryIds).stream()
            .collect(Collectors.groupingBy(s -> s.libraryEntryId));
    return new LibraryView.Page(
        result.stream()
            .map(
                e ->
                    view(
                        e,
                        bookMap.get(e.bookId),
                        idMap.getOrDefault(e.bookId, List.of()),
                        sourceMap.getOrDefault(e.bookId, List.of()),
                        artMap.getOrDefault(e.id, List.of()),
                        sessionMap.getOrDefault(e.id, List.of())))
            .toList(),
        result.hasNext(),
        result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public LibraryView get(UUID owner, UUID id) {
    return view(owned(owner, id));
  }

  public LibraryView edit(UUID owner, UUID id, Requests.EditMetadata request) {
    LibraryEntry e = owned(owner, id);
    version(e.version, request.version());
    Metadata m = request.metadata();
    checkMetadata(m);
    Metadata current = view(e).metadata();
    if (!Objects.equals(Isbn.normalize(current.isbn10()), Isbn.normalize(m.isbn10()))
        || !Objects.equals(Isbn.normalize(current.isbn13()), Isbn.normalize(m.isbn13())))
      throw ApiException.bad("ISBN identity cannot be changed. Add the correct edition instead.");
    e.metadataOverrides = overrides(m);
    entries.flush();
    return view(e);
  }

  public void delete(UUID owner, UUID id) {
    LibraryEntry e = owned(owner, id);
    artworks.findByLibraryEntryId(id).forEach(art::discard);
    artworks.flush();
    entries.delete(e);
  }

  private void dates(LocalDate start, LocalDate completion) {
    if (completion == null) throw ApiException.bad("Completion date is required");
    if (start != null && completion.isBefore(start))
      throw ApiException.bad("Completion date cannot precede start date");
  }

  public LibraryView addSession(UUID owner, UUID id, Requests.Reading request) {
    LibraryEntry e = owned(owner, id);
    if (sessions.findByLibraryEntryIdAndRequestId(id, request.requestId()).isPresent())
      return view(e);
    ReadingSession s = new ReadingSession();
    s.libraryEntryId = id;
    s.requestId = request.requestId();
    s.startDate = request.startDate();
    s.completionDate =
        request.completionDate() == null ? request.localDate() : request.completionDate();
    s.review = request.review();
    dates(s.startDate, s.completionDate);
    sessions.saveAndFlush(s);
    return view(e);
  }

  public LibraryView editSession(
      UUID owner, UUID id, UUID sessionId, Requests.EditReading request) {
    LibraryEntry e = owned(owner, id);
    ReadingSession s =
        sessions.findByIdAndLibraryEntryId(sessionId, id).orElseThrow(ApiException::missing);
    version(s.version, request.version());
    dates(request.startDate(), request.completionDate());
    s.startDate = request.startDate();
    s.completionDate = request.completionDate();
    s.review = request.review();
    sessions.flush();
    return view(e);
  }

  public void deleteSession(UUID owner, UUID id, UUID sessionId) {
    entries.lockOwned(id, owner).orElseThrow(ApiException::missing);
    ReadingSession s =
        sessions.findByIdAndLibraryEntryId(sessionId, id).orElseThrow(ApiException::missing);
    // Serialize final-session decisions against concurrent deletes.
    if (sessions.countByLibraryEntryId(id) <= 1)
      throw ApiException.conflict(
          "The final reading session cannot be removed. Delete the book instead.");
    sessions.delete(s);
  }

  public LibraryView attach(UUID owner, UUID id, String type, UUID artId) {
    LibraryEntry e = owned(owner, id);
    art.attach(owner, e, type, artId);
    entries.flush();
    return view(e);
  }

  public LibraryView removeArtwork(UUID owner, UUID id, String type) {
    LibraryEntry e = owned(owner, id);
    art.remove(e, type);
    entries.flush();
    return view(e);
  }

  private LibraryView view(LibraryEntry e) {
    Book b = books.findById(e.bookId).orElseThrow(ApiException::missing);
    return view(
        e,
        b,
        identifiers.findByBookId(b.id),
        sources.findByBookId(b.id),
        artworks.findByLibraryEntryId(e.id),
        sessions.findByLibraryEntryIdOrderByCompletionDateDescCreatedAtDesc(e.id));
  }

  @SuppressWarnings("unchecked")
  private LibraryView view(
      LibraryEntry e,
      Book b,
      List<BookIdentifier> ids,
      List<BookSource> bookSources,
      List<Artwork> artwork,
      List<ReadingSession> readings) {
    Map<String, Object> o = e.metadataOverrides;
    String i10 =
        ids.stream()
            .filter(i -> i.kind.equals("ISBN10"))
            .map(i -> i.value)
            .findFirst()
            .orElse(null);
    String i13 =
        ids.stream()
            .filter(i -> i.kind.equals("ISBN13"))
            .map(i -> i.value)
            .findFirst()
            .orElse(null);
    BookSource source =
        bookSources.stream().sorted(Comparator.comparing(s -> s.provider)).findFirst().orElse(null);
    Metadata m =
        new Metadata(
            (String) o.getOrDefault("title", b.title),
            (String) o.getOrDefault("subtitle", b.subtitle),
            (List<String>) o.getOrDefault("authors", b.authors),
            i10,
            i13,
            (String) o.getOrDefault("publisher", b.publisher),
            (String) o.getOrDefault("publicationDate", b.publicationDate),
            (String) o.getOrDefault("language", b.language),
            (Integer) o.getOrDefault("pageCount", b.pageCount),
            b.coverUrl,
            source == null ? null : source.provider,
            source == null ? null : source.editionId,
            source == null ? null : source.workId);
    List<LibraryView.ArtView> images =
        artwork.stream()
            .sorted(Comparator.comparing(a -> a.artworkType.equals("DRAWING") ? 0 : 1))
            .map(
                a ->
                    new LibraryView.ArtView(
                        a.id, a.artworkType, "/api/v1/artwork/" + a.id + "/content"))
            .toList();
    String cover = images.isEmpty() ? b.coverUrl : images.getFirst().url();
    var history =
        readings.stream()
            .map(
                s ->
                    new LibraryView.SessionView(
                        s.id, s.version, s.startDate, s.completionDate, s.review))
            .toList();
    return new LibraryView(
        e.id,
        e.version,
        m,
        e.readingStatus,
        e.dateAdded,
        e.spineColor,
        e.spineWidth,
        e.spineHeight,
        cover,
        images,
        history);
  }
}
