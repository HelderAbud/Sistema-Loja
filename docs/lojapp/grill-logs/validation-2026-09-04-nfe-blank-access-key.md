# Validação — NFe sem chave: `access_key` NULL + fingerprint canónico

**Data:** 2026-09-04  
**Branch:** `fix/nfe-blank-access-key`  
**Fatia:** C1 + H12 (review 2026-09-04)

## Comportamento

- `chNFe` em falta ou em branco persiste `access_key = NULL` (índice único parcial Postgres).
- Duas NFes distintas sem chave não colidem na chave; cada uma tem `content_fingerprint` próprio.
- Fingerprint: SHA-256 após remover BOM, unificar EOL (`\r\n`/`\r` → `\n`) e `strip`.

## Evidência

WSL Ubuntu (`./mvnw`), sem Docker/Testcontainers nesta fatia.

```text
./mvnw -q -Pci-unit-tests -Dtest=NfeXmlFingerprintTest,NfeXmlParserTest,ImportNfeUseCaseTest test
```

BUILD SUCCESS (exit 0).

```text
./mvnw -q -Dtest=LojappCoreServiceTest#importNfe_twoDistinctXmlsWithoutAccessKey_bothPersistWithNullAccessKey+importNfe_sameXmlWithoutAccessKey_crlfVsLf_isDuplicate+importNfe_duplicateXmlWithoutAccessKey_doesNotDoubleStock test
```

BUILD SUCCESS (exit 0). H2: importou 8801+8802; 8810 CRLF vs LF → duplicado; 7707 regressão existente.

## Não corrido

- `*IntegrationTest` / Testcontainers (Postgres real)
- Suite completa `./mvnw -Pci-unit-tests test`
- Frontend
