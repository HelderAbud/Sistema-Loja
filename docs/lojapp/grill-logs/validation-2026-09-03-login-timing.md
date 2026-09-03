# Grill — login timing (2026-09-03)

`AuthLoginUseCase` corre `passwordEncoder.matches` também quando o email não existe (hash dummy gerado no construtor com o mesmo encoder). Mensagem 401 inalterada. Rate limit 60/min: nota, não mudámos.
