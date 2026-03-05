
-- Add surrogate auto-increment id to team_members so that
-- PATCH /api/v1/team-members/{id}/respond can reference a single row.
-- The existing composite unique constraint (team_id, user_id) is retained.

-- 1. Drop foreign keys that reference the old composite primary key
ALTER TABLE team_members
    DROP FOREIGN KEY fk_team_members_team,
    DROP FOREIGN KEY fk_team_members_user,
    DROP FOREIGN KEY fk_team_members_invited_by,
    DROP FOREIGN KEY fk_team_members_assignment;

-- 2. Swap primary key and add surrogate id
ALTER TABLE team_members
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id);

-- 3. Re-add foreign keys
ALTER TABLE team_members
    ADD CONSTRAINT fk_team_members_team       FOREIGN KEY (team_id)       REFERENCES teams(id)       ON DELETE CASCADE,
    ADD CONSTRAINT fk_team_members_user       FOREIGN KEY (user_id)       REFERENCES users(id)       ON DELETE CASCADE,
    ADD CONSTRAINT fk_team_members_invited_by FOREIGN KEY (invited_by)    REFERENCES users(id),
    ADD CONSTRAINT fk_team_members_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id);