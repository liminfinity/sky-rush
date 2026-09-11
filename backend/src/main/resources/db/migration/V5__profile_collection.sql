CREATE TABLE skyrush.cosmetic_items (
    id VARCHAR(32) PRIMARY KEY,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('BALLOON','FRAME')),
    name VARCHAR(60) NOT NULL,
    fragments_required BIGINT NOT NULL CHECK (fragments_required >= 0),
    UNIQUE (id,kind)
);
INSERT INTO skyrush.cosmetic_items VALUES
 ('classic','BALLOON','Классика',0), ('plain','FRAME','Без рамки',0),
 ('constellations','BALLOON','Созвездия',5), ('bronze','FRAME','Бронза',10),
 ('ribbon','BALLOON','Ленты',15), ('aurora','FRAME','Сияние',20);
CREATE TABLE skyrush.user_cosmetic_unlocks (
    user_id UUID NOT NULL REFERENCES skyrush.users(id),
    cosmetic_id VARCHAR(32) NOT NULL REFERENCES skyrush.cosmetic_items(id),
    unlocked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    round_id UUID REFERENCES skyrush.game_rounds(id) ON DELETE SET NULL,
    PRIMARY KEY (user_id,cosmetic_id)
);
CREATE TABLE skyrush.user_equipped_cosmetics (
    user_id UUID NOT NULL,
    kind VARCHAR(16) NOT NULL,
    cosmetic_id VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id,kind),
    FOREIGN KEY (user_id,cosmetic_id) REFERENCES skyrush.user_cosmetic_unlocks(user_id,cosmetic_id),
    FOREIGN KEY (cosmetic_id,kind) REFERENCES skyrush.cosmetic_items(id,kind)
);
-- Existing rewards already spent on bonus currency still count towards collection.
-- No fabricated progress: only actual completed-round grants; retroactive unlocks have no reveal event.
INSERT INTO skyrush.user_cosmetic_unlocks(user_id,cosmetic_id,unlocked_at)
SELECT u.id,c.id,CURRENT_TIMESTAMP FROM skyrush.users u CROSS JOIN skyrush.cosmetic_items c
LEFT JOIN (SELECT user_id,SUM(reward_fragments) AS earned FROM skyrush.game_rounds
           WHERE completed_at IS NOT NULL GROUP BY user_id) r ON r.user_id=u.id
WHERE c.fragments_required <= COALESCE(r.earned,0);
