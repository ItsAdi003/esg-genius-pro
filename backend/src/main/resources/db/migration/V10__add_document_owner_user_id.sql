-- V10: Per-user document ownership. NULL means a shared sample visible to every signed-in user.
-- Compatible with PostgreSQL and H2 (PostgreSQL mode)

ALTER TABLE document ADD COLUMN owner_user_id UUID;

CREATE INDEX idx_document_owner_user_id ON document (owner_user_id);
