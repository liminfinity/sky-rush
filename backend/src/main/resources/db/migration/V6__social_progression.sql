CREATE TABLE skyrush.user_achievements (
 user_id UUID NOT NULL REFERENCES skyrush.users(id), achievement_id VARCHAR(32) NOT NULL,
 unlocked_at TIMESTAMPTZ NOT NULL, round_id UUID REFERENCES skyrush.game_rounds(id) ON DELETE SET NULL,
 PRIMARY KEY(user_id,achievement_id)
);
CREATE TABLE skyrush.daily_challenges (
 challenge_date DATE PRIMARY KEY, kind VARCHAR(32) NOT NULL,
 target NUMERIC(20,4) NOT NULL CHECK(target>0)
);
CREATE TABLE skyrush.daily_challenge_progress (
 user_id UUID NOT NULL REFERENCES skyrush.users(id), challenge_date DATE NOT NULL REFERENCES skyrush.daily_challenges(challenge_date),
 progress NUMERIC(20,4) NOT NULL CHECK(progress>=0), completed BOOLEAN NOT NULL DEFAULT FALSE,
 reward_fragments INTEGER NOT NULL DEFAULT 0 CHECK(reward_fragments IN(0,1)),
 completed_at TIMESTAMPTZ, reward_round_id UUID REFERENCES skyrush.game_rounds(id) ON DELETE SET NULL,
 PRIMARY KEY(user_id,challenge_date), CHECK(completed=(reward_fragments=1)), CHECK(completed=(completed_at IS NOT NULL))
);
CREATE TABLE skyrush.daily_round_contributions (
 round_id UUID PRIMARY KEY REFERENCES skyrush.game_rounds(id) ON DELETE CASCADE,
 user_id UUID NOT NULL, challenge_date DATE NOT NULL,
 FOREIGN KEY(user_id,challenge_date) REFERENCES skyrush.daily_challenge_progress(user_id,challenge_date)
);
CREATE TABLE skyrush.user_presence (
 user_id UUID PRIMARY KEY REFERENCES skyrush.users(id), last_seen_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX presence_last_seen_idx ON skyrush.user_presence(last_seen_at);
CREATE TABLE skyrush.player_activity_events (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES skyrush.users(id),
 round_id UUID REFERENCES skyrush.game_rounds(id) ON DELETE SET NULL,
 event_key VARCHAR(100) NOT NULL UNIQUE,
 kind VARCHAR(24) NOT NULL CHECK(kind IN('CASHOUT','BOOSTER','ACHIEVEMENT')),
 value VARCHAR(80) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX activity_recent_idx ON skyrush.player_activity_events(created_at DESC,id DESC);
-- Only definitions are created on demand. No player ownership, presence, progress or events are seeded.
