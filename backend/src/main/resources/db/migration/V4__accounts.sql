ALTER TABLE skyrush.users ADD COLUMN username VARCHAR(40), ADD COLUMN password_hash VARCHAR(100);
UPDATE skyrush.users SET username=CASE WHEN id='00000000-0000-0000-0000-000000000001' THEN 'demo' ELSE 'legacy_' || replace(id::text,'-','') END, password_hash='*';
ALTER TABLE skyrush.users ALTER COLUMN username SET NOT NULL, ALTER COLUMN password_hash SET NOT NULL;
ALTER TABLE skyrush.users ADD CONSTRAINT users_username_unique UNIQUE(username);
ALTER TABLE skyrush.users ADD CONSTRAINT users_username_lowercase CHECK(username=lower(username));
CREATE INDEX rounds_daily_scores_idx ON skyrush.game_rounds(started_at,user_id) INCLUDE(earned_points);
