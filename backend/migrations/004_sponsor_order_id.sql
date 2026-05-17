-- Store Razorpay order IDs so private QR receipt pages can show the same
-- payment metadata as the immediate receipt.

ALTER TABLE sponsors
    ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_sponsors_razorpay_order_id
    ON sponsors (razorpay_order_id);
