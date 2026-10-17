CREATE TABLE users (
 id uuid PRIMARY KEY, email varchar(254) NOT NULL UNIQUE, password_hash varchar(255) NOT NULL,
 verified_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE account_tokens (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 purpose varchar(20) NOT NULL, token_hash varchar(64) NOT NULL UNIQUE,
 expires_at timestamptz NOT NULL, consumed_at timestamptz
);
CREATE INDEX account_tokens_expiry ON account_tokens(expires_at);
CREATE TABLE books (
 id uuid PRIMARY KEY, title varchar(500) NOT NULL, subtitle varchar(500), authors jsonb NOT NULL DEFAULT '[]',
 publisher varchar(500), publication_date varchar(100), language varchar(40), page_count integer CHECK (page_count > 0),
 cover_url varchar(2000), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE book_identifiers (
 id uuid PRIMARY KEY, book_id uuid NOT NULL REFERENCES books(id), kind varchar(20) NOT NULL,
 value varchar(40) NOT NULL, UNIQUE(kind,value)
);
CREATE TABLE book_sources (
 id uuid PRIMARY KEY, book_id uuid NOT NULL REFERENCES books(id), provider varchar(30) NOT NULL,
 edition_id varchar(100) NOT NULL, work_id varchar(100), UNIQUE(provider,edition_id)
);
CREATE TABLE library_entries (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id), book_id uuid NOT NULL REFERENCES books(id),
 reading_status varchar(30) NOT NULL CHECK (reading_status IN ('WANT_TO_READ','CURRENTLY_READING','COMPLETED')),
 date_added timestamptz NOT NULL DEFAULT now(), metadata_overrides jsonb NOT NULL DEFAULT '{}',
 spine_color varchar(7) NOT NULL, spine_width integer NOT NULL, spine_height integer NOT NULL,
 spine_style_version integer NOT NULL DEFAULT 1, version bigint NOT NULL DEFAULT 0,
 request_id uuid NOT NULL, UNIQUE(user_id,book_id), UNIQUE(user_id,request_id)
);
CREATE INDEX library_owner_sort ON library_entries(user_id,reading_status,date_added,id);
CREATE TABLE reading_sessions (
 id uuid PRIMARY KEY, library_entry_id uuid NOT NULL REFERENCES library_entries(id) ON DELETE CASCADE,
 start_date date, completion_date date NOT NULL, review varchar(20000), version bigint NOT NULL DEFAULT 0,
 request_id uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 CHECK (start_date IS NULL OR completion_date >= start_date), UNIQUE(library_entry_id,request_id)
);
CREATE INDEX session_entry ON reading_sessions(library_entry_id);
CREATE TABLE artwork (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES users(id),
 library_entry_id uuid REFERENCES library_entries(id) ON DELETE CASCADE,
 artwork_type varchar(10) NOT NULL CHECK (artwork_type IN ('DRAWING','UPLOAD')),
 storage_key varchar(200) NOT NULL UNIQUE, mime_type varchar(30) NOT NULL,
 byte_size bigint NOT NULL, width integer NOT NULL, height integer NOT NULL,
 color varchar(7) NOT NULL, expires_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(library_entry_id,artwork_type)
);
CREATE INDEX artwork_expiry ON artwork(expires_at);
CREATE TABLE storage_cleanup (id uuid PRIMARY KEY, storage_key varchar(200) NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE spring_session (
 primary_id char(36) PRIMARY KEY, session_id char(36) NOT NULL UNIQUE,
 creation_time bigint NOT NULL, last_access_time bigint NOT NULL, max_inactive_interval integer NOT NULL,
 expiry_time bigint NOT NULL, principal_name varchar(100)
);
CREATE INDEX spring_session_expiry ON spring_session(expiry_time);
CREATE INDEX spring_session_principal ON spring_session(principal_name);
CREATE TABLE spring_session_attributes (
 session_primary_id char(36) NOT NULL REFERENCES spring_session(primary_id) ON DELETE CASCADE,
 attribute_name varchar(200) NOT NULL, attribute_bytes bytea NOT NULL,
 PRIMARY KEY(session_primary_id,attribute_name)
);
