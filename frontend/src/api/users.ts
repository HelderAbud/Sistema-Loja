import { apiJson } from "./client";

export type CurrentUser = {
  id: number;
  email: string;
  appRole: string;
};

export function fetchCurrentUser(): Promise<CurrentUser> {
  return apiJson<CurrentUser>("/api/v1/users/me");
}
