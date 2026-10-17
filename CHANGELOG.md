# Changelog

A tag `v0.1.0` (abril de 2026) marca uma publicação antiga do repositório. A demo que está no ar corresponde a `0.2.0`.

## 0.2.0 — 2026-10-16

Demo: https://sistema-loja-psi.vercel.app  
API: https://sistema-loja-production-7608.up.railway.app

- NFe: importação de XML, dedupe pela chave de acesso e, sem chave, pelo conteúdo. No match, o custo do produto passa a ser o último `vUnCom`.
- Stock: saldo com lock pessimista, kardex, valor ao custo no dashboard e desativação de produto sem apagar o histórico.
- PDV: pagamento dividido, troco, turno de caixa e `Idempotency-Key` na finalização.
- Auth: access token em memória e refresh em cookie HttpOnly. Na demo, `/api` fica no mesmo site do front para o cookie `SameSite=Lax` seguir no reload.
- Segurança: Jackson 2.21.7. O scan da imagem ignora dois CVE do Spring 6.2 cuja correção pública é o Framework 7; a API não usa `XsltView` nem SSE com fragmentos de view.

## 0.1.0 — 2026-04-30

Publicação inicial registada no GitHub (`v0.1.0`). Não descreve o estado atual da demo.
