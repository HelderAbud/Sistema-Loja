import { useQuery } from "@tanstack/react-query";
import { fetchCurrentUser } from "../api";
import { useAuthStore } from "../authStore";
import { queryKeys } from "../queryKeys";

/** Perfil do utilizador autenticado (`GET /api/v1/users/me`). */
export function useCurrentUser() {
  const accessToken = useAuthStore((s) => s.accessToken);

  return useQuery({
    queryKey: queryKeys.session.me(),
    queryFn: fetchCurrentUser,
    enabled: !!accessToken,
    staleTime: 60_000,
  });
}
