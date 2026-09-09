-- Metadados opcionais de pagamento do PDV. amount continua sendo o valor aplicado à venda.
ALTER TABLE sale_payments
    ADD COLUMN card_brand VARCHAR(40),
    ADD COLUMN installments INTEGER,
    ADD COLUMN transaction_id VARCHAR(80),
    ADD COLUMN end_to_end_id VARCHAR(32),
    ADD COLUMN received_amount NUMERIC(19, 2);

ALTER TABLE sale_payments
    ADD CONSTRAINT ck_sale_payments_installments_positive
        CHECK (installments IS NULL OR installments >= 1);

ALTER TABLE sale_payments
    ADD CONSTRAINT ck_sale_payments_received_cash
        CHECK (
            received_amount IS NULL
            OR (payment_method = 'CASH' AND received_amount >= amount)
        );
