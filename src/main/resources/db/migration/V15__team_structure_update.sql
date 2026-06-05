-- ================================================================
-- V15 — Multi-member team structure
-- ================================================================
-- Remove single modeler/editor/checker columns from projects.
-- Team is now fully managed via project_assignments table.
-- Add TEAM_LEAD role to project_assignments constraint.
-- ================================================================

ALTER TABLE projects
DROP COLUMN IF EXISTS modeler_id,
    DROP COLUMN IF EXISTS editor_id,
    DROP COLUMN IF EXISTS checker_id;

-- Update the role constraint to include TEAM_LEAD
ALTER TABLE project_assignments
DROP CONSTRAINT IF EXISTS chk_assignment_role;

ALTER TABLE project_assignments
    ADD CONSTRAINT chk_assignment_role
        CHECK (role_in_project IN (
                                   'PROJECT_MANAGER','TEAM_LEAD',
                                   'MODELER','EDITOR','CHECKER'
            ));

-- Add unique constraint: one PM per project at a time
CREATE UNIQUE INDEX IF NOT EXISTS
    uq_project_pm_active
    ON project_assignments (project_id)
    WHERE role_in_project = 'PROJECT_MANAGER'
    AND removed_at IS NULL
    AND is_deleted = FALSE;