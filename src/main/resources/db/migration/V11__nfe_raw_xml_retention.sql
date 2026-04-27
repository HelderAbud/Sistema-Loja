-- Permite retenção de XML bruto após período configurado.
ALTER TABLE nfe_entries
    ALTER COLUMN raw_xml DROP NOT NULL;
