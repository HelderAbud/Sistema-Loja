# NFe — XML sem chave de acesso (deduplicação)

## Decisão

Quando o XML **não** contém `chNFe` (ou chave vazia), o risco de importar o mesmo ficheiro duas vezes e **duplicar movimento de stock** é tratado por **impressão digital do XML canónico** (`content_fingerprint` = SHA-256 em `NfeXmlFingerprint`: remove BOM, unifica `\r\n`/`\r` para `\n` e faz `strip` das extremidades).

- **Chave persistida:** `chNFe` em falta ou em branco grava `access_key = NULL`. String vazia **não** é usada — o índice único parcial `uq_nfe_entries_user_id_access_key` (`WHERE access_key IS NOT NULL`) trata `""` como valor e bloquearia a 2.ª NFe distinta sem chave.
- **Âmbito:** por `user_id` — a mesma nota importada por outro utilizador não é bloqueada por esta regra.
- **Conflito com chave:** se existir chave de acesso, continua a prevalecer a verificação por `access_key` (comportamento anterior).

## Contrato de API

Segunda importação do **mesmo** XML (mesmo bytes processados → mesmo fingerprint) para o mesmo utilizador:

- Resposta **409 Conflict** com código de erro de domínio alinhado ao handler global (ex.: `DUPLICATE_NFE_XML` / mensagem estável na resposta JSON da API).

## Evolução

- Política fiscal real pode exigir rejeição explícita de XMLs sem chave; hoje o sistema **aceita** e deduplica para proteger stock.
- Migração Flyway que introduz a coluna: `V16__nfe_content_fingerprint.sql` (nome exato no repositório).
