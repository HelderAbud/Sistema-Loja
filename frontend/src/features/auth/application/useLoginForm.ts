import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import type { AuthMode } from "../domain/types";
import { mapFetchFailureToUserMessage } from "../../../shared/errors/mapUserFacingApiError";
import { useAuthSession } from "./useAuthSession";

export function useLoginForm() {
  const navigate = useNavigate();
  const { login, register } = useAuthSession();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [authMode, setAuthMode] = useState<AuthMode>("login");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const auth = authMode === "login" ? login : register;
      await auth(email.trim(), password);
      setPassword("");
      navigate("/piloto/products", { replace: true });
      toast.success(
        authMode === "login" ? "Sessão iniciada" : "Conta criada — já pode usar a loja",
      );
    } catch (err: unknown) {
      setError(mapFetchFailureToUserMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return {
    email,
    password,
    authMode,
    error,
    busy,
    setEmail,
    setPassword,
    setAuthMode,
    setError,
    onSubmit,
  };
}
