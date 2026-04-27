-- Garante updated_at na base mesmo fora do caminho JPA (SQL ad-hoc, ferramentas, futuras integrações).
CREATE OR REPLACE FUNCTION lojapp_touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_products_touch_updated_at ON products;
CREATE TRIGGER tr_products_touch_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE PROCEDURE lojapp_touch_updated_at();

DROP TRIGGER IF EXISTS tr_brands_touch_updated_at ON brands;
CREATE TRIGGER tr_brands_touch_updated_at
    BEFORE UPDATE ON brands
    FOR EACH ROW
    EXECUTE PROCEDURE lojapp_touch_updated_at();
