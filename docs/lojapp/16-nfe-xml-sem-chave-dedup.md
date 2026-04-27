# NFe — XML sem chave de acesso (deduplicação)

## Decisão

Quando o XML **não** contém `chNFe` (ou chave vazia), o risco de importar o mesmo ficheiro duas vezes e **duplicar movimento de stock** é tratado por **impressão digital do conteúdo bruto do XML** (`content_fingerprint` = SHA-256 do XML normalizado no use case de importação).

- **Âmbito:** por `user_id` — a mesma nota importada por outro utilizador não é bloqueada por esta regra.
- **Conflito com chave:** se existir chave de acesso, continua a prevalecer a verificação por `access_key` (comportamento anterior).

## Contrato de API

Segunda importação do **mesmo** XML (mesmo bytes processados → mesmo fingerprint) para o mesmo utilizador:

- Resposta **409 Conflict** com código de erro de domínio alinhado ao handler global (ex.: `DUPLICATE_NFE_XML` / mensagem estável na resposta JSON da API).

## Evolução

- Política fiscal real pode exigir rejeição explícita de XMLs sem chave; hoje o sistema **aceita** e deduplica para proteger stock.
- Migração Flyway que introduz a coluna: `V16__nfe_content_fingerprint.sql` (nome exato no repositório).
