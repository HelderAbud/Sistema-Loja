/**
 * Padrão de estado remoto para alinhar loading / sucesso / erro entre features.
 * Preferir mapear a partir de TanStack Query (`isPending`, `isError`, `failureReason`, `refetch`).
 */
export type RemoteIdle = { status: "idle" };
export type RemoteLoading = { status: "loading" };
export type RemoteSuccess<T> = { status: "success"; data: T };
export type RemoteError<E = string> = { status: "error"; error: E };

export type RemoteState<T, E = string> =
  | RemoteIdle
  | RemoteLoading
  | RemoteSuccess<T>
  | RemoteError<E>;

export function isRemoteSuccess<T, E>(s: RemoteState<T, E>): s is RemoteSuccess<T> {
  return s.status === "success";
}

export function isRemoteError<T, E>(s: RemoteState<T, E>): s is RemoteError<E> {
  return s.status === "error";
}
