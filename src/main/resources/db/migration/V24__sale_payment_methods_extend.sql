-- Alarga meios de pagamento do PDV sem remover CASH/CARD/PIX já persistidos.
ALTER TABLE sale_payments
    ALTER COLUMN payment_method TYPE VARCHAR(50);

ALTER TABLE sale_payments
    DROP CONSTRAINT ck_sale_payments_method;

ALTER TABLE sale_payments
    ADD CONSTRAINT ck_sale_payments_method CHECK (
        payment_method IN (
            'CASH',
            'CARD',
            'PIX',
            'CREDIT_CARD',
            'DEBIT_CARD',
            'BANK_TRANSFER',
            'BANK_SLIP',
            'OTHER'
        )
    );
