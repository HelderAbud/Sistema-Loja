-- Um GTIN por utilizador (ignora linhas sem EAN). Reduz duplicados na importação de NFe.
CREATE UNIQUE INDEX uq_products_user_ean ON products (user_id, ean)
    WHERE ean IS NOT NULL AND length(trim(ean)) > 0;
