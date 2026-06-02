# Checklist de Revisao Senior (Loja Sistema)

Este documento organiza a revisao tecnica para reduzir risco antes de merge/deploy.

## 1) Higiene de repositorio

- [x] `node_modules` bloqueado via `.gitignore`
- [x] `backup.sql` ignorado
- [x] Guardrail no CI para falhar se artefatos locais forem versionados
- [ ] Confirmar que nenhum artefato local antigo ficou no historico recente

## 2) CI e qualidade minima

- [x] Pipeline com testes backend unitarios
- [x] Pipeline com testes backend de integracao
- [x] Pipeline com testes/lint/build frontend
- [x] Build de imagem Docker no CI
- [x] Scan de vulnerabilidade da imagem (Trivy)

## 3) Fronteiras arquiteturais (alvo da Fase 2)

- [ ] Frontend: reforcar regra `domain` nao acessa `api` diretamente
- [ ] Frontend: centralizar casos de uso em `application`
- [ ] Backend: explicitar casos de uso por modulo de negocio
- [ ] Backend: revisar separacao DTO vs entidade em fluxos criticos

## 4) Testes estrategicos de negocio (alvo da Fase 2)

- [ ] Fluxo de venda ponta a ponta (pedido + baixa de estoque)
- [ ] Consistencia de estoque em cenario concorrente basico
- [ ] Regra de emissao/NFe cobrindo cenario principal

## 5) Produto e readiness comercial (alvo da Fase 2/3)

- [ ] Onboarding guiado de loja
- [ ] Multi-tenant claro e validado por testes
- [ ] Configuracoes por cliente (fiscal/operacional)
- [ ] UX de operacao comercial com foco em usuario nao tecnico

## Criterio de "pronto para revisao final"

1. Todos os checks de CI verdes.
2. Sem artefatos locais versionados.
3. Pelo menos 1 teste estrategico de negocio por fluxo critico.
4. Fronteiras de arquitetura documentadas e seguidas.
