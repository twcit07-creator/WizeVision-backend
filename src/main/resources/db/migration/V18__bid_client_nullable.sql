-- ================================================================
-- V17 — Make bid client_id nullable
-- ================================================================
-- A bid can now be created without a client record.
-- The company becomes a client only when bid is accepted.
-- For lead-based inquiries, clientId is null until acceptance.
-- ================================================================

ALTER TABLE bids
    ALTER COLUMN client_id DROP NOT NULL;