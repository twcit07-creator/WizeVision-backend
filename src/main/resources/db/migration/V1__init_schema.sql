-- ================================================================
-- V1 — Initial Schema Setup
-- ================================================================
-- Enable UUID generation function
-- Required for UUID primary keys in all tables
-- ================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";