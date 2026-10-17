# Life Tracker — Knowledge Library
## Project Specification & Codex Development Instructions

**Project Name:** Life Tracker  
**Module:** Knowledge Library  
**Version:** 1.0 — Initial Implementation  
**Application Type:** Full-stack web application

---

# 1. Project Overview

Create a full-stack web application named **Life Tracker**.

Life Tracker is intended to become a personal tracking platform with multiple modules covering different aspects of life.

The first module to develop is **Knowledge Library**, an interactive virtual library designed to encourage reading and provide users with a visually rewarding way to track completed books.

Instead of a conventional table or list of books, users will see a cozy virtual library containing physical-looking bookshelves.

Initially, all shelves will be empty.

Whenever a user finishes reading a book and records it, a corresponding book will appear on the shelf, with its spine facing outward.

Over time, the user's virtual library will grow, visually representing their reading accomplishments.

### Primary Objectives

- Encourage consistent reading habits.
- Make recording completed books enjoyable.
- Allow users to maintain personal book reviews and reading histories.
- Provide convenient book discovery using external metadata sources.
- Enable book-cover personalization through uploads or drawings.
- Build an immersive, interactive library experience.
- Maintain a scalable architecture for future Life Tracker modules.

# 2. Technology Stack

Use the following stack for a new project unless the existing repository contains established conventions that should be preserved.

| Component | Technology |
|---|---|
| Frontend | React + TypeScript |
| Build tool | Vite |
| Styling | Tailwind CSS |
| Animations | CSS animations / Framer Motion |
| Backend | Java Spring Boot |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Book metadata | Google Books API and Open Library API |
| Image storage | Local filesystem for development; object storage compatible architecture |
| Frontend testing | Vitest + React Testing Library |
| Backend testing | JUnit 5 + Mockito |
| End-to-end testing | Playwright |
| API format | REST / JSON |

### Architectural Requirements

- Separate frontend and backend concerns.
- Use reusable React components.
- Organize backend code into controllers, services, repositories, entities, and DTOs.
- Keep external book provider integrations modular.
- Implement appropriate validation and error handling.
- Do not hardcode secrets or API credentials.
- Use environment variables for configuration.
- Use database migrations for schema changes.
- Keep the architecture extensible for additional Life Tracker modules.

# 3. User Interface Design

## 3.1 Main Knowledge Library Page

Create an immersive virtual library with a cozy, aesthetically pleasing appearance.

### Layout

The main page should contain:

1. A zoomed-in view of wooden bookshelves.
2. Several horizontal shelves capable of holding books.
3. An initially empty bookcase for new users.
4. A computer workstation positioned beneath the bookcase.
5. A computer monitor, keyboard, and mouse.
6. Subtle decorative elements that evoke a warm reading environment.

### Visual Direction

The visual experience should feel closer to a cozy game than an administrative dashboard.

Suggested aesthetic:

- Warm wooden textures
- Soft ambient lighting
- Comfortable library atmosphere
- Subtle shadows and depth
- Gentle transitions
- Small interactive animations
- Carefully designed book spines

Use a performant 2D or 2.5D rendering approach initially.

Full 3D rendering is not required.

Prioritize responsiveness and browser performance.

## 3.2 Computer Interaction

The computer workstation serves as the primary entry point for adding books.

When users click the computer, keyboard, or designated interactive area:

- Open an Add Book interface.
- Display the book-search form.
- Allow users to search for and register a book.

Provide a clearly visible Add Book button as an alternative for mobile users and accessibility.

## 3.3 Bookshelf Behavior

Every completed book saved by a user should appear physically on the bookshelf.

Requirements:

- Display books vertically with their spines facing outward.
- Show the book title on the spine whenever space permits.
- Optionally display author names.
- Use different book heights, widths, colors, and typography.
- Maintain consistent visual properties across sessions.
- Arrange books automatically.
- Prevent books from overlapping.
- Place books on subsequent shelves when a shelf becomes full.
- Expand the available bookshelf space when necessary.
- Allow users to click books to view details.
- Support keyboard and touch interaction.

### Shelf Animations

When adding a new book:

1. Save the book successfully.
2. Update the bookshelf.
3. Animate the new book sliding or appearing on the shelf.
4. Display subtle success feedback.

When hovering over a book, it may tilt, move slightly outward, or become highlighted.

Respect reduced-motion accessibility preferences.

# 4. Add Book Workflow

## 4.1 Opening the Form

Clicking the computer should open a modal, panel, or dedicated interface for adding books.

The interface should have a clear, guided workflow:

1. Search for a book.
2. Select and confirm a matching result.
3. Review available metadata.
4. Enter personal reading information.
5. Customize the cover if desired.
6. Submit the book to the library.

## 4.2 Book Search Fields

Allow the user to provide:

| Field | Requirement |
|---|---|
| Title | Optional during search; required for manual save |
| Author | Optional |
| ISBN-10 | Optional |
| ISBN-13 | Optional |

At least one meaningful search field must be populated before performing a search.

Validate ISBN formats when supplied.

## 4.3 Book Search APIs

Integrate with:

- Google Books API
- Open Library API

Implement these integrations through separate provider adapters.

### Search Requirements

- Search by title.
- Search by author.
- Search by ISBN.
- Support combined queries where the provider allows.
- Support partial title matches.
- Debounce searches where appropriate.
- Display multiple candidate results.
- Provide a clear loading state.
- Handle provider downtime and rate limits.
- Handle empty results gracefully.
- Allow manual book entry.

Do not automatically select the first result.

### Search Result Display

Each search result should display available information:

- Book cover thumbnail
- Title
- Author(s)
- Publication year
- ISBN
- Publisher, if available

Users should explicitly confirm their chosen book.

## 4.4 Book Metadata

Retrieve metadata where available:

- Book title
- Subtitle
- Author(s)
- ISBN-10
- ISBN-13
- Publisher
- Publication date
- Language
- Page count
- Publisher description
- Book cover image
- Metadata provider name
- Provider-specific book identifier

Metadata fields should be optional when the provider does not supply them.

Do not fabricate missing information.

Allow the user to correct inaccurate metadata before saving.

## 4.5 No Matching Book Found

If neither provider returns a suitable result:

- Inform the user that no matching book was found.
- Allow the user to enter book information manually.
- Provide a default cover template.
- Allow an image upload or custom drawing.

A missing API result should never prevent manual book registration.

# 5. Book Description and Summary

After a book is selected, display its available description.

### Requirements

- Prefer publisher-supplied descriptions where reliably identifiable.
- Show the source of the description.
- Do not claim a summary is official unless its provenance is verified.
- Do not generate or invent descriptions when none exists.
- Display a fallback message when a description is unavailable.
- Allow books to be saved without descriptions.

### No-Cache Requirement

Externally sourced book descriptions must be retrieved on demand.

Do not intentionally persist or cache description text in:

- PostgreSQL
- Browser local storage
- Browser session storage
- Service workers
- Application-level caches

Store only the provider identifiers necessary to retrieve the description again.

Respect external API terms, attribution rules, and cache-control headers.

Personal reviews are separate from external descriptions and must be persisted.

# 6. Reading Information

Once the user confirms a book, allow them to record their reading experience.

## 6.1 Reading Dates

Provide:

- Start date (optional)
- Completion date (optional)

For a Completed book, if the completion date is blank, automatically use the user's current local calendar date when saving.

Validate that the completion date does not precede the start date.

Allow reading dates to be updated later.

## 6.2 Personal Book Review

Provide a multiline text area where the user can write their thoughts.

Requirements:

- Review is optional.
- Preserve paragraph breaks.
- Allow the user to leave the review blank.
- Allow the user to edit an existing review.
- Allow the user to delete or clear a review.
- Persist all changes.
- Never overwrite user reviews with external descriptions.

Rich-text formatting is not required for version 1.

## 6.3 Reading Status

Support these statuses in the data model:

- Want to Read
- Currently Reading
- Completed

For the initial Knowledge Library workflow, default newly registered books to Completed.

Only Completed books should appear on the main bookshelf in version 1.

Other reading statuses should be available for future extensions.

## 6.4 Rereading Books

A user may read the same book multiple times.

The data model should support multiple reading sessions for a single book.

Each session may have its own:

- Start date
- Completion date
- Review

Avoid duplicating canonical book metadata when recording a reread.

Provide an Add Reread action within the book-detail interface.

# 7. Book Cover Customization

## 7.1 Cover Selection Priority

The cover should be determined using the following priority:

1. User-created drawing
2. User-uploaded image
3. Cover obtained from a supported book metadata provider
4. Standard fallback book-cover template

Do not scrape images from Google Image Search.

Use available cover images from Google Books and Open Library instead.

## 7.2 User-Uploaded Covers

Allow users to upload an image to use as their book cover.

Requirements:

- Support common safe image formats.
- Validate content type and file size.
- Provide image preview.
- Support replacing an existing image.
- Persist uploaded artwork.
- Handle upload errors.
- Prevent malicious or unsupported file uploads.

## 7.3 Drawing a Custom Cover

Provide an interactive drawing canvas.

Initial drawing tools:

- Pen
- Eraser
- Color picker
- Brush-size adjustment
- Undo
- Clear canvas

The user should be able to save the drawing and use it as the book's cover.

Drawings must persist across application refreshes and sessions.

## 7.4 Book Spine Generation

When the book appears on the shelf, generate a corresponding spine.

The spine should:

- Display the book title when possible.
- Use a color derived from the cover or a consistent fallback palette.
- Have realistic width and height.
- Use readable typography.
- Handle long titles gracefully.
- Preserve the same appearance across refreshes.

The full front cover should remain visible when viewing book details.

# 8. Saving a Book

When the user clicks **Add to Library**:

1. Validate the data.
2. Confirm that the book record is complete enough to save.
3. Persist book metadata.
4. Save the user's library entry.
5. Save reading dates and review.
6. Save custom artwork or image references.
7. Generate stable book-spine properties.
8. Confirm successful persistence.
9. Update the bookshelf immediately.
10. Play the book-placement animation.

### Error Handling

If the save fails:

- Display an appropriate error.
- Retain entered information.
- Allow the user to retry.
- Prevent duplicate submissions.
- Avoid partially created or inconsistent records.

# 9. Viewing Existing Books

Clicking a book on the shelf opens a Book Details interface.

Display:

- Front book cover
- Title
- Author(s)
- ISBN
- Publisher
- Publication information
- Page count
- External book description
- Reading status
- Reading dates
- Personal review
- Previous reading sessions

Only display metadata fields that have values.

### Supported Actions

Users should be able to:

- Edit reading dates.
- Create or update their review.
- Clear or delete their review.
- Replace the cover.
- Draw a new cover.
- Add another reading session.
- Delete a reading session.
- Delete the book from their library.

### Deletion

Deleting a book must require confirmation.

On deletion:

- Remove the user's library entry.
- Remove associated personal reading sessions.
- Remove owned custom artwork when appropriate.
- Remove the corresponding spine from the bookshelf.
- Rearrange remaining books naturally.

Do not delete shared canonical book metadata that may be referenced by other users.

# 10. Database Design

Separate book information from user-specific reading records.

## 10.1 Books

Suggested fields:

- id
- title
- subtitle
- authors
- isbn10
- isbn13
- publisher
- publication_date
- language
- page_count
- metadata_provider
- provider_book_id
- cover_url
- created_at
- updated_at

Do not store external book description text.

## 10.2 Library Entries

Suggested fields:

- id
- user_id
- book_id
- reading_status
- date_added
- spine_color
- spine_height
- spine_width
- created_at
- updated_at

## 10.3 Reading Sessions

Suggested fields:

- id
- library_entry_id
- start_date
- completion_date
- review
- created_at
- updated_at

## 10.4 Custom Artwork

Suggested fields:

- id
- library_entry_id
- artwork_type
- storage_reference
- mime_type
- created_at
- updated_at

### Data Integrity

- Use appropriate primary and foreign keys.
- Enforce uniqueness where necessary.
- Use transactions for related updates.
- Implement appropriate cascading behavior.
- Prevent duplicate library entries for the same book and user.
- Handle manual entries with missing ISBNs.
- Keep schema extensible for future multi-user functionality.

For the initial release, authentication may be deferred if the application is strictly local and single-user. However, do not expose unauthenticated write endpoints publicly.

# 11. Backend Architecture

Use Spring Boot with a modular layered architecture.

Suggested package organization:

```text
backend/
  src/main/java/.../
    config/
    controller/
    service/
    repository/
    entity/
    dto/
    validation/
    exception/
    provider/
      googlebooks/
      openlibrary/
    storage/
```

### Backend Responsibilities

- Expose REST endpoints.
- Validate requests.
- Search metadata providers.
- Normalize external API results.
- Manage book metadata.
- Manage library entries.
- Manage reading sessions.
- Manage cover artwork.
- Enforce data integrity.
- Handle external service failures.
- Return consistent API errors.

### API Considerations

Potential endpoints:

```text
GET    /api/books/search
POST   /api/library/books
GET    /api/library/books
GET    /api/library/books/{id}
PATCH  /api/library/books/{id}
DELETE /api/library/books/{id}

POST   /api/library/books/{id}/sessions
PATCH  /api/library/books/{id}/sessions/{sessionId}
DELETE /api/library/books/{id}/sessions/{sessionId}

POST   /api/library/books/{id}/artwork
DELETE /api/library/books/{id}/artwork

GET    /api/library/books/{id}/description
```

These are suggested API contracts, not rigid requirements.

Use appropriate HTTP status codes and structured validation errors.

# 12. Frontend Architecture

Use React, TypeScript, and Tailwind CSS.

Suggested component structure:

```text
frontend/
  src/
    components/
      library/
        Bookcase.tsx
        Bookshelf.tsx
        BookSpine.tsx
        ComputerDesk.tsx
        BookDetails.tsx
        AddBookModal.tsx
        BookSearch.tsx
        BookSearchResults.tsx
        BookReviewEditor.tsx
        CoverCustomizer.tsx
        DrawingCanvas.tsx
    pages/
      KnowledgeLibraryPage.tsx
    hooks/
    services/
    types/
    utils/
    assets/
```

### Frontend Requirements

- Use reusable components.
- Separate API calls from UI rendering.
- Implement loading, empty, and error states.
- Ensure responsive layouts.
- Avoid unnecessary rerenders.
- Use stable identifiers.
- Provide accessible interactions.
- Maintain predictable state management.
- Use optimistic updates only when rollback is handled correctly.

# 13. Testing Requirements

Implement automated tests covering the primary workflow.

### Backend Tests

- ISBN validation
- Metadata provider integration logic
- Provider fallback
- Book creation
- Reading-session creation
- Review updates
- Book deletion
- Transaction handling
- Invalid input
- Duplicate prevention

### Frontend Tests

- Empty library display
- Add Book interaction
- Search-result selection
- Metadata confirmation
- Manual book entry
- Date defaults
- Review editing
- Cover fallback
- Book-detail display
- Shelf placement and overflow
- Save failures

### End-to-End Tests

At minimum, verify:

1. A user opens an empty Knowledge Library.
2. A user searches for a book.
3. A user selects or manually enters a book.
4. A user adds reading information.
5. A user saves the book.
6. The book appears on the shelf.
7. The user refreshes the page.
8. The book remains on the shelf.
9. The user edits the review.
10. The user deletes the book.
11. The book disappears from the shelf.

Mock external APIs during automated testing wherever appropriate.

# 14. Performance and Accessibility

### Performance

- Avoid unnecessary external API requests.
- Debounce appropriate search interactions.
- Use sensible request timeouts.
- Avoid excessive DOM complexity.
- Keep bookshelf rendering performant as the library grows.
- Lazy-load nonessential images where appropriate.
- Avoid storing unnecessary large image payloads in the database.

### Accessibility

- Support keyboard navigation.
- Provide accessible names for book spines.
- Provide visible focus indicators.
- Support screen readers.
- Respect reduced-motion settings.
- Maintain sufficient text contrast.
- Provide alternatives to purely visual interactions.
- Ensure the interface works on touch devices.

# 15. Implementation Plan

Implement the project incrementally.

## Phase 1 — Foundation

- Initialize frontend and backend.
- Configure PostgreSQL.
- Create database migrations.
- Implement basic application routing.
- Build the Life Tracker application shell.
- Create the Knowledge Library page.
- Establish the API structure.

**Acceptance:** The application runs locally with a functional frontend, backend, and database connection.

## Phase 2 — Book Management

- Integrate Google Books and Open Library.
- Implement book search.
- Implement result selection.
- Implement manual entry.
- Implement book metadata persistence.
- Implement reviews and reading dates.
- Implement book CRUD operations.
- Implement external description retrieval without application-level caching.

**Acceptance:** Users can add, view, edit, and delete books persistently through a functional interface.

## Phase 3 — Interactive Bookshelf

- Create virtual wooden bookcases.
- Render empty shelves.
- Create spine components.
- Implement automatic book placement.
- Handle shelf overflow.
- Implement clicking books for details.
- Add book-placement animations.

**Acceptance:** Completed books appear as interactive spines, persist after refresh, and rearrange correctly.

## Phase 4 — Cover Customization

- Implement uploaded covers.
- Implement drawing canvas.
- Implement standard fallback covers.
- Connect cover customization to spine appearance.
- Handle image replacement and deletion.

**Acceptance:** Users can personalize their book covers and retain the customizations after refresh.

## Phase 5 — Testing and Polish

- Expand automated test coverage.
- Test API failures and edge cases.
- Refine animations.
- Test mobile responsiveness.
- Improve accessibility.
- Optimize performance.
- Finalize documentation.

**Acceptance:** The main end-to-end workflow is stable and reproducible.

# 16. Overall Acceptance Criteria

The Knowledge Library is considered functionally complete when:

- [ ] A new user sees an empty virtual library.
- [ ] The computer opens the Add Book interface.
- [ ] Book search returns relevant available results.
- [ ] The user can confirm the correct book.
- [ ] Manual entry works when search fails.
- [ ] External descriptions are fetched on demand, not stored.
- [ ] Reviews are optional and editable.
- [ ] Completion dates default to today's local date when omitted.
- [ ] Users can upload or draw custom covers.
- [ ] Fallback covers work.
- [ ] Saving a book creates a persistent library entry.
- [ ] The saved book appears on a bookshelf.
- [ ] The spine faces outward with its title visible where possible.
- [ ] Clicking a spine opens book details.
- [ ] The user can edit and delete reading records.
- [ ] Rereads can be recorded.
- [ ] The bookshelf remains consistent after page reload.
- [ ] Books do not overlap or overflow incorrectly.
- [ ] The application handles external API outages gracefully.
- [ ] Relevant automated tests pass.

# 17. Future Enhancements

The following are out of scope for the initial implementation but should be considered when designing extensible architecture.

### Reading Statistics

- Books completed per month
- Books completed per year
- Total pages read
- Reading streaks
- Annual reading goals
- Genre breakdown

### Gamification

- Reading achievements
- Milestone celebrations
- Unlockable decorations
- Library room upgrades
- Reading challenges

### Library Organization

- Custom bookshelf categories
- Books sorted by author or genre
- Multiple virtual library rooms
- Search and filter within the library
- Want-to-read and currently-reading shelves

### Personal Knowledge Management

- Quotes and highlights
- Reading reflections
- Notes associated with specific books
- Linking related books and ideas

### Data Portability

- CSV import and export
- Backup and restore
- Import from compatible book-tracking platforms

### Future Life Tracker Modules

- Fitness tracking
- Learning progress
- Personal goals
- Hobbies and skills
- Habit tracking
- Personal achievements

These modules should remain separate from Knowledge Library while sharing the core application infrastructure.

# 18. Instructions for Codex

Before writing code:

1. Inspect the existing project files and architecture.
2. Identify whether this is a new or existing project.
3. Propose the implementation approach.
4. Identify dependencies and environment configuration.
5. Define the database schema.
6. Outline the REST API contracts.
7. Identify risks and assumptions.
8. Create an `IMPLEMENTATION_PLAN.md` file.

The implementation plan should include:

- Project architecture
- Development phases
- Task checklists
- Dependencies
- Acceptance criteria
- Testing requirements
- Known limitations

### Implementation Rules

- Follow the implementation phases in order.
- Maintain the checklist as progress is made.
- Keep the application runnable at the end of each phase.
- Prioritize working functionality over decorative polish.
- Do not silently omit features.
- Do not use static mocks in place of required functionality.
- Use real persistence for saved user data.
- Keep code modular and maintainable.
- Run relevant tests after significant changes.
- Fix failing tests before declaring a phase complete.
- Document configuration requirements.
- Avoid unnecessary dependencies or architectural complexity.
- Clearly document any incomplete or deferred functionality.

### Expected Initial Deliverables

1. Application architecture proposal
2. Implementation plan
3. Database schema
4. REST API design
5. Frontend component structure
6. Working application implementation
7. Automated tests
8. README with setup and execution instructions

**Final Goal:** Deliver a functional, visually engaging, persistent virtual reading library that rewards users for completing books and forms the foundation of the broader Life Tracker application.