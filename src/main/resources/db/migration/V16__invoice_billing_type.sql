-- ================================================================
-- V16 — Add billing type fields to invoices
-- ================================================================

ALTER TABLE invoices
    ADD COLUMN target_type        VARCHAR(20) NOT NULL
        DEFAULT 'CONTRACT_BASE',
    ADD COLUMN change_order_id    UUID,
    ADD COLUMN billing_status     VARCHAR(20),
    ADD COLUMN billing_percentage DECIMAL(5,2);

ALTER TABLE invoices
    ADD CONSTRAINT fk_invoice_change_order
        FOREIGN KEY (change_order_id)
            REFERENCES change_orders (id)
            ON DELETE SET NULL;

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoice_target_type
        CHECK (target_type IN (
                               'CONTRACT_BASE', 'CHANGE_ORDER'
            ));

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoice_billing_status
        CHECK (billing_status IS NULL
            OR billing_status IN (
                                  'PARTIAL', 'FULL_AND_FINAL'
                ));