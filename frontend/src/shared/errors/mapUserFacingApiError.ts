/** Mensagem segura para mostrar ao utilizador (sem códigos técnicos). */
export function mapUserFacingApiError(
  status: number,
  code: string | undefined,
  message: string | undefined,
): string {
  const serverMsg = (message ?? "").trim();

  switch (code) {
    case "UNAUTHORIZED":
      return "Email ou palavra-passe incorretos.";
    case "INTERNAL_ERROR":
      return "O servidor encontrou um erro. Tente novamente dentro de alguns minutos.";
    case "DUPLICATE_NFE_XML":
      return "Este ficheiro fiscal já foi importado.";
    case "VALIDATION_ERROR":
      return serverMsg || "Dados inválidos. Verifique os campos.";
    case "CONFLICT":
      return serverMsg || "Operação em conflito com o estado atual.";
    case "NOT_FOUND":
      return serverMsg || "Recurso não encontrado.";
    default:
      break;
  }

  if (status === 401) {
    return "Não foi possível iniciar sessão. Verifique as credenciais.";
  }
  if (status >= 500) {
    return "Serviço temporariamente indisponível. Tente novamente.";
  }
  if (serverMsg) {
    return serverMsg;
  }
  return "Não foi possível concluir o pedido.";
}

export function mapFetchFailureToUserMessage(err: unknown): string {
  if (err instanceof TypeError && /fetch|network|Failed to fetch/i.test(err.message)) {
    return "Sem ligação ao servidor. Verifique a internet e se a API está a correr.";
  }
  if (err instanceof Error) {
    return err.message;
  }
  return "Ocorreu um erro inesperado.";
}
