CREATE TABLE skyrush.users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE skyrush.wallets (
    user_id UUID PRIMARY KEY REFERENCES skyrush.users(id),
    balance NUMERIC(24,2) NOT NULL CHECK (balance >= 0)
);
CREATE TABLE skyrush.reward_progress (
    user_id UUID PRIMARY KEY REFERENCES skyrush.users(id),
    fragments BIGINT NOT NULL DEFAULT 0 CHECK (fragments >= 0)
);
CREATE TABLE skyrush.game_rounds (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES skyrush.users(id),
    request_id UUID NOT NULL,
    active_user_id UUID UNIQUE REFERENCES skyrush.users(id),
    theme VARCHAR(10) NOT NULL CHECK (theme IN ('RED', 'GREEN')),
    bet_option_id VARCHAR(32) NOT NULL,
    stake NUMERIC(18,2) NOT NULL CHECK (stake > 0),
    booster_multiplier INTEGER NOT NULL CHECK (booster_multiplier BETWEEN 1 AND 4),
    booster_level INTEGER CHECK (booster_level BETWEEN 1 AND 12),
    config_version VARCHAR(64) NOT NULL,
    config_snapshot TEXT NOT NULL,
    crash_base NUMERIC(12,4) NOT NULL CHECK (crash_base > 1),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    crash_at TIMESTAMP WITH TIME ZONE NOT NULL,
    state VARCHAR(30) NOT NULL CHECK (state IN ('ACTIVE', 'CASHED_OUT', 'COMPLETED_WIN', 'COMPLETED_LOSS')),
    cashout_at TIMESTAMP WITH TIME ZONE,
    cashout_multiplier NUMERIC(12,4),
    payout NUMERIC(24,2) NOT NULL DEFAULT 0 CHECK (payout >= 0),
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_level INTEGER NOT NULL DEFAULT 0 CHECK (completed_level BETWEEN 0 AND 12),
    booster_active BOOLEAN NOT NULL DEFAULT FALSE,
    earned_points BIGINT NOT NULL DEFAULT 0 CHECK (earned_points >= 0),
    reward_fragments INTEGER NOT NULL DEFAULT 0 CHECK (reward_fragments >= 0),
    reward_redeemed BIGINT NOT NULL DEFAULT 0 CHECK (reward_redeemed >= 0),
    reward_bonus NUMERIC(24,2) NOT NULL DEFAULT 0 CHECK (reward_bonus >= 0),
    UNIQUE (user_id, request_id),
    CHECK (crash_at > started_at),
    CHECK ((booster_multiplier = 1 AND booster_level IS NULL) OR (booster_multiplier > 1 AND booster_level IS NOT NULL)),
    CHECK (theme <> 'GREEN' OR (completed_level <= 9 AND (booster_level IS NULL OR booster_level <= 9))),
    CHECK ((state IN ('ACTIVE', 'CASHED_OUT') AND active_user_id = user_id AND active_user_id IS NOT NULL AND completed_at IS NULL)
        OR (state IN ('COMPLETED_WIN', 'COMPLETED_LOSS') AND active_user_id IS NULL AND completed_at IS NOT NULL AND completed_at = crash_at)),
    CHECK ((state IN ('ACTIVE', 'COMPLETED_LOSS') AND cashout_at IS NULL AND cashout_multiplier IS NULL AND payout = 0)
        OR (state IN ('CASHED_OUT', 'COMPLETED_WIN') AND cashout_at IS NOT NULL AND cashout_at >= started_at
            AND cashout_at < crash_at AND cashout_multiplier IS NOT NULL AND cashout_multiplier >= 1 AND payout > 0))
);
CREATE INDEX rounds_history_idx ON skyrush.game_rounds(user_id, completed_at DESC, id);
CREATE INDEX rounds_due_idx ON skyrush.game_rounds(state, crash_at);
CREATE TABLE skyrush.wallet_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES skyrush.users(id),
    round_id UUID NOT NULL REFERENCES skyrush.game_rounds(id),
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('STAKE', 'PAYOUT', 'FRAGMENT_BONUS')),
    amount NUMERIC(24,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (round_id, kind),
    CHECK ((kind = 'STAKE' AND amount < 0) OR (kind IN ('PAYOUT', 'FRAGMENT_BONUS') AND amount > 0))
);
CREATE INDEX wallet_entries_user_idx ON skyrush.wallet_entries(user_id, created_at);
