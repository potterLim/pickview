CREATE TABLE refund_adjustments (
    line_id VARCHAR(64) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    seller_id VARCHAR(64) NOT NULL,
    amount_won INTEGER NOT NULL CHECK (amount_won >= 0),
    settlement_id VARCHAR(64) NOT NULL
);
CREATE INDEX idx_adjustment_seller ON refund_adjustments(seller_id, settlement_id);
