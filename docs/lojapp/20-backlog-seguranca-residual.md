# Backlog único de segurança residual (P0.3)

Objetivo: consolidar pendências de segurança remanescentes em um único ponto rastreável após a validação de autenticação/sessão.

## Estado

- Data de consolidação: 2026-04-27
- Escopo: autenticação JWT, refresh token, sessão SPA e proteções de abuso
- Fonte técnica: `12-contratos-autenticacao-e-sessao.md` e `13-threat-model-auth-spa.md`

## Pendências residuais priorizadas

- [ ] Implementar deteção de reuse de refresh token com revogação de família/sessão em caso de replay confirmado. (auditoria 2026-09: rotação single-use já existe; o dono só vê “sessão expirada”.)
- [ ] Avaliar MFA para perfis sensíveis (ex.: `ADMIN`) com rollout progressivo e fallback operacional.
- [ ] Definir política de CAPTCHA para registo público quando `invite-secret` não estiver ativo.
- [ ] Incluir varredura dinâmica (ex.: OWASP ZAP baseline) na pipeline CI para endpoints públicos de auth.
- [ ] Revisar política de `LOJAPP_TRUST_FORWARD_HEADERS` por ambiente para evitar spoof de IP em rate limiting.

## Critério de fecho

- Cada item precisa de: decisão técnica, evidência de validação (teste/log/scan) e atualização dos docs de operação.
