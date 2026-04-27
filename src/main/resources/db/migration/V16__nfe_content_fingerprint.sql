-- Dedupe de importação NFe quando não há chave de acesso: hash SHA-256 do XML bruto (UTF-8) por utilizador.
ALTER TABLE nfe_entries ADD COLUMN content_fingerprint VARCHAR(64) NULL;

CREATE UNIQUE INDEX uq_nfe_entries_user_content_fingerprint
    ON nfe_entries (user_id, content_fingerprint)
    WHERE content_fingerprint IS NOT NULL;
