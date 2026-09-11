ALTER TABLE skyrush.game_rounds ADD COLUMN proof_salt VARCHAR(64), ADD COLUMN proof_hash VARCHAR(64);
ALTER TABLE skyrush.game_rounds ADD CONSTRAINT proof_pair CHECK ((proof_salt IS NULL AND proof_hash IS NULL) OR (length(proof_salt)=64 AND length(proof_hash)=64));
CREATE INDEX rounds_tournament_idx ON skyrush.game_rounds(user_id, started_at);
CREATE TABLE skyrush.ticket_offers (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES skyrush.users(id),
 session_id UUID NOT NULL,
 round_id UUID NOT NULL REFERENCES skyrush.game_rounds(id) ON DELETE CASCADE,
 quantity INTEGER NOT NULL CHECK(quantity BETWEEN 1 AND 100),
 unit_price NUMERIC(18,2) NOT NULL CHECK(unit_price>0),
 total NUMERIC(24,2) NOT NULL CHECK(total>0),
 expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
 status VARCHAR(12) NOT NULL CHECK(status IN ('OFFERED','PURCHASED','DECLINED')),
 purchased_at TIMESTAMP WITH TIME ZONE,
 UNIQUE(user_id,session_id),
 CHECK(total=quantity*unit_price),
 CHECK((status='PURCHASED')=(purchased_at IS NOT NULL))
);
CREATE UNIQUE INDEX one_ticket_purchase_per_round ON skyrush.ticket_offers(round_id) WHERE status='PURCHASED';
CREATE INDEX ticket_inventory_idx ON skyrush.ticket_offers(user_id,status);
