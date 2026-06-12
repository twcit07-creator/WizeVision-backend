ALTER TABLE leads
DROP CONSTRAINT IF EXISTS chk_lead_source;

ALTER TABLE leads
    ADD CONSTRAINT chk_lead_source
        CHECK (
            source IN (
                       'REFERRAL',
                       'COLD_CALL',
                       'WEBSITE',
                       'EXHIBITION',
                       'LINKEDIN',
                       'EMAIL_CAMPAIGN',
                       'EXISTING_CLIENT',
                       'INSTAGRAM',
                       'FACEBOOK',
                       'OTHER'
                )
            );