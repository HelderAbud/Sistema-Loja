-- Chave de acesso NFe única por utilizador (PostgreSQL). Vários NULL permitidos.

DROP INDEX IF EXISTS idx_nfe_entries_access_key;



CREATE UNIQUE INDEX uq_nfe_entries_user_id_access_key

    ON nfe_entries (user_id, access_key)

    WHERE access_key IS NOT NULL;


