# Matriz de riscos operacionais (impacto x probabilidade)

Consolidação do backlog de riscos operacionais para continuidade da API.

Escala:
- Impacto: baixo / médio / alto
- Probabilidade: baixa / média / alta

## Riscos priorizados

| ID | Risco | Impacto | Probabilidade | Mitigação atual | Próxima ação |
|----|-------|---------|---------------|------------------|--------------|
| R1 | Falha de restore não testada periodicamente | Alto | Média | Scripts de backup/restore documentados | Executar teste de restore em ambiente isolado por rotina (mensal) e registrar evidência |
| R2 | Deploy com variáveis sensíveis ausentes/inválidas (`LOJAPP_JWT_SECRET`, DB) | Alto | Média | Checklist go/no-go de deploy | Validar vars obrigatórias em pipeline pré-deploy |
| R3 | Saturação/erro de endpoint sem alerta adequado | Alto | Baixa-Média | Alertas 5xx/p99 + disponibilidade base | Ajustar limiares por ambiente e simular firing trimestral |
| R4 | Abuso/replay em fluxo de refresh sem resposta operacional rápida | Médio-Alto | Média | Métrica `lojapp.auth.refresh` com `outcome` | Criar playbook para picos de `unexpected`/`invalid` |
| R5 | Crescimento de carga assíncrona sem política de fila ativa | Médio | Média | Estratégia de retry/DLQ documentada | Definir gatilho formal para adoção de fila (ex.: SLA violado por N semanas) |

## Lacunas de teste operacional (estado atual)

- Backup/restore end-to-end não executado nesta sessão por indisponibilidade de Docker no ambiente atual.
- Validação de readiness/liveness em runtime também dependente de stack ativa.

## Critério de revisão contínua

- Revisar esta matriz a cada ciclo de release e atualizar impacto/probabilidade com incidentes reais.
